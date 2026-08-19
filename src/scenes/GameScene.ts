import Phaser from 'phaser';
import { Hero } from '../objects/Hero';
import { EntityManager } from '../core/EntityManager';
import { InputManager } from '../core/InputManager';
import { Barrel } from '../objects/Barrel';

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

        // Setup Collision events
        this.matter.world.on('collisionstart', (event: Phaser.Physics.Matter.Events.CollisionStartEvent) => {
            event.pairs.forEach((pair) => {
                const bodyA = pair.bodyA as MatterJS.BodyType;
                const bodyB = pair.bodyB as MatterJS.BodyType;

                const gameObjectA = bodyA.gameObject as Phaser.GameObjects.GameObject;
                const gameObjectB = bodyB.gameObject as Phaser.GameObjects.GameObject;

                if (!gameObjectA || !gameObjectB) return;

                this.handleProjectileCollision(gameObjectA, gameObjectB);
            });
        });

        const WORLD_SIZE = 4096;
        const TILE_SIZE = 128; // Wall tile size reduced by half

        // Set world bounds
        this.matter.world.setBounds(0, 0, WORLD_SIZE, WORLD_SIZE);

        // Add ground texture (also visually scale down the grass tile)
        const ground = this.add.tileSprite(WORLD_SIZE / 2, WORLD_SIZE / 2, WORLD_SIZE * 2, WORLD_SIZE * 2, 'level1', 'grass');
        ground.setDepth(-1);
        ground.setScale(0.5);

        // Generate border walls
        this.generateBorderWalls(WORLD_SIZE, TILE_SIZE);

        // Test objects (Scaled down by half)
        // We use setBody to set custom physics bounds.
        // Important: Phaser's setBody automatically scales by the sprite's scale.
        // A scale of 0.5 makes the sprite visually 128x128 (from 256x256).
        // So we pass unscaled values to setBody.
        const box = this.matter.add.sprite(400, 300, 'level1', 'box');
        // Unscaled Box physics body 256x256 (scaled down it will perfectly match the 128x128 sprite)
        box.setBody({ type: 'rectangle', width: 256, height: 256 });
        box.setScale(0.5);
        box.setFrictionAir(0.1);
        box.setMass(70);

        // Spawn barrels
        const barrel1 = new Barrel(this, 800, 500);
        const barrel2 = new Barrel(this, 900, 500);
        const barrel3 = new Barrel(this, 850, 400);
        this.barrels.push(barrel1, barrel2, barrel3);
        this.entityManager.add(barrel1);
        this.entityManager.add(barrel2);
        this.entityManager.add(barrel3);

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

    private processHit(projectile: Phaser.GameObjects.GameObject, _target: Phaser.GameObjects.GameObject) {
        // Destroy the projectile
        projectile.destroy();

        // Check if target is a barrel
        if (_target instanceof Barrel) {
            _target.explode(this.hero, this.barrels);
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
    }

    update(time: number, delta: number) {
        this.entityManager.update(time, delta);
        this.inputManager.update();
    }
}