const { WebSocketServer } = require('ws');
const { randomUUID } = require('node:crypto');
const Matter = require('matter-js');
const map = require('./multiplayer-map.json');
const { generateWorld } = require('./map-gen');

const PORT = Number(process.env.PORT || 8080);
const MAX_PLAYERS = 100;
const MIN_INTEREST_RADIUS = 1400;
const EFFECT_MARGIN = 1000;
const STEP_MS = 1000 / 60;
const MAX_FRAME_MS = 250;
const MAX_FIRE_PER_SECOND = 60;
const MAX_BUFFERED_BYTES = 64 * 1024;
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const WEAPONS = {
    rifle: { damage: 55, speed: 60, piercing: true, fireRate: 1000 },
    assaultRifle: { damage: 10, speed: 30, piercing: false, fireRate: 100 },
    shotgun: { damage: 5, minSpeed: 12, maxSpeed: 18, piercing: false, fireRate: 2700 }
};
const players = new Map();
const dynamicObjects = [];
const wallSegments = [];
const oilPuddles = [];
const scorchMarks = [];
const projectiles = new Map();
let events = [];
let worldLayout = null;
let wssInstance = null;
let lastSimulationAt = performance.now();
let simulationAccumulator = 0;
let tick = 0;
const simulationDurations = [];
const engine = Matter.Engine.create({ gravity: { x: 0, y: 0 } });

function addDynamicBody(type, x, y) {
    const isCircle = type === 'barrel' || type === 'oilTank';
    const body = isCircle
        ? Matter.Bodies.circle(x, y, 64, { frictionAir: 0.1 })
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
                32,
                32,
                { isStatic: true }
            );
            const record = { id: `thinWall-${i}`, type: 'thinWall', body, health: 30 };
            wallSegments.push(record);
            segments.push(body);
        }
        Matter.World.add(engine.world, segments);
        worldLayout.thinWall.segments = wallSegments.map(({ id }) => ({ id }));
    }
}

function chooseSpawn() {
    const freeInitialSpawn = map.spawns.find(({ x, y }) =>
        [...players.values()].every(({ body }) => Math.hypot(body.position.x - x, body.position.y - y) > 60)
    );
    if (freeInitialSpawn) return freeInitialSpawn;

    // ponytail: сетка O(candidates × players) достаточна для 100 игроков; spatial index нужен после профиля.
    const bodies = Matter.Composite.allBodies(engine.world);
    let best = map.spawns[0];
    let bestDistance = -1;
    for (let y = 256; y < map.worldSize - 256; y += 256) {
        for (let x = 256; x < map.worldSize - 256; x += 256) {
            const occupied = Matter.Query.region(bodies, {
                min: { x: x - map.playerRadius, y: y - map.playerRadius },
                max: { x: x + map.playerRadius, y: y + map.playerRadius }
            }).length;
            if (occupied) continue;
            const distance = Math.min(
                ...[...players.values()].map(({ body }) => Math.hypot(body.position.x - x, body.position.y - y))
            );
            if (distance > bestDistance) {
                best = { x, y };
                bestDistance = distance;
            }
        }
    }
    return best;
}

function simulate() {
    tick++;
    for (const player of players.values()) {
        if (player.isDead) {
            Matter.Body.setVelocity(player.body, { x: 0, y: 0 });
            player.body.isSensor = true;
            continue;
        }

        const dashing = player.dashUntil > Date.now();
        const moving = player.input.x !== 0 || player.input.y !== 0;
        if (!dashing && player.input.running && moving && player.stamina > 0) {
            player.stamina = Math.max(0, player.stamina - 20 / 60);
        } else if (!dashing && !player.input.running) {
            player.stamina = Math.min(100, player.stamina + 15 / 60);
        }

        const slowed = oilPuddles.some(
            ({ x, y, radius }) => Math.hypot(player.body.position.x - x, player.body.position.y - y) <= radius
        );
        const speed = dashing ? 50 : player.input.running && player.stamina > 0 ? 9 : 5;
        Matter.Body.setVelocity(player.body, {
            x: player.input.x * speed * (slowed ? 0.45 : 1),
            y: player.input.y * speed * (slowed ? 0.45 : 1)
        });
        Matter.Body.setAngle(player.body, player.input.rotation);
        player.body.isSensor = false;
    }
    Matter.Engine.update(engine, STEP_MS);

    for (const projectile of projectiles.values()) {
        if (Date.now() - projectile.createdAt > 3000) removeProjectile(projectile);
    }
}

function runSimulation() {
    const now = performance.now();
    simulationAccumulator += Math.min(now - lastSimulationAt, MAX_FRAME_MS);
    lastSimulationAt = now;
    while (simulationAccumulator >= STEP_MS) {
        const startedAt = performance.now();
        simulate();
        simulationDurations.push(performance.now() - startedAt);
        if (simulationDurations.length > 300) simulationDurations.shift();
        simulationAccumulator -= STEP_MS;
    }
}

function damagePlayer(player, damage) {
    if (player.isDead) return;
    player.hp = Math.max(0, player.hp - damage);
    player.isDead = player.hp === 0;
    events.push({ type: 'playerDamaged', id: player.id, x: 0, y: 0, damage });
}

function identifyWeapon(message) {
    if (message.damage === 55 && Math.abs(message.speed - 60) < 0.1 && message.piercing === true) return 'rifle';
    if (message.damage === 10 && Math.abs(message.speed - 30) < 0.1 && message.piercing !== true) return 'assaultRifle';
    if (message.damage === 5 && message.speed >= 12 && message.speed <= 18 && message.piercing !== true)
        return 'shotgun';
    return null;
}

function consumeWeaponShot(player, weaponName, now) {
    const weapon = player.weapons[weaponName];
    const config = WEAPONS[weaponName];
    if (weaponName === 'shotgun' && now <= weapon.burstUntil && weapon.burstCount < 32) {
        weapon.burstCount++;
        return config;
    }
    if (now - weapon.lastFiredAt < config.fireRate) return null;
    weapon.lastFiredAt = now;
    if (weaponName === 'shotgun') {
        weapon.burstUntil = now + 100;
        weapon.burstCount = 1;
    }
    return config;
}

function removeProjectile(projectile) {
    projectiles.delete(projectile.body.id);
    Matter.World.remove(engine.world, projectile.body);
    events.push({
        type: 'projectileDestroyed',
        id: projectile.id,
        x: projectile.body.position.x,
        y: projectile.body.position.y
    });
}

Matter.Events.on(engine, 'collisionStart', (event) => {
    for (const pair of event.pairs) {
        const projectile = projectiles.get(pair.bodyA.id) ?? projectiles.get(pair.bodyB.id);
        if (!projectile) continue;

        const targetBody = pair.bodyA === projectile.body ? pair.bodyB : pair.bodyA;
        if (projectiles.has(targetBody.id)) continue;
        const player = [...players.values()].find(({ body }) => body === targetBody);
        if (player) {
            if (player.id !== projectile.ownerId && !player.isDead) {
                damagePlayer(player, projectile.damage);
                removeProjectile(projectile);
            }
            continue;
        }

        const object = dynamicObjects.find(({ body }) => body === targetBody);
        if (object?.type === 'barrel') explodeObject(object);
        if (object?.type === 'oilTank') {
            object.health -= projectile.damage;
            if (object.health <= 0) ruptureOilTank(object);
        }

        const wall = wallSegments.find(({ body }) => body === targetBody);
        if (wall) {
            wall.health -= projectile.damage;
            if (wall.health <= 0) {
                wallSegments.splice(wallSegments.indexOf(wall), 1);
                Matter.World.remove(engine.world, wall.body);
                events.push({
                    type: 'thinWallDestroyed',
                    id: wall.id,
                    x: wall.body.position.x,
                    y: wall.body.position.y
                });
            }
            if (projectile.piercing) continue;
        }

        removeProjectile(projectile);
    }
});

function currentWorldLayout(center, radius = MIN_INTEREST_RADIUS) {
    const visible = ({ body }) =>
        !center || Math.hypot(body.position.x - center.x, body.position.y - center.y) <= radius;
    return {
        boxes: dynamicObjects
            .filter((record) => record.type === 'box' && visible(record))
            .map(({ id, body }) => ({ id, x: body.position.x, y: body.position.y })),
        barrels: dynamicObjects
            .filter((record) => record.type === 'barrel' && visible(record))
            .map(({ id, body }) => ({ id, x: body.position.x, y: body.position.y })),
        oilTank: dynamicObjects.find((record) => record.type === 'oilTank' && visible(record))
            ? (() => {
                  const tank = dynamicObjects.find((record) => record.type === 'oilTank' && visible(record));
                  return { id: tank.id, x: tank.body.position.x, y: tank.body.position.y };
              })()
            : null,
        thinWall: worldLayout.thinWall
            ? {
                  x: worldLayout.thinWall.x,
                  y: worldLayout.thinWall.y,
                  isVertical: worldLayout.thinWall.isVertical,
                  segments: wallSegments.filter(visible).map(({ id }) => ({ id, index: Number(id.split('-')[1]) }))
              }
            : null
    };
}

function snapshot() {
    const sortedDurations = simulationDurations.toSorted((a, b) => a - b);
    const playersState = [...players.values()].map(({ id, body, input, isDead, hp, stamina }) => ({
        id,
        x: body.position.x,
        y: body.position.y,
        rotation: input.rotation,
        isDead,
        hp,
        stamina,
        vx: body.velocity.x,
        vy: body.velocity.y
    }));
    const objectsState = [
        ...dynamicObjects.map(({ id, type, body }) => ({
            id,
            type,
            x: body.position.x,
            y: body.position.y,
            rotation: body.angle,
            vx: body.velocity.x,
            vy: body.velocity.y
        })),
        ...wallSegments.map(({ id, type, body }) => ({
            id,
            type,
            x: body.position.x,
            y: body.position.y,
            rotation: body.angle,
            vx: 0,
            vy: 0
        })),
        ...oilPuddles.map(({ id, x, y, radius }) => ({
            id,
            type: 'oilPuddle',
            x,
            y,
            radius,
            rotation: 0,
            vx: 0,
            vy: 0
        })),
        ...scorchMarks.map(({ id, x, y, radius }) => ({
            id,
            type: 'scorch',
            x,
            y,
            radius,
            rotation: 0,
            vx: 0,
            vy: 0
        })),
        ...[...projectiles.values()].map(({ id, body, ownerId, texture, frame, piercing }) => ({
            id,
            type: 'projectile',
            x: body.position.x,
            y: body.position.y,
            rotation: Math.atan2(body.velocity.y, body.velocity.x),
            vx: body.velocity.x,
            vy: body.velocity.y,
            ownerId,
            texture,
            frame,
            piercing
        }))
    ];
    return {
        tick,
        simulationP95: sortedDurations[Math.floor(sortedDurations.length * 0.95)] ?? 0,
        players: playersState,
        objects: objectsState,
        events
    };
}

function changedRecords(records, previous) {
    return records.filter((record) => JSON.stringify(record) !== JSON.stringify(previous.get(record.id)));
}

function broadcast() {
    const snap = snapshot();
    for (const recipient of players.values()) {
        if (recipient.socket && recipient.socket.readyState === recipient.socket.OPEN) {
            const center = recipient.body.position;
            const interestRadius = recipient.interestRadius;
            recipient.pendingEvents.push(
                ...(recipient.protocolVersion === 2
                    ? snap.events.filter(
                          (event) =>
                              event.id === recipient.id ||
                              Math.hypot(event.x - center.x, event.y - center.y) <= interestRadius + EFFECT_MARGIN
                      )
                    : snap.events)
            );
            if (recipient.pendingEvents.length > 1000)
                recipient.pendingEvents.splice(0, recipient.pendingEvents.length - 1000);
            if (recipient.socket.bufferedAmount > MAX_BUFFERED_BYTES) {
                recipient.needsBaseline = true;
                continue;
            }
            if (recipient.protocolVersion === 2) {
                const visiblePlayers = snap.players.filter(
                    (player) =>
                        player.id === recipient.id ||
                        Math.hypot(player.x - center.x, player.y - center.y) <= interestRadius
                );
                const visibleObjects = snap.objects.filter(
                    (object) => Math.hypot(object.x - center.x, object.y - center.y) <= interestRadius
                );
                const baseline = recipient.needsBaseline || tick - recipient.lastBaselineTick >= 300;
                recipient.socket.send(
                    JSON.stringify({
                        v: 2,
                        tick: snap.tick,
                        simulationP95: snap.simulationP95,
                        type: baseline ? 'snapshot' : 'snapshotDelta',
                        authoritative: true,
                        players: baseline ? visiblePlayers : changedRecords(visiblePlayers, recipient.previousPlayers),
                        ...(baseline
                            ? {}
                            : {
                                  removedPlayers: [...recipient.previousPlayers.keys()].filter(
                                      (id) => !visiblePlayers.some((player) => player.id === id)
                                  )
                              }),
                        objects: baseline ? visibleObjects : changedRecords(visibleObjects, recipient.previousObjects),
                        ...(baseline
                            ? {}
                            : {
                                  removedObjects: [...recipient.previousObjects.keys()].filter(
                                      (id) => !visibleObjects.some((object) => object.id === id)
                                  )
                              }),
                        events: recipient.pendingEvents
                    })
                );
                recipient.needsBaseline = false;
                if (baseline) recipient.lastBaselineTick = tick;
                recipient.previousPlayers = new Map(visiblePlayers.map((player) => [player.id, player]));
                recipient.previousObjects = new Map(visibleObjects.map((object) => [object.id, object]));
            } else {
                recipient.socket.send(
                    JSON.stringify({
                        type: 'snapshot',
                        authoritative: true,
                        players: snap.players,
                        objects: snap.objects,
                        events: recipient.pendingEvents
                    })
                );
            }
            recipient.pendingEvents = [];
        }
    }
    events = [];
}

function explodeObject(record) {
    const index = dynamicObjects.indexOf(record);
    if (index < 0) return;

    dynamicObjects.splice(index, 1);
    Matter.World.remove(engine.world, record.body);
    events.push({
        type: `${record.type}Exploded`,
        id: record.id,
        x: record.body.position.x,
        y: record.body.position.y
    });

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
            x: body.velocity.x + (dx / distance) * speed,
            y: body.velocity.y + (dy / distance) * speed
        });
    }

    if (record.type === 'barrel') {
        scorchMarks.push({
            id: `scorch-${record.id}`,
            x: record.body.position.x,
            y: record.body.position.y,
            radius: 350
        });
        for (const player of players.values()) {
            const distance = Math.hypot(
                player.body.position.x - record.body.position.x,
                player.body.position.y - record.body.position.y
            );
            if (distance > 500) continue;
            const damage = Math.round(100 * (1 - (distance / 500) ** 2));
            if (damage > 0) damagePlayer(player, damage);
        }
    }

    if (record.type !== 'barrel') return;
    for (const target of dynamicObjects.slice()) {
        if (target.type !== 'barrel') continue;
        if (
            Math.hypot(
                target.body.position.x - record.body.position.x,
                target.body.position.y - record.body.position.y
            ) <= 500
        ) {
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
    oilPuddles.push({ id: 'puddle-0', x, y, radius: 420 });
    for (let i = 0; i < 8; i++) {
        const angle = ((Math.PI * 2) / 8) * i;
        oilPuddles.push({
            id: `puddle-${i + 1}`,
            x: x + Math.cos(angle) * 390,
            y: y + Math.sin(angle) * 390,
            radius: 210
        });
    }
}

function onConnection(socket, request) {
    const params = new URL(request.url, 'ws://localhost').searchParams;
    const protocolVersion = params.get('v') === '2' ? 2 : 1;
    const requestedSession = params.get('session');
    const requestedRadius = Number(params.get('radius'));
    const interestRadius = Number.isFinite(requestedRadius)
        ? Math.max(MIN_INTEREST_RADIUS, Math.min(map.worldSize, requestedRadius))
        : MIN_INTEREST_RADIUS;
    let connectionPlayer = [...players.values()].find(({ sessionToken }) => sessionToken === requestedSession);
    if (!connectionPlayer) {
        if (players.size >= MAX_PLAYERS) {
            socket.close(1013, 'The game is full');
            return;
        }
        const id = randomUUID();
        const spawn = chooseSpawn();
        const body = Matter.Bodies.circle(spawn.x, spawn.y, map.playerRadius, {
            frictionAir: 0,
            inertia: Infinity
        });
        Matter.Body.setMass(body, 100);
        Matter.World.add(engine.world, body);
        connectionPlayer = {
            id,
            socket,
            sessionToken: randomUUID(),
            body,
            hp: 100,
            isDead: false,
            stamina: 100,
            dashUntil: 0,
            lastDashAt: -Infinity,
            fireWindowStartedAt: 0,
            fireCount: 0,
            protocolVersion,
            needsBaseline: true,
            lastBaselineTick: 0,
            previousPlayers: new Map(),
            previousObjects: new Map(),
            pendingEvents: [],
            interestRadius,
            weapons: {
                rifle: { lastFiredAt: -Infinity },
                assaultRifle: { lastFiredAt: -Infinity },
                shotgun: { lastFiredAt: -Infinity, burstUntil: 0, burstCount: 0 }
            },
            input: { x: 0, y: 0, rotation: 0, running: false }
        };
        players.set(id, connectionPlayer);
    } else {
        connectionPlayer.socket?.terminate();
        clearTimeout(connectionPlayer.disconnectTimer);
        connectionPlayer.socket = socket;
        connectionPlayer.protocolVersion = protocolVersion;
        connectionPlayer.interestRadius = interestRadius;
        connectionPlayer.needsBaseline = true;
        connectionPlayer.pendingEvents = [];
    }
    const { id, body, sessionToken } = connectionPlayer;
    socket.isAlive = true;
    socket.on('pong', () => {
        socket.isAlive = true;
    });
    socket.send(
        JSON.stringify({
            ...(protocolVersion === 2 ? { v: 2, tick } : {}),
            type: 'welcome',
            id,
            sessionToken,
            x: body.position.x,
            y: body.position.y,
            world: currentWorldLayout(protocolVersion === 2 ? body.position : null, interestRadius)
        })
    );
    broadcast();

    socket.on('message', (rawMessage) => {
        if (players.get(id)?.socket !== socket) return;
        try {
            const message = JSON.parse(rawMessage.toString());
            if (message.type === 'fire') {
                const player = players.get(id);
                if (!player) return;
                const now = Date.now();
                if (now - player.fireWindowStartedAt >= 1000) {
                    player.fireWindowStartedAt = now;
                    player.fireCount = 0;
                }
                if (++player.fireCount > MAX_FIRE_PER_SECOND) return;
                if (
                    typeof message.id !== 'string' ||
                    !UUID_PATTERN.test(message.id) ||
                    typeof message.texture !== 'string' ||
                    typeof message.frame !== 'string' ||
                    message.texture.length > 64 ||
                    message.frame.length > 64 ||
                    ![message.x, message.y, message.angle, message.speed, message.damage].every(Number.isFinite)
                )
                    return;
                const weaponName = identifyWeapon(message);
                if (
                    player.isDead ||
                    !weaponName ||
                    [...projectiles.values()].some(({ id: projectileId }) => projectileId === message.id) ||
                    Math.hypot(message.x - player.body.position.x, message.y - player.body.position.y) > 300
                )
                    return;

                const weapon = consumeWeaponShot(player, weaponName, now);
                if (!weapon) return;
                const speed = weaponName === 'shotgun' ? message.speed : weapon.speed;
                const damage = weapon.damage;
                const piercing = weapon.piercing;

                const projectileBody = Matter.Bodies.circle(message.x, message.y, 4, {
                    isSensor: true,
                    frictionAir: 0
                });
                Matter.Body.setVelocity(projectileBody, {
                    x: Math.cos(message.angle) * speed,
                    y: Math.sin(message.angle) * speed
                });
                Matter.World.add(engine.world, projectileBody);
                projectiles.set(projectileBody.id, {
                    id: message.id,
                    body: projectileBody,
                    ownerId: id,
                    damage,
                    piercing,
                    texture: 'projectiles',
                    frame: 'bullet',
                    createdAt: Date.now()
                });
                events.push({
                    type: 'projectileFired',
                    id: message.id,
                    x: message.x,
                    y: message.y,
                    angle: message.angle,
                    speed,
                    damage,
                    texture: 'projectiles',
                    frame: 'bullet',
                    piercing,
                    playerId: id
                });
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
                if (Number.isFinite(message.interestRadius)) {
                    player.interestRadius = Math.max(
                        MIN_INTEREST_RADIUS,
                        Math.min(map.worldSize, message.interestRadius)
                    );
                }
                const now = Date.now();
                if (
                    message.dash === true &&
                    now - player.lastDashAt >= 1000 &&
                    player.stamina >= 30 &&
                    !player.isDead
                ) {
                    player.stamina -= 30;
                    player.lastDashAt = now;
                    player.dashUntil = now + 250;
                }
            }
        } catch {
            // Некорректные сообщения просто игнорируются.
        }
    });

    socket.on('close', (code, reason) => {
        const player = players.get(id);
        if (!player || player.socket !== socket) return;
        if (code === 1000 && reason.toString() === 'restart') {
            Matter.World.remove(engine.world, player.body);
            players.delete(id);
            broadcast();
            return;
        }
        player.socket = null;
        player.input.x = 0;
        player.input.y = 0;
        player.input.running = false;
        player.dashUntil = 0;
        player.disconnectTimer = setTimeout(() => {
            if (player.socket) return;
            Matter.World.remove(engine.world, player.body);
            players.delete(id);
            broadcast();
        }, 15000);
    });
}

function start({ port, server } = {}) {
    if (wssInstance) return wssInstance;

    const options = { maxPayload: 16 * 1024, perMessageDeflate: { threshold: 1024 } };
    if (port !== undefined) {
        wssInstance = new WebSocketServer({ port, ...options });
    } else {
        wssInstance = new WebSocketServer({ noServer: true, ...options });
        server.on('upgrade', (request, socket, head) => {
            if (new URL(request.url, 'http://localhost').pathname !== '/ws') return;
            wssInstance.handleUpgrade(request, socket, head, (webSocket) => {
                wssInstance.emit('connection', webSocket, request);
            });
        });
    }
    wssInstance.on('connection', onConnection);

    createWorld();
    lastSimulationAt = performance.now();
    simulationAccumulator = 0;
    tick = 0;
    setInterval(runSimulation, STEP_MS);
    setInterval(broadcast, 1000 / 30);
    setInterval(() => {
        for (const socket of wssInstance.clients) {
            if (!socket.isAlive) {
                socket.terminate();
                continue;
            }
            socket.isAlive = false;
            socket.ping();
        }
    }, 30000);
    console.log(
        port !== undefined
            ? `Multiplayer MVP is listening on ws://localhost:${port}`
            : 'Multiplayer MVP is mounted at /ws on the provided http-server'
    );
    return wssInstance;
}

if (require.main === module) {
    start({ port: PORT });
} else {
    module.exports = { start };
}
