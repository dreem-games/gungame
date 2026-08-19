const { WebSocketServer } = require('ws');
const { randomUUID } = require('node:crypto');
const Matter = require('matter-js');
const map = require('./multiplayer-map.json');
const { generateWorld } = require('./map-gen');

const PORT = Number(process.env.PORT || 8080);
const MAX_PLAYERS = 4;
const players = new Map();
const dynamicObjects = [];
const wallSegments = [];
const oilPuddles = [];
let events = [];
let worldLayout = null;
const wss = new WebSocketServer({ port: PORT });
const engine = Matter.Engine.create({ gravity: { x: 0, y: 0 } });

function addDynamicBody(type, x, y) {
    const isCircle = type === 'barrel' || type === 'oilTank';
    const body = isCircle
        ? Matter.Bodies.circle(x, y, 128, { frictionAir: 0.1 })
        : Matter.Bodies.rectangle(x, y, 128, 128, { frictionAir: 0.1 });
    Matter.Body.setMass(body, type === 'oilTank' ? 200 : type === 'barrel' ? 50 : 70);
    Matter.World.add(engine.world, body);
    const record = { id: `${type}-${dynamicObjects.length}`, type, body, health: type === 'oilTank' ? 100 : 0 };
    dynamicObjects.push(record);
    return record;
}

function createWorld() {
    const { worldSize } = map;
    const wall = 128;
    const boundaries = [
        Matter.Bodies.rectangle(worldSize / 2, wall / 2, worldSize, wall, { isStatic: true }),
        Matter.Bodies.rectangle(worldSize / 2, worldSize - wall / 2, worldSize, wall, { isStatic: true }),
        Matter.Bodies.rectangle(wall / 2, worldSize / 2, wall, worldSize, { isStatic: true }),
        Matter.Bodies.rectangle(worldSize - wall / 2, worldSize / 2, wall, worldSize, { isStatic: true })
    ];
    Matter.World.add(engine.world, boundaries);

    const world = generateWorld(worldSize);
    worldLayout = {
        boxes: [],
        barrels: [],
        oilTank: null,
        thinWall: null
    };
    for (const box of world.boxes) {
        const record = addDynamicBody('box', box.x, box.y);
        worldLayout.boxes.push({ id: record.id, x: box.x, y: box.y });
    }
    for (const barrel of world.barrels) {
        const record = addDynamicBody('barrel', barrel.x, barrel.y);
        worldLayout.barrels.push({ id: record.id, x: barrel.x, y: barrel.y });
    }
    if (world.oilTank) {
        const record = addDynamicBody('oilTank', world.oilTank.x, world.oilTank.y);
        worldLayout.oilTank = { id: record.id, x: world.oilTank.x, y: world.oilTank.y };
    }
    if (world.thinWall) {
        worldLayout.thinWall = world.thinWall;
        const { x: startX, y: startY, isVertical } = world.thinWall;
        const segments = [];
        for (let i = 0; i < 8; i++) {
            const body = Matter.Bodies.rectangle(
                startX + (isVertical ? 0 : i * 32),
                startY + (isVertical ? i * 32 : 0),
                32, 32, { isStatic: true }
            );
            const record = { id: `thinWall-${i}`, type: 'thinWall', body, health: 30 };
            wallSegments.push(record);
            segments.push(body);
        }
        Matter.World.add(engine.world, segments);
        worldLayout.thinWall.segments = wallSegments.map(({ id }) => ({ id }));
    }
}

function simulate() {
    for (const player of players.values()) {
        const slowed = oilPuddles.some(({ x, y, radius }) => Math.hypot(player.body.position.x - x, player.body.position.y - y) <= radius);
        const speed = player.dashUntil > Date.now() ? 50 : player.input.running ? 9 : 5;
        Matter.Body.setVelocity(player.body, {
            x: player.input.x * speed * (slowed ? 0.45 : 1),
            y: player.input.y * speed * (slowed ? 0.45 : 1)
        });
        Matter.Body.setAngle(player.body, player.input.rotation);
    }
    Matter.Engine.update(engine, 1000 / 60);
}

function currentWorldLayout() {
    return {
        boxes: dynamicObjects.filter(({ type }) => type === 'box').map(({ id, body }) => ({ id, x: body.position.x, y: body.position.y })),
        barrels: dynamicObjects.filter(({ type }) => type === 'barrel').map(({ id, body }) => ({ id, x: body.position.x, y: body.position.y })),
        oilTank: dynamicObjects.find(({ type }) => type === 'oilTank') ? (() => {
            const tank = dynamicObjects.find(({ type }) => type === 'oilTank');
            return { id: tank.id, x: tank.body.position.x, y: tank.body.position.y };
        })() : null,
        thinWall: worldLayout.thinWall ? {
            x: worldLayout.thinWall.x,
            y: worldLayout.thinWall.y,
            isVertical: worldLayout.thinWall.isVertical,
            segments: wallSegments.map(({ id }) => ({ id, index: Number(id.split('-')[1]) }))
        } : null
    };
}

function snapshot() {
    const playersState = [...players.values()].map(({ id, body, input }) => ({
        id,
        x: body.position.x,
        y: body.position.y,
        rotation: input.rotation,
        vx: body.velocity.x,
        vy: body.velocity.y
    }));
    const objectsState = dynamicObjects.map(({ id, type, body }) => ({
        id, type,
        x: body.position.x,
        y: body.position.y,
        rotation: body.angle,
        vx: body.velocity.x,
        vy: body.velocity.y
    }));
    return { players: playersState, objects: objectsState, events };
}

function broadcast() {
    const snap = snapshot();
    const message = JSON.stringify({ type: 'snapshot', authoritative: true, players: snap.players, objects: snap.objects, events: snap.events });
    for (const player of players.values()) {
        if (player.socket && player.socket.readyState === player.socket.OPEN) {
            player.socket.send(message);
        }
    }
    events = [];
}

function explodeObject(record) {
    const index = dynamicObjects.indexOf(record);
    if (index < 0) return;

    dynamicObjects.splice(index, 1);
    Matter.World.remove(engine.world, record.body);
    events.push({ type: `${record.type}Exploded`, id: record.id, x: record.body.position.x, y: record.body.position.y });

    const radius = record.type === 'barrel' ? 700 : 500;
    const maxSpeed = record.type === 'barrel' ? 40 : 30;
    for (const target of [...dynamicObjects, ...players.values()]) {
        const body = target.body;
        if (body === record.body || body.isStatic || target.type === 'barrel') continue;
        const dx = body.position.x - record.body.position.x;
        const dy = body.position.y - record.body.position.y;
        const distance = Math.hypot(dx, dy);
        if (distance === 0 || distance > radius) continue;
        const speed = maxSpeed * (1 - distance / radius);
        Matter.Body.setVelocity(body, {
            x: body.velocity.x + dx / distance * speed,
            y: body.velocity.y + dy / distance * speed
        });
    }

    if (record.type === 'barrel') {
        for (const player of players.values()) {
            const distance = Math.hypot(player.body.position.x - record.body.position.x, player.body.position.y - record.body.position.y);
            if (distance > 500) continue;
            const damage = Math.round(100 * (1 - (distance / 500) ** 2));
            if (damage > 0) events.push({ type: 'playerDamaged', id: player.id, x: 0, y: 0, damage });
        }
    }

    if (record.type !== 'barrel') return;
    for (const target of [...dynamicObjects]) {
        if (target.type !== 'barrel') continue;
        if (Math.hypot(target.body.position.x - record.body.position.x, target.body.position.y - record.body.position.y) <= 500) {
            explodeObject(target);
        }
    }
}

function ruptureOilTank(record) {
    const index = dynamicObjects.indexOf(record);
    if (index < 0) return;

    dynamicObjects.splice(index, 1);
    Matter.World.remove(engine.world, record.body);
    const x = record.body.position.x;
    const y = record.body.position.y;
    events.push({ type: 'oilTankRuptured', id: record.id, x, y });
    oilPuddles.push({ x, y, radius: 420 });
    for (let i = 0; i < 8; i++) {
        const angle = Math.PI * 2 / 8 * i;
        oilPuddles.push({ x: x + Math.cos(angle) * 390, y: y + Math.sin(angle) * 390, radius: 210 });
    }
}

wss.on('connection', (socket) => {
    if (players.size >= MAX_PLAYERS) {
        socket.close(1013, 'The game is full');
        return;
    }

    const id = randomUUID();
    const spawn = map.spawns[players.size];
    const body = Matter.Bodies.circle(spawn.x, spawn.y, map.playerRadius, { frictionAir: 0, inertia: Infinity });
    Matter.Body.setMass(body, 100);
    Matter.World.add(engine.world, body);
    players.set(id, { id, socket, body, dashUntil: 0, input: { x: 0, y: 0, rotation: 0, running: false } });
    socket.send(JSON.stringify({ type: 'welcome', id, world: currentWorldLayout() }));
    broadcast();

    socket.on('message', (rawMessage) => {
        try {
            const message = JSON.parse(rawMessage.toString());
            if (message.type === 'hit' && typeof message.id === 'string' && Number.isFinite(message.damage)) {
                const target = dynamicObjects.find((object) => object.id === message.id);
                if (target?.type === 'barrel') explodeObject(target);
                if (target?.type === 'oilTank') {
                    target.health -= Math.max(0, Math.min(message.damage, 100));
                    if (target.health <= 0) ruptureOilTank(target);
                }

                const wall = wallSegments.find((object) => object.id === message.id);
                if (wall) {
                    wall.health -= Math.max(0, Math.min(message.damage, 100));
                    if (wall.health <= 0) {
                        wallSegments.splice(wallSegments.indexOf(wall), 1);
                        Matter.World.remove(engine.world, wall.body);
                        events.push({ type: 'thinWallDestroyed', id: wall.id, x: wall.body.position.x, y: wall.body.position.y });
                    }
                }
                return;
            }
            if (message.type !== 'input' || ![message.x, message.y, message.rotation].every(Number.isFinite)) return;

            const player = players.get(id);
            if (player) {
                const length = Math.hypot(message.x, message.y);
                player.input.x = length > 1 ? message.x / length : message.x;
                player.input.y = length > 1 ? message.y / length : message.y;
                player.input.rotation = message.rotation;
                player.input.running = message.running === true;
                if (message.dash === true) player.dashUntil = Date.now() + 250;
            }
        } catch {
            // Некорректные сообщения просто игнорируются.
        }
    });

    socket.on('close', () => {
        const player = players.get(id);
        if (player) Matter.World.remove(engine.world, player.body);
        players.delete(id);
        broadcast();
    });
});

createWorld();
setInterval(simulate, 1000 / 60);
setInterval(broadcast, 1000 / 30);
console.log(`Multiplayer MVP is listening on ws://localhost:${PORT}`);
