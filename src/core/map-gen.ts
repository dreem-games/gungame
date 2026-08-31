// Раскладка карты для офлайн-клиента (ESM). Сервер использует map-gen.js (CJS).
// Офлайн-раскладка одноразовая: при подключении сервера она заменяется серверной.

export interface PlacedSpot {
    x: number;
    y: number;
}

export interface ThinWallPlacement {
    x: number;
    y: number;
    isVertical: boolean;
}

export interface GeneratedWorld {
    boxes: PlacedSpot[];
    barrels: PlacedSpot[];
    oilTank: PlacedSpot | null;
    thinWall: ThinWallPlacement | null;
}

const dist = (x1: number, y1: number, x2: number, y2: number) => Math.hypot(x1 - x2, y1 - y2);

export function generateWorld(worldSize: number, rng: () => number = Math.random): GeneratedWorld {
    const MARGIN = 128;
    const CENTER = worldSize / 2;
    const SAFE_RADIUS = 300;
    const placed: { x: number; y: number; radius: number; type: string }[] = [];

    const findSpot = (radius: number, type: string): PlacedSpot | null => {
        for (let attempts = 0; attempts < 50; attempts++) {
            let x = MARGIN + rng() * (worldSize - MARGIN * 2);
            let y = MARGIN + rng() * (worldSize - MARGIN * 2);

            if (type === 'box' && rng() < 0.3) {
                const boxes = placed.filter((o) => o.type === 'box');
                if (boxes.length > 0) {
                    const target = boxes[Math.floor(rng() * boxes.length)];
                    x = Math.min(worldSize - MARGIN, Math.max(MARGIN, target.x + (rng() * 600 - 300)));
                    y = Math.min(worldSize - MARGIN, Math.max(MARGIN, target.y + (rng() * 600 - 300)));
                }
            }

            if (dist(x, y, CENTER, CENTER) < SAFE_RADIUS + radius) continue;
            if (placed.some((o) => dist(x, y, o.x, o.y) < radius + o.radius)) continue;
            return { x, y };
        }
        return null;
    };

    const place = (type: string, radius: number): PlacedSpot | null => {
        const spot = findSpot(radius, type);
        if (!spot) return null;
        placed.push({ ...spot, radius, type });
        return spot;
    };

    const world: GeneratedWorld = { boxes: [], barrels: [], oilTank: null, thinWall: null };
    for (let i = 0; i < 75; i++) {
        const spot = place('box', 90);
        if (spot) world.boxes.push(spot);
    }
    for (let i = 0; i < 16; i++) {
        const spot = place('barrel', 64);
        if (spot) world.barrels.push(spot);
    }
    world.oilTank = place('oilTank', 102);
    const wallSpot = place('thinWall', 140);
    if (wallSpot) {
        const isVertical = rng() > 0.5;
        world.thinWall = { x: wallSpot.x - (isVertical ? 0 : 128), y: wallSpot.y - (isVertical ? 128 : 0), isVertical };
    }
    return world;
}
