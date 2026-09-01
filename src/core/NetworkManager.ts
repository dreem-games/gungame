export interface RemotePlayerState {
    id: string;
    x: number;
    y: number;
    rotation: number;
    isDead?: boolean;
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
}

export interface WorldLayout {
    boxes: { id: string; x: number; y: number }[];
    barrels: { id: string; x: number; y: number }[];
    oilTank: { id: string; x: number; y: number } | null;
    thinWall: { x: number; y: number; isVertical: boolean; segments: { id: string; index: number }[] } | null;
}

export interface WorldEvent {
    type: 'barrelExploded' | 'oilTankRuptured' | 'thinWallDestroyed' | 'playerDamaged' | 'projectileFired';
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
    private socket: WebSocket;
    private localPlayerId: string | null = null;
    private players = new Map<string, RemotePlayerState>();
    private lastSentAt = 0;
    private isAuthoritative = false;
    private worldObjects: WorldObjectState[] = [];
    private worldLayout: WorldLayout | null = null;
    private worldEvents: WorldEvent[] = [];

    constructor() {
        const port = window.location.port === '5173' ? '8080' : window.location.port;
        const url = `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.hostname}${port ? `:${port}` : ''}`;
        this.socket = new WebSocket(url);
        this.socket.addEventListener('message', (event) => this.handleMessage(event));
    }

    public sendInput(x: number, y: number, rotation: number, running: boolean, dash: boolean, isDead?: boolean) {
        if (this.socket.readyState !== WebSocket.OPEN || performance.now() - this.lastSentAt < 33) return;

        this.lastSentAt = performance.now();
        this.socket.send(JSON.stringify({ type: 'input', x, y, rotation, running, dash, isDead }));
    }

    public getRemotePlayers(): ReadonlyMap<string, RemotePlayerState> {
        return new Map([...this.players].filter(([id]) => id !== this.localPlayerId));
    }

    public getLocalPlayer(): RemotePlayerState | undefined {
        return this.isAuthoritative && this.localPlayerId ? this.players.get(this.localPlayerId) : undefined;
    }

    public getWorldObjects(): readonly WorldObjectState[] {
        return this.worldObjects;
    }

    public getWorldLayout(): WorldLayout | null {
        return this.worldLayout;
    }

    public consumeWorldEvents(): WorldEvent[] {
        const events = this.worldEvents;
        this.worldEvents = [];
        return events;
    }

    public sendFire(
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
            this.socket.send(JSON.stringify({ type: 'fire', x, y, angle, speed, damage, texture, frame, piercing }));
        }
    }

    public isConnected(): boolean {
        return this.localPlayerId !== null;
    }

    public destroy() {
        this.socket.close();
    }

    private handleMessage(event: MessageEvent<string>) {
        const message: unknown = JSON.parse(event.data);
        if (!this.isMessage(message)) return;

        if (message.type === 'welcome') {
            this.localPlayerId = message.id;
            this.worldLayout = message.world;
            return;
        }

        this.isAuthoritative = message.authoritative;
        this.players = new Map(message.players.map((player) => [player.id, player]));
        this.worldObjects = message.objects;
        this.worldEvents.push(...message.events);
    }

    private isMessage(message: unknown): message is
        | { type: 'welcome'; id: string; world: WorldLayout }
        | {
              type: 'snapshot';
              authoritative: boolean;
              players: RemotePlayerState[];
              objects: WorldObjectState[];
              events: WorldEvent[];
          } {
        if (typeof message !== 'object' || message === null || !('type' in message)) return false;
        if (message.type === 'welcome') return 'id' in message && typeof message.id === 'string' && 'world' in message;
        return (
            message.type === 'snapshot' &&
            'authoritative' in message &&
            typeof message.authoritative === 'boolean' &&
            'players' in message &&
            Array.isArray(message.players) &&
            'objects' in message &&
            Array.isArray(message.objects) &&
            'events' in message &&
            Array.isArray(message.events)
        );
    }
}
