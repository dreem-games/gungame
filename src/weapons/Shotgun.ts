import { BaseWeapon } from './BaseWeapon';
import { Projectile } from '../objects/Projectile';
import Phaser from 'phaser';

export class Shotgun extends BaseWeapon {
    constructor(scene: Phaser.Scene) {
        super(scene, {
            name: 'Shotgun',
            iconFrame: 'shotgun',
            maxAmmo: 1,
            fireRate: 300,
            damage: 5,
            spread: 0.1,
            pellets: 32,
            speed: 15, // Base java speed 1.5 * 10
            texture: 'projectiles',
            frame: 'bullet',
            sound: 'shotgun_shot_sound'
        });
    }

    public fire(x: number, y: number, angle: number, time: number): void {
        if (!this.canFire(time)) return;

        this.currentAmmo--;
        this.lastFiredTime = time;

        for (let i = 0; i < this.stats.pellets; i++) {
            // Java used nextGaussian() * spread, so let's approximate
            // Math.random() + Math.random() - 1 gives a cheap pseudo-gaussian
            const gaussian = (Math.random() + Math.random() + Math.random() - 1.5) * 2;
            const spreadAngle = angle + (gaussian * this.stats.spread);

            // Add some speed variance to pellets
            const speedVariance = this.stats.speed * Phaser.Math.FloatBetween(0.8, 1.2);
            new Projectile(this.scene, x, y, spreadAngle, speedVariance, this.stats.damage, this.stats.texture, this.stats.frame, this.stats.piercing);
        }

        this.scene.sound.play(this.stats.sound);

        // Heavy shake for Shotgun
        this.scene.cameras.main.shake(150, 0.01);
    }
}
