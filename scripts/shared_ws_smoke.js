const assert = require('node:assert/strict');
const http = require('node:http');
const { WebSocket, WebSocketServer } = require('ws');

const httpServer = http.createServer();
const otherWss = new WebSocketServer({ noServer: true });
httpServer.on('upgrade', (request, socket, head) => {
    if (new URL(request.url, 'http://localhost').pathname !== '/hmr') return;
    otherWss.handleUpgrade(request, socket, head, (webSocket) => otherWss.emit('connection', webSocket));
});
otherWss.on('connection', (socket) => socket.send('hmr-ok'));

const { start } = require('../server');
start({ server: httpServer });

function firstMessage(url) {
    return new Promise((resolve, reject) => {
        const socket = new WebSocket(url);
        const timer = setTimeout(() => reject(new Error(`No message from ${url}`)), 3000);
        socket.once('message', (message) => {
            clearTimeout(timer);
            socket.close(1000, 'restart');
            resolve(message.toString());
        });
    });
}

httpServer.listen(0, async () => {
    const { port } = httpServer.address();
    const [hmr, game] = await Promise.all([
        firstMessage(`ws://localhost:${port}/hmr`),
        firstMessage(`ws://localhost:${port}/ws?v=2`)
    ]);
    assert.equal(hmr, 'hmr-ok');
    assert.equal(JSON.parse(game).type, 'welcome');
    console.log('shared ws smoke: ok');
    process.exit();
});
