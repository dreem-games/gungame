export interface RemotePlayerState {
    id: string;
    x: number;
    y: number;
    rotation: number;
    isDead?: boolean;
    hp?: number;
    stamina?: number;
    vx?: number;
    vy?: number;
}

export interface WorldObjectState {
    id: string;
    type: string;
    x: number;
    y: number;
    rotation: number;
    isDead?: boolean;
    vx: number;
    vy: number;
    radius?: number;
    ownerId?: string;
    texture?: string;
    frame?: string;
    piercing?: boolean;
}

export interface WorldLayout {
    boxes: { id: string; x: number; y: number }[];
    barrels: { id: string; x: number; y: number }[];
    oilTank: { id: string; x: number; y: number } | null;
    thinWall: { x: number; y: number; isVertical: boolean; segments: { id: string; index: number }[] } | null;
}

export interface WorldEvent {
    type:
        | 'barrelExploded'
        | 'oilTankRuptured'
        | 'thinWallDestroyed'
        | 'playerDamaged'
        | 'projectileFired'
        | 'projectileDestroyed';
    id: string;
    x: number;
    y: number;
    damage?: number;
    angle?: number;
    speed?: number;
    texture?: string;
    frame?: string;
    piercing?: boolean;
    playerId?: string;
}

export class NetworkManager {
    private socket!: WebSocket;
    private readonly url: string;
    private sessionToken: string | null = null;
    private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
    private reconnectDelay = 500;
    private destroyed = false;
    private localPlayerId: string | null = null;
    private players = new Map<string, RemotePlayerState>();
    private lastSentAt = 0;
    private isAuthoritative = false;
    private worldObjects: WorldObjectState[] = [];
    private worldLayout: WorldLayout | null = null;
    private spawnPosition: { x: number; y: number } | null = null;
    private worldEvents: WorldEvent[] = [];
    private welcomeSequence = 0;
    private hasWorldState = false;
    private sessionReplaced = false;
    private playerHistory = new Map<string, { receivedAt: number; state: RemotePlayerState }[]>();
    private snapshotIntervals: number[] = [];
    private lastSnapshotAt = 0;
    private interpolationDelay = 100;
    private everConnected = false;
    private lost = false;

    constructor(interestRadius: number) {
        // Игровой ws-сервер в дев-режиме живёт рядом с Vite на том же порту (путь /ws, vite.config.ts);
        // в автономном режиме сервера отдельный адрес задаётся снаружи.
        const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws';
        this.url = `${scheme}://${window.location.host}/ws?v=2&radius=${Math.ceil(interestRadius)}`;
        this.connect();
    }

    private connect() {
        const suffix = this.sessionToken ? `&session=${encodeURIComponent(this.sessionToken)}` : '';
        const socket = new WebSocket(this.url + suffix);
        this.socket = socket;
        socket.addEventListener('close', () => {
            if (socket !== this.socket || this.destroyed) return;
            // «Потеря связи» — только если клиент уже получил welcome
            if (this.everConnected) this.lost = true;
            this.localPlayerId = null;
            this.reconnectTimer = setTimeout(() => {
                this.reconnectTimer = null;
                this.connect();
            }, this.reconnectDelay);
            this.reconnectDelay = Math.min(3000, this.reconnectDelay * 2);
        });
        socket.addEventListener('message', (event) => {
            if (socket === this.socket) this.handleMessage(event);
        });
    }

    public sendInput(x: number, y: number, rotation: number, running: boolean, dash: boolean, interestRadius: number) {
        if (this.socket.readyState !== WebSocket.OPEN || performance.now() - this.lastSentAt < 33) return;

        this.lastSentAt = performance.now();
        this.socket.send(JSON.stringify({ type: 'input', x, y, rotation, running, dash, interestRadius }));
    }

    public getRemotePlayers(): ReadonlyMap<string, RemotePlayerState> {
        const result = new Map<string, RemotePlayerState>();
        const renderAt = performance.now() - this.interpolationDelay;
        for (const [id, state] of this.players) {
            if (id === this.localPlayerId) continue;
            const history = this.playerHistory.get(id);
            if (!history?.length) {
                result.set(id, state);
                continue;
            }
            const afterIndex = history.findIndex(({ receivedAt }) => receivedAt >= renderAt);
            if (afterIndex <= 0) {
                result.set(id, afterIndex === 0 ? history[0].state : history.at(-1)!.state);
                continue;
            }
            const before = history[afterIndex - 1];
            const after = history[afterIndex];
            if (Math.hypot(after.state.x - before.state.x, after.state.y - before.state.y) > 80) {
                result.set(id, after.state);
                continue;
            }
            const span = after.receivedAt - before.receivedAt;
            const t = span > 0 ? (renderAt - before.receivedAt) / span : 1;
            const angleDelta = Math.atan2(
                Math.sin(after.state.rotation - before.state.rotation),
                Math.cos(after.state.rotation - before.state.rotation)
            );
            result.set(id, {
                ...after.state,
                x: before.state.x + (after.state.x - before.state.x) * t,
                y: before.state.y + (after.state.y - before.state.y) * t,
                rotation: before.state.rotation + angleDelta * t
            });
        }
        return result;
    }

    public getLocalPlayer(): RemotePlayerState | undefined {
        return this.isAuthoritative && this.localPlayerId ? this.players.get(this.localPlayerId) : undefined;
    }

    public getWorldObjects(): readonly WorldObjectState[] {
        return this.worldObjects;
    }

    public hasReceivedWorldState(): boolean {
        return this.hasWorldState;
    }

    public getWorldLayout(): WorldLayout | null {
        return this.worldLayout;
    }

    public getSpawnPosition(): { x: number; y: number } | null {
        return this.spawnPosition;
    }

    public getWelcomeSequence(): number {
        return this.welcomeSequence;
    }

    public consumeSessionReplacement(): boolean {
        const replaced = this.sessionReplaced;
        this.sessionReplaced = false;
        return replaced;
    }

    public consumeWorldEvents(): WorldEvent[] {
        const events = this.worldEvents;
        this.worldEvents = [];
        return events;
    }

    public sendFire(
        id: string,
        x: number,
        y: number,
        angle: number,
        speed: number,
        damage: number,
        texture: string,
        frame: string,
        piercing: boolean
    ) {
        if (this.socket.readyState === WebSocket.OPEN) {
            this.socket.send(
                JSON.stringify({ type: 'fire', id, x, y, angle, speed, damage, texture, frame, piercing })
            );
        }
    }

    public isConnected(): boolean {
        return this.socket.readyState === WebSocket.OPEN && this.localPlayerId !== null;
    }

    public hasLostConnection(): boolean {
        return this.lost;
    }

    public retryNow() {
        if (this.destroyed || !this.reconnectTimer) return;
        clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
        this.reconnectDelay = 500;
        this.connect();
    }

    public destroy() {
        this.destroyed = true;
        if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
        this.socket.close(1000, 'restart');
    }

    private handleMessage(event: MessageEvent<string>) {
        let message: unknown;
        try {
            message = JSON.parse(event.data);
        } catch {
            return;
        }
        if (!this.isMessage(message)) return;

        if (message.type === 'welcome') {
            this.sessionReplaced = this.sessionToken !== null && this.sessionToken !== message.sessionToken;
            this.players.clear();
            this.worldObjects = [];
            this.worldEvents = [];
            this.playerHistory.clear();
            this.snapshotIntervals = [];
            this.lastSnapshotAt = 0;
            this.hasWorldState = false;
            this.localPlayerId = message.id;
            this.worldLayout = message.world;
            this.spawnPosition = { x: message.x, y: message.y };
            this.sessionToken = message.sessionToken ?? null;
            this.welcomeSequence++;
            this.everConnected = true;
            this.lost = false;
            this.reconnectDelay = 500;
            return;
        }

        const receivedAt = performance.now();
        if (this.lastSnapshotAt) {
            this.snapshotIntervals.push(receivedAt - this.lastSnapshotAt);
            if (this.snapshotIntervals.length > 30) this.snapshotIntervals.shift();
            const sorted = this.snapshotIntervals.toSorted((a, b) => a - b);
            this.interpolationDelay = Math.max(40, Math.min(200, sorted[Math.floor(sorted.length * 0.9)] * 2));
        }
        this.lastSnapshotAt = receivedAt;
        this.hasWorldState = true;
        this.isAuthoritative = message.authoritative;
        if (message.type === 'snapshot') {
            this.players = new Map(message.players.map((player) => [player.id, player]));
            this.worldObjects = message.objects;
            for (const id of this.playerHistory.keys()) {
                if (!this.players.has(id)) this.playerHistory.delete(id);
            }
        } else {
            for (const id of message.removedPlayers) {
                this.players.delete(id);
                this.playerHistory.delete(id);
            }
            for (const player of message.players) this.players.set(player.id, player);
            const objects = new Map(this.worldObjects.map((object) => [object.id, object]));
            for (const id of message.removedObjects) objects.delete(id);
            for (const object of message.objects) objects.set(object.id, object);
            this.worldObjects = [...objects.values()];
        }
        for (const player of message.players) {
            if (player.id === this.localPlayerId) continue;
            const history = this.playerHistory.get(player.id) ?? [];
            history.push({ receivedAt, state: player });
            if (history.length > 10) history.shift();
            this.playerHistory.set(player.id, history);
        }
        this.worldEvents.push(...message.events);
    }

    private isMessage(message: unknown): message is
        | {
              v?: number;
              tick?: number;
              type: 'welcome';
              id: string;
              x: number;
              y: number;
              world: WorldLayout;
              sessionToken?: string;
          }
        | {
              v?: number;
              tick?: number;
              type: 'snapshot';
              authoritative: boolean;
              players: RemotePlayerState[];
              objects: WorldObjectState[];
              events: WorldEvent[];
          }
        | {
              v: 2;
              tick: number;
              type: 'snapshotDelta';
              authoritative: boolean;
              players: RemotePlayerState[];
              removedPlayers: string[];
              objects: WorldObjectState[];
              removedObjects: string[];
              events: WorldEvent[];
          } {
        if (typeof message !== 'object' || message === null || !('type' in message)) return false;
        const hasValidEnvelope =
            (!('v' in message) || message.v === 2) &&
            (!('tick' in message) || (typeof message.tick === 'number' && Number.isInteger(message.tick)));
        if (!hasValidEnvelope) return false;
        if (message.type === 'snapshotDelta' && (!('v' in message) || message.v !== 2)) return false;
        if (message.type === 'welcome') {
            return (
                'id' in message &&
                typeof message.id === 'string' &&
                'x' in message &&
                typeof message.x === 'number' &&
                Number.isFinite(message.x) &&
                'y' in message &&
                typeof message.y === 'number' &&
                Number.isFinite(message.y) &&
                (!('sessionToken' in message) || typeof message.sessionToken === 'string') &&
                'world' in message
            );
        }
        return (
            (message.type === 'snapshot' || message.type === 'snapshotDelta') &&
            'authoritative' in message &&
            typeof message.authoritative === 'boolean' &&
            'players' in message &&
            Array.isArray(message.players) &&
            'objects' in message &&
            Array.isArray(message.objects) &&
            'events' in message &&
            Array.isArray(message.events) &&
            (message.type === 'snapshot' ||
                ('removedPlayers' in message &&
                    Array.isArray(message.removedPlayers) &&
                    'removedObjects' in message &&
                    Array.isArray(message.removedObjects)))
        );
    }
}
