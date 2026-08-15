import Phaser from 'phaser';

export class PreloadScene extends Phaser.Scene {
    constructor() {
        super('PreloadScene');
    }

    preload() {
        // Display loading text
        const width = this.cameras.main.width;
        const height = this.cameras.main.height;
        const loadingText = this.make.text({
            x: width / 2,
            y: height / 2,
            text: 'Loading...',
            style: {
                font: '20px monospace',
                color: '#ffffff'
            }
        });
        loadingText.setOrigin(0.5, 0.5);

        // Load atlases
        // The paths are relative to the public/ directory since we copy assets there
        this.load.atlas('hero', 'assets/texture/hero.png', 'assets/texture/hero.json');
        this.load.atlas('level1', 'assets/texture/level1.png', 'assets/texture/level1.json');
        this.load.atlas('explosion', 'assets/texture/explosion.png', 'assets/texture/explosion.json');
        this.load.atlas('projectiles', 'assets/texture/projectiles.png', 'assets/texture/projectiles.json');

        // Load independent textures
        this.load.image('cursor', 'assets/texture/cursor.png');

        // The libGDX .atlas files are not directly compatible with Phaser's json format
        // We will need to either convert them to JSON (TexturePacker format) or parse libGDX atlas format.
        // Phaser has a built-in loader for LibGDX atlases! We should use load.multiatlas if it supports it,
        // but load.atlas can load libgdx format if we use a special loader or specify the format.
        // Let's check if the files are standard json or libgdx text format.
    }

    create() {
        this.scene.start('GameScene');
    }
}