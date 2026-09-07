import { createRequire } from 'node:module';

import { defineConfig, type Plugin } from 'vite';

const require = createRequire(import.meta.url);

function gameWs(): Plugin {
    return {
        name: 'gun-game-ws',
        configureServer(server) {
            const { start } = require('./server.js');
            if (server.httpServer) start({ server: server.httpServer });
        }
    };
}

export default defineConfig({
    plugins: [gameWs()]
});
