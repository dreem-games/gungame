import Phaser from 'phaser';
import { Hero } from '../objects/Hero';

export class GameScene extends Phaser.Scene {
    private hero!: Hero;

    constructor() {
        super('GameScene');
    }

    create() {
        // Set world bounds
        this.matter.world.setBounds(0, 0, 2048, 2048);

        // Add some ground texture (tiled)
        const ground = this.add.tileSprite(1024, 1024, 2048, 2048, 'level1', 'grass');
        ground.setDepth(-1); // send to back

        // Add some test objects from level1 atlas
        const box = this.matter.add.sprite(400, 300, 'level1', 'box');
        box.setRectangle(256, 256);
        box.setStatic(true);

        const barrel = this.matter.add.sprite(800, 500, 'level1', 'barrel');
        barrel.setCircle(128);
        barrel.setFrictionAir(0.1);
        barrel.setMass(50);

        // Add hero
        this.hero = new Hero(this, 1024, 1024);

        // Make camera follow hero
        this.cameras.main.startFollow(this.hero);
        this.cameras.main.setBounds(0, 0, 2048, 2048);
    }

    update() {
        this.hero.update();
    }
}