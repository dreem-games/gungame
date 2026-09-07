const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const WebSocket = require('ws');

const port = 18081;
const server = spawn(process.execPath, ['server.js'], {
    cwd: require('node:path').join(__dirname, '..'),
    env: { ...process.env, PORT: String(port) },
    stdio: ['ignore', 'pipe', 'inherit']
});
const sockets = [];

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

    const visibleCounts = [];
    const visibleObjectCounts = [];
    const simulationP95 = [];
    const visibleTypes = new Set();
    await Promise.all(
        Array.from(
            { length: 100 },
            () =>
                new Promise((resolve, reject) => {
                    const socket = new WebSocket(`ws://localhost:${port}?v=2`);
                    sockets.push(socket);
                    const timer = setTimeout(
                        () => reject(new Error('Client did not receive welcome + baseline')),
                        15000
                    );
                    let welcomed = false;
                    let receivedBaseline = false;
                    socket.on('message', (raw) => {
                        const message = JSON.parse(raw.toString());
                        if (message.type === 'snapshot' && !receivedBaseline) {
                            visibleCounts.push(message.players.length);
                            visibleObjectCounts.push(message.objects.length);
                            for (const object of message.objects) visibleTypes.add(object.type);
                            simulationP95.push(message.simulationP95);
                            receivedBaseline = true;
                        }
                        if (message.type === 'welcome') welcomed = true;
                        if (!welcomed || !receivedBaseline) return;
                        clearTimeout(timer);
                        resolve();
                    });
                })
        )
    );
    await new Promise((resolve) => setTimeout(resolve, 500));
    assert.equal(sockets.length, 100);
    assert.equal(visibleCounts.length, 100);
    assert(Math.max(...visibleCounts) < 100);
    assert(Math.max(...visibleObjectCounts) < 100);
    assert(Math.max(...simulationP95) < 16);
    assert(visibleTypes.has('thinWall'));
    console.log(
        `server load: ok (sim p95: ${Math.max(...simulationP95).toFixed(2)}ms, visible players: ${Math.max(...visibleCounts)}/100, objects: ${Math.max(...visibleObjectCounts)}/100)`
    );
}

main().finally(() => {
    for (const socket of sockets) socket.close(1000, 'restart');
    server.kill();
});
