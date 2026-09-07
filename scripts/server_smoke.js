const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const { randomUUID } = require('node:crypto');
const WebSocket = require('ws');

const port = 18080;
const server = spawn(process.execPath, ['server.js'], {
    cwd: require('node:path').join(__dirname, '..'),
    env: { ...process.env, PORT: String(port) },
    stdio: ['ignore', 'pipe', 'inherit']
});
const sockets = [];

const isState = (message) => message.type === 'snapshot' || message.type === 'snapshotDelta';

function waitFor(socket, predicate, timeout = 3000) {
    return new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error(`Timed out waiting for: ${predicate}`)), timeout);
        const onMessage = (raw) => {
            const message = JSON.parse(raw.toString());
            if (!predicate(message)) return;
            clearTimeout(timer);
            socket.off('message', onMessage);
            resolve(message);
        };
        socket.on('message', onMessage);
    });
}

async function connect(sessionToken, radius) {
    const session = sessionToken ? `&session=${sessionToken}` : '';
    const interest = radius ? `&radius=${radius}` : '';
    const socket = new WebSocket(`ws://localhost:${port}?v=2${session}${interest}`);
    sockets.push(socket);
    const welcome = await waitFor(socket, (message) => message.type === 'welcome');
    assert(Number.isFinite(welcome.x) && Number.isFinite(welcome.y));
    assert.equal(welcome.v, 2);
    assert(Number.isInteger(welcome.tick));
    assert.equal(typeof welcome.sessionToken, 'string');
    return {
        socket,
        id: welcome.id,
        sessionToken: welcome.sessionToken,
        x: welcome.x,
        y: welcome.y,
        world: welcome.world
    };
}

async function main() {
    await new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error('Server did not start')), 5000);
        server.stdout.on('data', (data) => {
            if (data.toString().includes('listening')) {
                clearTimeout(timer);
                resolve();
            }
        });
        server.once('exit', (code) => reject(new Error(`Server exited early (${code})`)));
    });

    const first = await connect();
    const second = await connect();
    const idleDelta = await waitFor(first.socket, (message) => message.type === 'snapshotDelta');
    assert.equal(idleDelta.v, 2);

    const firstState = first;
    const secondState = second;
    const fire = (id, speed, damage, piercing) =>
        first.socket.send(
            JSON.stringify({
                type: 'fire',
                id,
                x: firstState.x,
                y: firstState.y,
                angle: Math.atan2(secondState.y - firstState.y, secondState.x - firstState.x),
                speed,
                damage,
                texture: 'projectiles',
                frame: 'bullet',
                piercing
            })
        );
    fire(randomUUID(), 80, 100, false);
    await new Promise((resolve) => setTimeout(resolve, 200));
    const firstRifleId = randomUUID();
    const rifleVisible = waitFor(
        first.socket,
        (message) => isState(message) && message.objects.some(({ id }) => id === firstRifleId)
    );
    fire(firstRifleId, 60, 55, true);
    await rifleVisible;
    const wounded = await waitFor(
        second.socket,
        (message) => isState(message) && message.players.find(({ id }) => id === second.id)?.hp === 45
    );
    assert(wounded.events.some((event) => event.type === 'playerDamaged' && event.damage === 55));
    await new Promise((resolve) => setTimeout(resolve, 1000));
    fire(randomUUID(), 60, 55, true);
    const death = await waitFor(
        second.socket,
        (message) => isState(message) && message.players.find(({ id }) => id === second.id)?.isDead,
        5000
    );
    assert(death.events.some((event) => event.type === 'playerDamaged' && event.id === second.id));
    first.socket.send('{bad json');
    first.socket.send(JSON.stringify({ type: 'input', x: 1, y: 0, rotation: 0, running: false, dash: true }));
    await waitFor(
        first.socket,
        (message) => isState(message) && message.players.find(({ id }) => id === first.id)?.vx > 40
    );
    const spam = setInterval(
        () => first.socket.send(JSON.stringify({ type: 'input', x: 1, y: 0, rotation: 0, dash: true })),
        20
    );
    await new Promise((resolve) => setTimeout(resolve, 350));
    clearInterval(spam);
    await waitFor(
        first.socket,
        (message) => isState(message) && message.players.find(({ id }) => id === first.id)?.vx < 10
    );

    const third = await connect();
    const closed = new Promise((resolve) => second.socket.once('close', resolve));
    second.socket.close();
    await closed;
    const replacement = await connect(second.sessionToken);
    assert.equal(replacement.id, second.id);
    assert.notDeepEqual([replacement.x, replacement.y], [third.x, third.y]);
    const resumed = await waitFor(
        replacement.socket,
        (message) =>
            isState(message) &&
            message.players.some(({ id, hp, isDead }) => id === replacement.id && hp === 0 && isDead)
    );
    assert(resumed.players.some(({ id }) => id === replacement.id));
    replacement.socket.send(JSON.stringify({ type: 'input', x: 1, y: 0, rotation: 0, isDead: false }));
    await waitFor(
        replacement.socket,
        (message) => isState(message) && message.players.some(({ id, vx }) => id === replacement.id && vx === 0),
        6000
    );

    const shotgunner = await connect(undefined, 6144);
    for (let i = 0; i < 32; i++) {
        shotgunner.socket.send(
            JSON.stringify({
                type: 'fire',
                id: randomUUID(),
                x: shotgunner.x,
                y: shotgunner.y,
                angle: 0,
                speed: 15,
                damage: 5,
                texture: 'projectiles',
                frame: 'bullet'
            })
        );
    }
    await waitFor(
        shotgunner.socket,
        (message) => isState(message) && message.objects.filter(({ ownerId }) => ownerId === shotgunner.id).length >= 20
    );

    const legacy = new WebSocket(`ws://localhost:${port}`);
    sockets.push(legacy);
    const legacyWelcome = await waitFor(legacy, (message) => message.type === 'welcome');
    assert.equal(legacyWelcome.v, undefined);
    const legacySnapshot = await waitFor(legacy, (message) => message.type === 'snapshot');
    assert.equal(legacySnapshot.v, undefined);
    const legacyClosed = new Promise((resolve) => legacy.once('close', resolve));
    legacy.close(1000, 'restart');
    await legacyClosed;
    await connect();
}

main()
    .then(() => console.log('server smoke: ok'))
    .finally(() => {
        for (const socket of sockets) socket.close();
        server.kill();
    });
