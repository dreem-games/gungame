import Phaser from 'phaser';

import map from '../../multiplayer-map.json';
import { EntityManager } from '../core/EntityManager';
import { FlashManager } from '../core/FlashManager';
import { InputManager } from '../core/InputManager';
import { generateWorld } from '../core/map-gen';
import { NetworkManager, WorldLayout } from '../core/NetworkManager';
import { Barrel } from '../objects/Barrel';
import { Hero } from '../objects/Hero';
import { OilTank } from '../objects/OilTank';
import { Projectile } from '../objects/Projectile';
import { ThinWall } from '../objects/ThinWall';

export class GameScene extends Phaser.Scene {
    private entityManager!: EntityManager;
    public flashManager!: FlashManager;
    private inputManager!: InputManager;
    private hero!: Hero;
    private barrels: Barrel[] = [];
    private network!: NetworkManager;
    private remotePlayers = new Map<string, Phaser.GameObjects.Sprite>();
    private boxes = new Map<string, Phaser.Physics.Matter.Sprite>();
    private localEnvironment: Phaser.GameObjects.GameObject[] = [];
    private usesServerPhysics = false;
    private multiplayer = true;

    constructor() {
        super('GameScene');
    }

    create() {
        // Initialize Core Systems
        this.entityManager = new EntityManager();
        this.inputManager = new InputManager(this);
        this.flashManager = new FlashManager(this);

        // Setup explosion animation
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

        // Projectiles in our system are set up as sensors (setSensor(true)),
        // so they don't produce physical pushing/bouncing forces against other objects.
        // We only need to control whether our custom collision logic runs.
        this.matter.world.on('collisionactive', (event: Phaser.Physics.Matter.Events.CollisionActiveEvent) => {
            event.pairs.forEach((pair) => {
                const bodyA = pair.bodyA as MatterJS.BodyType;
                const bodyB = pair.bodyB as MatterJS.BodyType;

                const isPuddleA = (bodyA as any).isPuddle;
                const isPuddleB = (bodyB as any).isPuddle;

                // If hero is in the puddle, slow them down
                if (isPuddleA && bodyB.gameObject === this.hero) {
                    this.hero.setSlowed(true);
                } else if (isPuddleB && bodyA.gameObject === this.hero) {
                    this.hero.setSlowed(true);
                }
            });
        });

        this.matter.world.on('collisionstart', (event: Phaser.Physics.Matter.Events.CollisionStartEvent) => {
            event.pairs.forEach((pair) => {
                const bodyA = pair.bodyA as MatterJS.BodyType;
                const bodyB = pair.bodyB as MatterJS.BodyType;

                const gameObjectA = bodyA.gameObject as Phaser.GameObjects.GameObject;
                const gameObjectB = bodyB.gameObject as Phaser.GameObjects.GameObject;

                if (!gameObjectA || !gameObjectB) return;

                // Check if one is a projectile and the other is ignored
                const isProjA = gameObjectA.getData('isProjectile');
                const isProjB = gameObjectB.getData('isProjectile');

                if (isProjA) {
                    const ignoredBodies: Phaser.GameObjects.GameObject[] = gameObjectA.getData('ignoredBodies') || [];
                    if (ignoredBodies.includes(gameObjectB)) {
                        return; // Ignore this collision completely
                    }
                    if (gameObjectB instanceof Hero && bodyB.label === 'hero_movement') {
                        return; // Ignore collisions with movement body, only hitbox matters
                    }
                }

                if (isProjB) {
                    const ignoredBodies: Phaser.GameObjects.GameObject[] = gameObjectB.getData('ignoredBodies') || [];
                    if (ignoredBodies.includes(gameObjectA)) {
                        return; // Ignore this collision completely
                    }
                    if (gameObjectA instanceof Hero && bodyA.label === 'hero_movement') {
                        return; // Ignore collisions with movement body, only hitbox matters
                    }
                }

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

        this.createLocalEnvironment(WORLD_SIZE);

        // Create Hero in the center
        this.hero = new Hero(this, WORLD_SIZE / 2, WORLD_SIZE / 2, this.inputManager);
        this.hero.setDepth(1);
        this.entityManager.add(this.hero);
        if (this.multiplayer) {
            this.network = new NetworkManager();
            this.events.once(Phaser.Scenes.Events.SHUTDOWN, () => this.network.destroy());
        }

        this.events.on('projectileFired', (data: any) => {
            if (this.multiplayer && this.network) {
                this.network.sendFire(data.x, data.y, data.angle, data.speed, data.damage, data.texture, data.frame);
            }
        });

        // Camera setup
        this.cameras.main.startFollow(this.hero);
        this.cameras.main.setBounds(0, 0, WORLD_SIZE, WORLD_SIZE);
        // Make everything appear 2 times smaller (see 2x more space)
        this.cameras.main.setZoom(0.5);

        // Start UI scene
        this.scene.launch('UIScene');

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

        if (isProjA) {
            this.processHit(gameObjectA, gameObjectB);
        } else if (isProjB) {
            this.processHit(gameObjectB, gameObjectA);
        }
    }

    private processHit(projectile: Phaser.GameObjects.GameObject, target: Phaser.GameObjects.GameObject) {
        const damage = projectile.getData('damage');
        const isPiercing = projectile.getData('isPiercing');

        const networkId = target.getData('networkId');
        if (this.usesServerPhysics && typeof networkId === 'string') {
            this.network.sendHit(networkId, damage);
            const isThinWall = target.getData('isThinWall');
            const isHero = target instanceof Hero;
            if ((isThinWall && isPiercing) || isHero) {
                const ignoredBodies: Phaser.GameObjects.GameObject[] = projectile.getData('ignoredBodies') || [];
                ignoredBodies.push(target);
                projectile.setData('ignoredBodies', ignoredBodies);
                return;
            }
            projectile.destroy();
            return;
        }

        // Check if target is a barrel — it explodes instead of taking generic damage
        if (target instanceof Barrel) {
            target.explode(this.hero, this.barrels);
            projectile.destroy();
            return;
        }

        if (target) {
            // Check properties before applying damage, because taking damage might destroy the target
            const isThinWall = (target as any).getData ? (target as any).getData('isThinWall') : false;

            // Apply damage if target supports it
            if ((target as any).takeDamage) {
                (target as any).takeDamage(damage);
            }

            // If the target is a thin wall and the projectile is piercing, do NOT destroy it
            const isHero = target instanceof Hero;
            if ((isThinWall && isPiercing) || isHero) {
                // Ignore the segment so we don't hit it again immediately
                const ignoredBodies: Phaser.GameObjects.GameObject[] = projectile.getData('ignoredBodies') || [];
                ignoredBodies.push(target);
                projectile.setData('ignoredBodies', ignoredBodies);
                return; // Projectile survives
            }

            // Target taking damage logic for enemies would go here
            // if (target instanceof Enemy) target.takeDamage(damage);
        }

        // Projectile is destroyed
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
            this.barrels.push(sprite);
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

    private createLocalEnvironment(worldSize: number) {
        const world = generateWorld(worldSize);

        for (const box of world.boxes) {
            const sprite = this.matter.add.sprite(box.x, box.y, 'level1', 'box');
            sprite.setBody({ type: 'rectangle', width: 256, height: 256 });
            sprite.setScale(0.5);
            sprite.setFrictionAir(0.1);
            sprite.setMass(70);
            sprite.setData('blocksVision', true);
            this.localEnvironment.push(sprite);
        }

        for (const barrel of world.barrels) {
            const sprite = new Barrel(this, barrel.x, barrel.y);
            this.barrels.push(sprite);
            this.localEnvironment.push(sprite);
        }

        if (world.oilTank) {
            const tank = new OilTank(this, world.oilTank.x, world.oilTank.y);
            this.localEnvironment.push(tank);
        }

        if (world.thinWall) {
            const wall = new ThinWall(this, world.thinWall.x, world.thinWall.y, 256, world.thinWall.isVertical);
            this.localEnvironment.push(...wall.getSegments());
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
        if (this.multiplayer && this.network) {
            const movement = this.inputManager.getMovementVector();
            this.network.sendInput(
                movement.x,
                movement.y,
                this.hero.rotation,
                this.inputManager.isRunning(),
                this.hero.isDashingNow(),
                this.hero.isDead
            );
            const objects = this.network.getWorldObjects();
            if (this.network.isConnected() && objects.length) {
                if (!this.usesServerPhysics) {
                    this.localEnvironment.forEach((obj) => obj.destroy());
                    this.localEnvironment = [];
                    this.barrels = [];
                    this.boxes.clear();
                    const layout = this.network.getWorldLayout();
                    if (layout) this.createSharedObstacles(layout);
                    this.hero.setFrictionAir(0);
                    this.usesServerPhysics = true;
                }
                for (const object of objects) {
                    const box = this.boxes.get(object.id);
                    if (box) {
                        const distance = Phaser.Math.Distance.Between(box.x, box.y, object.x, object.y);
                        if (distance > 80) {
                            box.setPosition(object.x, object.y);
                        } else {
                            const k = Math.min(1, delta / 120);
                            box.setPosition(
                                Phaser.Math.Linear(box.x, object.x, k),
                                Phaser.Math.Linear(box.y, object.y, k)
                            );
                        }
                        box.setRotation(object.rotation);
                        box.setVelocity(object.vx, object.vy);
                    }
                }
                this.applyServerEvents();
                this.reconcileLocalPlayer(delta);
                this.updateRemotePlayers(delta);
            }
        }
    }

    private applyServerEvents() {
        for (const event of this.network.consumeWorldEvents()) {
            if (event.type === 'projectileFired') {
                if (this.network.getLocalPlayer()?.id !== event.playerId) {
                    new Projectile(
                        this,
                        event.x,
                        event.y,
                        event.angle!,
                        event.speed!,
                        event.damage!,
                        event.texture!,
                        event.frame!,
                        false,
                        true
                    );
                }
                continue;
            }
            if (event.type === 'playerDamaged') {
                if (this.network.getLocalPlayer()?.id === event.id && event.damage) this.hero.takeDamage(event.damage);
                continue;
            }
            const object = this.boxes.get(event.id);
            if (!object) continue;

            this.boxes.delete(event.id);
            if (object instanceof Barrel) this.barrels = this.barrels.filter((barrel) => barrel !== object);
            if (event.type === 'oilTankRuptured' && object instanceof OilTank) {
                object.createPuddle();
                object.destroy();
                continue;
            }
            object.destroy();
            if (event.type === 'thinWallDestroyed') continue;

            const radius = event.type === 'barrelExploded' ? 500 : 400;
            const explosion = this.add.sprite(event.x, event.y, 'explosion', 'explosion_10').setDepth(5);
            explosion.setScale((radius * 2) / 64);
            explosion.play('explosion_anim');
            explosion.once('animationcomplete', () => explosion.destroy());
            this.add
                .sprite(event.x, event.y, 'scorch')
                .setDisplaySize(radius * 1.4, radius * 1.4)
                .setDepth(-1);
            this.sound.play('barrel_explosion');
            this.cameras.main.shake(400, 0.008);
            this.flashManager.createExplosionFlash(event.x, event.y, radius);
        }
    }

    private reconcileLocalPlayer(delta: number) {
        const state = this.network.getLocalPlayer();
        if (!state) return;

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

    private updateRemotePlayers(delta: number) {
        const k = Math.min(1, delta / 120);
        const states = this.network.getRemotePlayers();
        for (const [id, state] of states) {
            let player = this.remotePlayers.get(id);
            if (!player) {
                player = this.add.sprite(state.x, state.y, 'hero', 'hero').setTint(0x66ccff).setAlpha(0.8).setDepth(1);
                player.setOrigin(0.5, 0.5);
                this.remotePlayers.set(id, player);
            }

            player.x = Phaser.Math.Linear(player.x, state.x, k);
            player.y = Phaser.Math.Linear(player.y, state.y, k);
            player.rotation = state.rotation;
            if (state.isDead && player.frame.name !== 'hero_dead') {
                player.setFrame('hero_dead');
            } else if (!state.isDead && player.frame.name === 'hero_dead') {
                player.setFrame('hero');
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
