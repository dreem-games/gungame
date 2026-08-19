import { generateSeamlessNoiseTexture } from '../utils/NoiseGenerator';
import Phaser from 'phaser';

export class PreloadScene extends Phaser.Scene {
    constructor() {
        super('PreloadScene');
    }

    preload() {
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

        this.load.atlas('hero', 'assets/texture/hero.png', 'assets/texture/hero.json');
        this.load.atlas('level1', 'assets/texture/level1.png', 'assets/texture/level1.json');
        this.load.atlas('explosion', 'assets/texture/explosion.png', 'assets/texture/explosion.json');
        this.load.atlas('projectiles', 'assets/texture/projectiles.png', 'assets/texture/projectiles.json');

        this.load.atlas('guns', 'assets/ui/guns.png', 'assets/ui/guns.json');

        this.load.image('cursor', 'assets/texture/cursor.png');

        // Sounds
        this.load.audio('rifle_shot_sound', 'assets/sound/rifle_shot_sound.wav');
        this.load.audio('smg_shot_sound', 'assets/sound/smg_shot_sound.wav');
        this.load.audio('shotgun_shot_sound', 'assets/sound/shotgun_shot_sound.wav');
        this.load.audio('reload', 'assets/sound/reload.wav');
        this.load.audio('empty_gun_shot', 'assets/sound/empty_gun_shot.wav');
    }

    create() {
        // Generate noise texture once before starting the game
        generateSeamlessNoiseTexture(this, 'grass_noise', 512);

        this.scene.start('GameScene');
    }
}
