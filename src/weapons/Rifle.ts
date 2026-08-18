import { BaseWeapon } from './BaseWeapon';
import { Projectile } from '../objects/Projectile';
import Phaser from 'phaser';

export class Rifle extends BaseWeapon {
    constructor(scene: Phaser.Scene) {
        super(scene, {
            name: 'Rifle',
            iconFrame: 'rifle', // Assuming 'rifle' is the frame in guns.atlas
            maxAmmo: 5,
            fireRate: 800, // Slow fire rate
            damage: 80, // 80% damage (e.g. 80 out of 100 HP)
            spread: 0, // High accuracy (no spread)
            pellets: 1,
            speed: 20,
            texture: 'projectiles',
            frame: 'bullet',
            sound: 'rifle_shot_sound'
        });
    }

    public fire(x: number, y: number, angle: number, time: number): void {
        if (!this.canFire(time)) return;

        this.currentAmmo--;
        this.lastFiredTime = time;

        new Projectile(this.scene, x, y, angle, this.stats.speed, this.stats.damage, this.stats.texture, this.stats.frame);

        this.scene.sound.play(this.stats.sound);
    }
}
