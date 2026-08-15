import Phaser from 'phaser';

export class BootScene extends Phaser.Scene {
    constructor() {
        super('BootScene');
    }

    preload() {
        // Load very first assets here if needed
    }

    create() {
        this.scene.start('PreloadScene');
    }
}