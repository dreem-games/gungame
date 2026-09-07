import Phaser from 'phaser';

import map from '../../multiplayer-map.json';
import { EntityManager } from '../core/EntityManager';
import { FlashManager } from '../core/FlashManager';
import { InputManager } from '../core/InputManager';
import { NetworkManager, WorldLayout, WorldObjectState } from '../core/NetworkManager';
import { Barrel } from '../objects/Barrel';
import { Hero } from '../objects/Hero';
import { OilTank } from '../objects/OilTank';
import { Projectile } from '../objects/Projectile';
import { ThinWall, ThinWallSegment } from '../objects/ThinWall';

export class GameScene extends Phaser.Scene {
    private entityManager!: EntityManager;
    public flashManager!: FlashManager;
    private inputManager!: InputManager;
    private hero!: Hero;
    private network!: NetworkManager;
    private remotePlayers = new Map<string, Phaser.GameObjects.Sprite>();
    private boxes = new Map<string, Phaser.Physics.Matter.Sprite>();
    private projectiles = new Map<string, Phaser.Physics.Matter.Sprite>();
    private networkProjectiles = new Set<string>();
    private puddles = new Map<string, Phaser.GameObjects.Graphics>();
    private scorchMarks = new Map<string, Phaser.GameObjects.Sprite>();
    private connectionText!: Phaser.GameObjects.Text;
    private enterKey: Phaser.Input.Keyboard.Key | null = null;
    private builtWelcomeSequence = 0;

    constructor() {
        super('GameScene');
    }

    create() {
        // Scene-инстанс переживает restart, а игровые объекты — нет.
        this.remotePlayers.clear();
        this.boxes.clear();
        this.projectiles.clear();
        this.networkProjectiles.clear();
        this.puddles.clear();
        this.scorchMarks.clear();
        this.builtWelcomeSequence = 0;

        // Initialize Core Systems
        this.entityManager = new EntityManager();
        this.inputManager = new InputManager(this);
        this.flashManager = new FlashManager(this);

        // Setup explosion animation
        if (!this.anims.exists('explosion_anim')) {
            this.anims.create({
                key: 'explosion_anim',
                frames: this.anims.generateFrameNames('explosion', {
                    prefix: 'explosion_',
                    start: 1,
                    end: 25,
                    zeroPad: 2
                }),
                frameRate: 30,
                repeat: 0
            });
        }

        // Projectiles in our system are set up as sensors (setSensor(true)),
        // so they don't produce physical pushing/bouncing forces against other objects.
        // Мы только контролируем, запускается ли наша своя логика коллизий.
        this.matter.world.on('collisionstart', (event: Phaser.Physics.Matter.Events.CollisionStartEvent) => {
            event.pairs.forEach((pair) => {
                const bodyA = pair.bodyA as MatterJS.BodyType;
                const bodyB = pair.bodyB as MatterJS.BodyType;

                const gameObjectA = bodyA.gameObject as Phaser.GameObjects.GameObject;
                const gameObjectB = bodyB.gameObject as Phaser.GameObjects.GameObject;

                if (!gameObjectA || !gameObjectB) return;

                // Локальный hero — не цель для локальных расчётов: его здоровьем
                // распоряжается сервер (playerDamaged), своими пулями — тоже сервер.
                if (gameObjectA === this.hero || gameObjectB === this.hero) return;

                this.handleProjectileCollision(gameObjectA, gameObjectB);
            });
        });

        const WORLD_SIZE = map.worldSize;
        const TILE_SIZE = 128; // Wall tile size reduced by half

        // Set world bounds
        this.matter.world.setBounds(0, 0, WORLD_SIZE, WORLD_SIZE);

        // Add ground texture (also visually scale down the grass tile)
        const ground = this.add.tileSprite(
            WORLD_SIZE / 2,
            WORLD_SIZE / 2,
            WORLD_SIZE * 2,
            WORLD_SIZE * 2,
            'level1',
            'grass'
        );
        ground.setDepth(-2);
        ground.setScale(0.5);

        // Add procedural noise overlay on top of grass
        const noiseScale = 4.0; // scale up the texture size to cover more area
        const noise = this.add.tileSprite(
            WORLD_SIZE / 2,
            WORLD_SIZE / 2,
            WORLD_SIZE / noiseScale,
            WORLD_SIZE / noiseScale,
            'grass_noise'
        );
        noise.setBlendMode(Phaser.BlendModes.MULTIPLY); // MULTIPLY makes dark areas darker, light areas transparent
        noise.setAlpha(0.7); // Adjust intensity of the noise
        noise.setDepth(-1);
        noise.setScale(noiseScale);

        // Generate border walls
        this.generateBorderWalls(WORLD_SIZE, TILE_SIZE);

        // Create Hero in the center
        this.hero = new Hero(this, WORLD_SIZE / 2, WORLD_SIZE / 2, this.inputManager);
        this.hero.setDepth(1);
        this.entityManager.add(this.hero);

        // Camera setup
        this.cameras.main.startFollow(this.hero);
        this.cameras.main.setBounds(0, 0, WORLD_SIZE, WORLD_SIZE);
        // Make everything appear 2 times smaller (see 2x more space)
        this.cameras.main.setZoom(0.5);

        this.network = new NetworkManager(this.getInterestRadius());
        this.events.once(Phaser.Scenes.Events.SHUTDOWN, () => {
            this.network.destroy();
            this.events.off('projectileFired');
        });

        this.events.on('projectileFired', (data: any) => {
            this.projectiles.set(data.id, data.gameObject);
            data.gameObject.once('destroy', () => this.projectiles.delete(data.id));
            this.network.sendFire(
                data.id,
                data.x,
                data.y,
                data.angle,
                data.speed,
                data.damage,
                data.texture,
                data.frame,
                data.piercing
            );
        });

        // Start UI scene
        this.scene.launch('UIScene');

        // Оверлей соединения: «Подключение…» до welcome, «потеряно» при обрыве.
        // ENTER после обрыва рестартит сцены — свежий сокет (полный backoff придёт с п. 21(d)).
        this.connectionText = this.add
            .text(0, 0, '', {
                font: '24px monospace',
                color: '#ffffff',
                backgroundColor: 'rgba(0,0,0,0.6)',
                padding: { x: 16, y: 8 }
            })
            .setOrigin(0.5)
            .setDepth(1000)
            .setScrollFactor(0)
            .setVisible(false);
        this.enterKey = this.input.keyboard?.addKey('ENTER') ?? null;

        // Emit initial weapon state so UI can render
        this.events.once('update', () => {
            const wm = this.hero.getWeaponManager();
            const wp = wm.getCurrentWeapon();
            this.game.events.emit('weaponChanged', wp);
            this.game.events.emit('ammoChanged', wp.currentAmmo, wp.stats.maxAmmo);
        });
    }

    private handleProjectileCollision(
        gameObjectA: Phaser.GameObjects.GameObject,
        gameObjectB: Phaser.GameObjects.GameObject
    ) {
        const isProjA = gameObjectA.getData('isProjectile');
        const isProjB = gameObjectB.getData('isProjectile');

        if (isProjA && isProjB) return; // Projectiles don't collide with each other
        if (isProjA && (gameObjectA.getData('ignoredBodies') || []).includes(gameObjectB)) return;
        if (isProjB && (gameObjectB.getData('ignoredBodies') || []).includes(gameObjectA)) return;

        if (isProjA) {
            this.processHit(gameObjectA, gameObjectB);
        } else if (isProjB) {
            this.processHit(gameObjectB, gameObjectA);
        }
    }

    private processHit(projectile: Phaser.GameObjects.GameObject, target: Phaser.GameObjects.GameObject) {
        if (target instanceof Hero && target.isDead) {
            // Ignore dead heroes
            return;
        }

        // Локально пуля всегда гаснет по контакту; настоящим уроном управляет сервер.
        // Исключение: pierce через тонкую стену — сегмент игнорируем, пуля живёт дальше
        // до решения сервера (projectileDestroyed).
        const isPiercing = projectile.getData('isPiercing');
        const isThinWall = target.getData?.('isThinWall') === true;
        if (isThinWall && isPiercing) {
            const ignoredBodies: Phaser.GameObjects.GameObject[] = projectile.getData('ignoredBodies') || [];
            ignoredBodies.push(target);
            projectile.setData('ignoredBodies', ignoredBodies);
            return;
        }
        projectile.destroy();
    }

    private createSharedObstacles(layout: WorldLayout) {
        for (const box of layout.boxes) {
            const sprite = this.matter.add.sprite(box.x, box.y, 'level1', 'box');
            sprite.setBody({ type: 'rectangle', width: 256, height: 256 });
            sprite.setScale(0.5);
            sprite.setFrictionAir(0.1);
            sprite.setMass(70);
            sprite.setData('blocksVision', true);
            sprite.setData('networkId', box.id);
            this.boxes.set(box.id, sprite);
        }

        for (const barrel of layout.barrels) {
            const sprite = new Barrel(this, barrel.x, barrel.y);
            sprite.setData('networkId', barrel.id);
            this.boxes.set(barrel.id, sprite);
        }

        if (layout.oilTank) {
            const sprite = new OilTank(this, layout.oilTank.x, layout.oilTank.y);
            sprite.setData('networkId', layout.oilTank.id);
            this.boxes.set(layout.oilTank.id, sprite);
        }

        if (layout.thinWall) {
            const wall = new ThinWall(this, layout.thinWall.x, layout.thinWall.y, 256, layout.thinWall.isVertical);
            const segments = wall.getSegments();
            const alive = new Map(layout.thinWall.segments.map((s) => [s.index, s.id]));
            segments.forEach((segment, index) => {
                const id = alive.get(index);
                if (!id) return segment.destroy();
                segment.setData('networkId', id);
                this.boxes.set(id, segment);
            });
        }
    }

    private generateBorderWalls(worldSize: number, tileSize: number) {
        // Top and Bottom walls
        for (let x = tileSize / 2; x <= worldSize; x += tileSize) {
            this.createWall(x, tileSize / 2, tileSize); // Top
            this.createWall(x, worldSize - tileSize / 2, tileSize); // Bottom
        }

        // Left and Right walls (skipping corners to avoid overlap)
        for (let y = tileSize + tileSize / 2; y < worldSize - tileSize; y += tileSize) {
            this.createWall(tileSize / 2, y, tileSize); // Left
            this.createWall(worldSize - tileSize / 2, y, tileSize); // Right
        }
    }

    private createWall(x: number, y: number, _size: number) {
        const wall = this.matter.add.sprite(x, y, 'level1', 'wall');
        // We want the hitbox to match the visible block exactly to prevent gaps.
        // Since we scale by 0.5, we must pass the unscaled original size (256).
        wall.setBody({ type: 'rectangle', width: 256, height: 256 });
        wall.setScale(0.5);
        wall.setStatic(true);
        wall.setData('blocksVision', true);
    }

    update(time: number, delta: number) {
        this.entityManager.update(time, delta);
        this.inputManager.update();

        const connected = this.network.isConnected();
        if (connected) {
            if (this.network.consumeSessionReplacement()) {
                const ui = this.scene.get('UIScene') as Phaser.Scene;
                ui.scene.restart();
                this.scene.restart();
                return;
            }
            const welcomeSequence = this.network.getWelcomeSequence();
            if (welcomeSequence !== this.builtWelcomeSequence) {
                const layout = this.network.getWorldLayout();
                const spawn = this.network.getSpawnPosition();
                if (layout && spawn) {
                    for (const object of this.boxes.values()) object.destroy();
                    this.boxes.clear();
                    this.hero.setPosition(spawn.x, spawn.y);
                    this.createSharedObstacles(layout);
                    this.builtWelcomeSequence = welcomeSequence;
                }
            }

            const movement = this.inputManager.getMovementVector();
            this.network.sendInput(
                movement.x,
                movement.y,
                this.hero.rotation,
                this.inputManager.isRunning(),
                this.hero.isDashingNow(),
                this.getInterestRadius()
            );
            const worldObjects = this.network.getWorldObjects();
            for (const object of worldObjects) {
                if (object.type === 'projectile') {
                    this.syncNetworkProjectile(object);
                    continue;
                }
                if (object.type === 'oilPuddle') {
                    this.syncPuddle(object);
                    continue;
                }
                if (object.type === 'scorch') {
                    this.syncScorch(object);
                    continue;
                }
                const box = this.boxes.get(object.id) ?? this.createNetworkObject(object);
                if (box) {
                    const distance = Phaser.Math.Distance.Between(box.x, box.y, object.x, object.y);
                    if (distance > 80) {
                        box.setPosition(object.x, object.y);
                    } else {
                        const k = Math.min(1, delta / 120);
                        box.setPosition(Phaser.Math.Linear(box.x, object.x, k), Phaser.Math.Linear(box.y, object.y, k));
                    }
                    box.setRotation(object.rotation);
                    box.setVelocity(object.vx, object.vy);
                }
            }
            this.applyServerEvents();
            if (this.network.hasReceivedWorldState()) {
                const visible = new Set(
                    worldObjects
                        .filter(({ type }) => type !== 'projectile' && type !== 'oilPuddle' && type !== 'scorch')
                        .map(({ id }) => id)
                );
                for (const [id, object] of this.boxes) {
                    if (visible.has(id)) continue;
                    object.destroy();
                    this.boxes.delete(id);
                }
                const visibleProjectiles = new Set(
                    worldObjects.filter(({ type }) => type === 'projectile').map(({ id }) => id)
                );
                for (const id of this.networkProjectiles) {
                    if (visibleProjectiles.has(id)) continue;
                    this.projectiles.get(id)?.destroy();
                    this.projectiles.delete(id);
                    this.networkProjectiles.delete(id);
                }
                const visiblePuddles = new Set(
                    worldObjects.filter(({ type }) => type === 'oilPuddle').map(({ id }) => id)
                );
                for (const [id, puddle] of this.puddles) {
                    if (visiblePuddles.has(id)) continue;
                    puddle.destroy();
                    this.puddles.delete(id);
                }
                const visibleScorches = new Set(
                    worldObjects.filter(({ type }) => type === 'scorch').map(({ id }) => id)
                );
                for (const [id, scorch] of this.scorchMarks) {
                    if (visibleScorches.has(id)) continue;
                    scorch.destroy();
                    this.scorchMarks.delete(id);
                }
            }
            this.reconcileLocalPlayer(delta);
            this.updateRemotePlayers();
        }
        this.updateConnectionOverlay(connected);
    }

    private getInterestRadius(): number {
        const camera = this.cameras.main;
        return Math.hypot(camera.width, camera.height) / (2 * camera.zoom) + 512;
    }

    private updateConnectionOverlay(connected: boolean) {
        const lost = this.network.hasLostConnection();
        const dead = this.hero.isDead;
        if (lost && this.enterKey && Phaser.Input.Keyboard.JustDown(this.enterKey)) {
            this.network.retryNow();
            return;
        }
        if (dead && this.enterKey && Phaser.Input.Keyboard.JustDown(this.enterKey)) {
            const ui = this.scene.get('UIScene') as Phaser.Scene;
            ui.scene.restart();
            this.scene.restart();
            return;
        }
        this.connectionText.setPosition(this.cameras.main.width / 2, this.cameras.main.height / 2);
        if (dead) {
            this.connectionText.setText('Вы погибли — нажмите ENTER').setVisible(true);
        } else if (lost) {
            this.connectionText.setText('Соединение потеряно — нажмите ENTER').setVisible(true);
        } else if (!connected) {
            this.connectionText.setText('Подключение к серверу…').setVisible(true);
        } else {
            this.connectionText.setVisible(false);
        }
    }

    private applyServerEvents() {
        for (const event of this.network.consumeWorldEvents()) {
            if (event.type === 'projectileFired') {
                if (this.network.getLocalPlayer()?.id !== event.playerId && !this.projectiles.has(event.id)) {
                    const projectile = new Projectile(
                        this,
                        event.x,
                        event.y,
                        event.angle!,
                        event.speed!,
                        event.damage!,
                        event.texture!,
                        event.frame!,
                        event.piercing,
                        true,
                        event.id
                    );
                    this.projectiles.set(event.id, projectile.gameObject);
                    projectile.gameObject.once('destroy', () => this.projectiles.delete(event.id));
                }
                continue;
            }
            if (event.type === 'projectileDestroyed') {
                this.projectiles.get(event.id)?.destroy();
                this.projectiles.delete(event.id);
                continue;
            }
            if (event.type === 'playerDamaged') {
                if (this.network.getLocalPlayer()?.id === event.id && event.damage) this.hero.takeDamage(event.damage);
                continue;
            }
            const object = this.boxes.get(event.id);
            if (event.type === 'oilTankRuptured') {
                if (object instanceof OilTank) object.destroy();
                this.boxes.delete(event.id);
                continue;
            }
            if (event.type === 'thinWallDestroyed') {
                object?.destroy();
                this.boxes.delete(event.id);
                continue;
            }
            if (event.type !== 'barrelExploded') continue;
            object?.destroy();
            this.boxes.delete(event.id);

            const radius = 500;
            const explosion = this.add.sprite(event.x, event.y, 'explosion', 'explosion_10').setDepth(5);
            explosion.setScale((radius * 2) / 64);
            explosion.play('explosion_anim');
            explosion.once('animationcomplete', () => explosion.destroy());
            this.sound.play('barrel_explosion');
            this.cameras.main.shake(400, 0.008);
            this.flashManager.createExplosionFlash(event.x, event.y, radius);
        }
    }

    private reconcileLocalPlayer(delta: number) {
        const state = this.network.getLocalPlayer();
        if (!state) return;
        if (state.hp !== undefined) this.hero.applyServerHealth(state.hp);
        if (state.stamina !== undefined) this.hero.applyServerStamina(state.stamina);

        const distance = Phaser.Math.Distance.Between(this.hero.x, this.hero.y, state.x, state.y);
        if (distance < 6) return;
        if (distance > 80) {
            this.hero.setPosition(state.x, state.y);
        } else {
            const k = Math.min(1, delta / 60);
            this.hero.setPosition(
                Phaser.Math.Linear(this.hero.x, state.x, k),
                Phaser.Math.Linear(this.hero.y, state.y, k)
            );
        }
        this.hero.setVelocity(state.vx ?? 0, state.vy ?? 0);
    }

    private createNetworkObject(state: { id: string; type: string; x: number; y: number }) {
        let object: Phaser.Physics.Matter.Sprite;
        if (state.type === 'barrel') {
            object = new Barrel(this, state.x, state.y);
        } else if (state.type === 'oilTank') {
            object = new OilTank(this, state.x, state.y);
        } else if (state.type === 'thinWall') {
            object = new ThinWallSegment(this, state.x, state.y);
        } else if (state.type === 'box') {
            object = this.matter.add.sprite(state.x, state.y, 'level1', 'box');
            object.setBody({ type: 'rectangle', width: 256, height: 256 });
            object.setScale(0.5);
            object.setFrictionAir(0.1);
            object.setMass(70);
            object.setData('blocksVision', true);
        } else {
            return null;
        }
        object.setData('networkId', state.id);
        this.boxes.set(state.id, object);
        return object;
    }

    private syncNetworkProjectile(state: WorldObjectState) {
        let object = this.projectiles.get(state.id);
        if (!object) {
            const projectile = new Projectile(
                this,
                state.x,
                state.y,
                state.rotation,
                0,
                0,
                state.texture ?? 'projectiles',
                state.frame ?? 'bullet',
                state.piercing,
                true,
                state.id
            );
            object = projectile.gameObject;
            this.projectiles.set(state.id, object);
            object.once('destroy', () => this.projectiles.delete(state.id));
        }
        this.networkProjectiles.add(state.id);
        object.setPosition(state.x, state.y);
        object.setRotation(state.rotation);
        object.setVelocity(state.vx, state.vy);
    }

    private syncScorch(state: WorldObjectState) {
        if (this.scorchMarks.has(state.id)) return;
        const radius = state.radius ?? 350;
        this.scorchMarks.set(
            state.id,
            this.add
                .sprite(state.x, state.y, 'scorch')
                .setDisplaySize(radius * 2, radius * 2)
                .setDepth(-1)
        );
    }

    private syncPuddle(state: WorldObjectState) {
        if (this.puddles.has(state.id)) return;
        const puddle = this.add.graphics().setDepth(-0.5);
        puddle.fillStyle(0x1a2b1a, 0.8);
        puddle.fillCircle(state.x, state.y, state.radius ?? 0);
        this.puddles.set(state.id, puddle);
    }

    private updateRemotePlayers() {
        const states = this.network.getRemotePlayers();
        for (const [id, state] of states) {
            let player = this.remotePlayers.get(id);
            if (!player) {
                player = this.add.sprite(state.x, state.y, 'hero', 'hero').setTint(0x66ccff).setAlpha(0.8).setDepth(1);
                player.setOrigin(0.2, 0.5);
                this.remotePlayers.set(id, player);
            }

            player.x = state.x;
            player.y = state.y;
            player.rotation = state.rotation;
            if (state.isDead && player.frame.name !== 'hero_dead') {
                player.setFrame('hero_dead');
                player.setDepth(-0.5);
            } else if (!state.isDead && player.frame.name === 'hero_dead') {
                player.setFrame('hero');
                player.setDepth(1);
            }
        }

        for (const [id, player] of this.remotePlayers) {
            if (!states.has(id)) {
                player.destroy();
                this.remotePlayers.delete(id);
            }
        }
    }
}
