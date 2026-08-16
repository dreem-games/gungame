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

        // Set world bounds
        this.matter.world.setBounds(0, 0, 2048, 2048);

        // Add ground texture
        const ground = this.add.tileSprite(1024, 1024, 2048, 2048, 'level1', 'grass');
        ground.setDepth(-1);

        // Test objects
        const box = this.matter.add.sprite(400, 300, 'level1', 'box');
        box.setRectangle(256, 256);
        box.setStatic(true);

        const barrel = this.matter.add.sprite(800, 500, 'level1', 'barrel');
        barrel.setCircle(128);
        barrel.setFrictionAir(0.1);
        barrel.setMass(50);

        // Create Hero
        this.hero = new Hero(this, 1024, 1024, this.inputManager);
        this.entityManager.add(this.hero);

        // Camera setup
        this.cameras.main.startFollow(this.hero);
        this.cameras.main.setBounds(0, 0, 2048, 2048);
    }

    update(time: number, delta: number) {
        this.inputManager.update();
        this.entityManager.update(time, delta);
    }
}