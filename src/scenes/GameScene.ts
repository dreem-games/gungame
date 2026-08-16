import Phaser from 'phaser';
import { Hero } from '../objects/Hero';
import { EntityManager } from '../core/EntityManager';
import { InputManager } from '../core/InputManager';

export class GameScene extends Phaser.Scene {
    private entityManager!: EntityManager;
    private inputManager!: InputManager;
    private hero!: Hero;

    constructor() {
        super('GameScene');
    }

    create() {
        // Initialize Core Systems
        this.entityManager = new EntityManager();
        this.inputManager = new InputManager(this);

        const WORLD_SIZE = 4096;
        const TILE_SIZE = 256;

        // Set world bounds
        this.matter.world.setBounds(0, 0, WORLD_SIZE, WORLD_SIZE);

        // Add ground texture
        const ground = this.add.tileSprite(WORLD_SIZE / 2, WORLD_SIZE / 2, WORLD_SIZE, WORLD_SIZE, 'level1', 'grass');
        ground.setDepth(-1);

        // Generate border walls
        this.generateBorderWalls(WORLD_SIZE, TILE_SIZE);

        // Test objects
        const box = this.matter.add.sprite(400, 300, 'level1', 'box');
        box.setRectangle(256, 256);
        box.setStatic(true);

        const barrel = this.matter.add.sprite(800, 500, 'level1', 'barrel');
        barrel.setCircle(128);
        barrel.setFrictionAir(0.1);
        barrel.setMass(50);

        // Create Hero in the center
        this.hero = new Hero(this, WORLD_SIZE / 2, WORLD_SIZE / 2, this.inputManager);
        this.entityManager.add(this.hero);

        // Camera setup
        this.cameras.main.startFollow(this.hero);
        this.cameras.main.setBounds(0, 0, WORLD_SIZE, WORLD_SIZE);
        // Make everything appear 2 times smaller (see 2x more space)
        this.cameras.main.setZoom(0.5);
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

    private createWall(x: number, y: number, size: number) {
        const wall = this.matter.add.sprite(x, y, 'level1', 'wall');
        wall.setRectangle(size, size);
        wall.setStatic(true);
    }

    update(time: number, delta: number) {
        this.inputManager.update();
        this.entityManager.update(time, delta);
    }
}