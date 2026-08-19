import Phaser from 'phaser';
import { Hero } from '../objects/Hero';
import { EntityManager } from '../core/EntityManager';
import { InputManager } from '../core/InputManager';
import { Barrel } from '../objects/Barrel';
import { OilTank } from '../objects/OilTank';
import { ThinWall } from '../objects/ThinWall';

export class GameScene extends Phaser.Scene {
    private entityManager!: EntityManager;
    private inputManager!: InputManager;
    private hero!: Hero;
    private barrels: Barrel[] = [];

    constructor() {
        super('GameScene');
    }

    create() {
        // Initialize Core Systems
        this.entityManager = new EntityManager();
        this.inputManager = new InputManager(this);

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
                }

                if (isProjB) {
                    const ignoredBodies: Phaser.GameObjects.GameObject[] = gameObjectB.getData('ignoredBodies') || [];
                    if (ignoredBodies.includes(gameObjectA)) {
                        return; // Ignore this collision completely
                    }
                }

                this.handleProjectileCollision(gameObjectA, gameObjectB);
            });
        });

        const WORLD_SIZE = 6144;
        const TILE_SIZE = 128; // Wall tile size reduced by half

        // Set world bounds
        this.matter.world.setBounds(0, 0, WORLD_SIZE, WORLD_SIZE);

        // Add ground texture (also visually scale down the grass tile)
        const ground = this.add.tileSprite(WORLD_SIZE / 2, WORLD_SIZE / 2, WORLD_SIZE * 2, WORLD_SIZE * 2, 'level1', 'grass');
        ground.setDepth(-2);
        ground.setScale(0.5);

        // Add procedural noise overlay on top of grass
        const noiseScale = 4.0; // scale up the texture size to cover more area
        const noise = this.add.tileSprite(WORLD_SIZE / 2, WORLD_SIZE / 2, WORLD_SIZE / noiseScale, WORLD_SIZE / noiseScale, 'grass_noise');
        noise.setBlendMode(Phaser.BlendModes.MULTIPLY); // MULTIPLY makes dark areas darker, light areas transparent
        noise.setAlpha(0.7); // Adjust intensity of the noise
        noise.setDepth(-1);
        noise.setScale(noiseScale);

        // Generate border walls
        this.generateBorderWalls(WORLD_SIZE, TILE_SIZE);

// Generate environment objects (crates, barrels)
        this.generateEnvironmentObjects(WORLD_SIZE, TILE_SIZE);

        // Create Hero in the center
        this.hero = new Hero(this, WORLD_SIZE / 2, WORLD_SIZE / 2, this.inputManager);
        this.entityManager.add(this.hero);

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

    private handleProjectileCollision(gameObjectA: Phaser.GameObjects.GameObject, gameObjectB: Phaser.GameObjects.GameObject) {
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
            if (isThinWall && isPiercing) {
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

    private generateEnvironmentObjects(worldSize: number, tileSize: number) {
        const CRATE_COUNT = 75;
        const BARREL_COUNT = 16;
        const SPAWN_SAFE_RADIUS = 300; // Distance from center
        const CENTER_X = worldSize / 2;
        const CENTER_Y = worldSize / 2;
        const MARGIN = tileSize; // Keep away from walls

        // Helper to check distance
        const dist = (x1: number, y1: number, x2: number, y2: number) => Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));

        // We will keep track of placed objects to avoid overlaps
        // Each object needs an x, y, and a collision radius
        const placedObjects: {x: number, y: number, radius: number, isBox?: boolean}[] = [];

        // Try to place an object
        const placeObject = (type: 'box' | 'barrel' | 'oilTank' | 'thinWall', collisionRadius: number) => {
            let attempts = 0;
            const MAX_ATTEMPTS = 50;

            while (attempts < MAX_ATTEMPTS) {
                let x = Phaser.Math.Between(MARGIN, worldSize - MARGIN);
                let y = Phaser.Math.Between(MARGIN, worldSize - MARGIN);

                // 30% chance for a box to cluster near another box
                if (type === 'box' && Math.random() < 0.3) {
                    const boxes = placedObjects.filter(o => o.isBox);
                    if (boxes.length > 0) {
                        const targetBox = boxes[Phaser.Math.Between(0, boxes.length - 1)];
                        // Try to place near this box (within roughly 2-3 box widths)
                        x = targetBox.x + Phaser.Math.Between(-300, 300);
                        y = targetBox.y + Phaser.Math.Between(-300, 300);
                        // Clamp to world
                        x = Phaser.Math.Clamp(x, MARGIN, worldSize - MARGIN);
                        y = Phaser.Math.Clamp(y, MARGIN, worldSize - MARGIN);
                    }
                }

                // Check distance to center (hero spawn)
                if (dist(x, y, CENTER_X, CENTER_Y) < SPAWN_SAFE_RADIUS + collisionRadius) {
                    attempts++;
                    continue;
                }

                // Check collisions with already placed objects
                let hasCollision = false;
                for (const obj of placedObjects) {
                    if (dist(x, y, obj.x, obj.y) < collisionRadius + obj.radius) {
                        hasCollision = true;
                        break;
                    }
                }

                if (!hasCollision) {
                    // Place it!
                    if (type === 'box') {
                        const box = this.matter.add.sprite(x, y, 'level1', 'box');
                        box.setBody({ type: 'rectangle', width: 256, height: 256 });
                        box.setScale(0.5);
                        box.setFrictionAir(0.1);
                        box.setMass(70);
                        placedObjects.push({ x, y, radius: collisionRadius, isBox: true });
                    } else if (type === 'barrel') {
                        const barrel = new Barrel(this, x, y);
                        this.barrels.push(barrel);
                        placedObjects.push({ x, y, radius: collisionRadius });
                    } else if (type === 'oilTank') {
                        new OilTank(this, x, y);
                        placedObjects.push({ x, y, radius: collisionRadius });
                    } else if (type === 'thinWall') {
                        // Place a thin wall of roughly length 256
                        const isVertical = Math.random() > 0.5;
                        new ThinWall(this, x - (isVertical ? 0 : 128), y - (isVertical ? 128 : 0), 256, isVertical);
                        placedObjects.push({ x, y, radius: collisionRadius });
                    }

                    return true;
                }

                attempts++;
            }

            return false; // Failed to place after MAX_ATTEMPTS
        };

        // Box visual size is 128x128, radius roughly 90 for spacing
        for (let i = 0; i < CRATE_COUNT; i++) {
            placeObject('box', 90);
        }

        // Barrel visual size is 128x128, radius 64
        for (let i = 0; i < BARREL_COUNT; i++) {
            placeObject('barrel', 64);
        }

        // Place one Oil Tank (radius around 102)
        placeObject('oilTank', 102);

        // Place one Thin Wall (treat as a circle of radius 140 for spawning space)
        placeObject('thinWall', 140);
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
    }

    update(time: number, delta: number) {
        this.entityManager.update(time, delta);
        this.inputManager.update();
    }
}