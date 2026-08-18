import { BaseWeapon } from './BaseWeapon';
import { Projectile } from '../objects/Projectile';
import Phaser from 'phaser';

export class Shotgun extends BaseWeapon {
    constructor(scene: Phaser.Scene) {
        super(scene, {
            name: 'Shotgun',
            iconFrame: 'shotgun', // Assuming 'shotgun' is the frame in guns.atlas
            maxAmmo: 1, // 1 round per clip
            fireRate: 1000,
            damage: 20, // Damage per pellet
            spread: Phaser.Math.DegToRad(20), // 20 degrees total spread
            pellets: 8, // Multiple pellets
            speed: 15,
            texture: 'projectiles',
            frame: 'bullet',
            sound: 'shotgun_shot_sound'
        });
    }

    public fire(x: number, y: number, angle: number, time: number): void {
        if (!this.canFire(time)) return;

        this.currentAmmo--;
        this.lastFiredTime = time;

        const halfSpread = this.stats.spread / 2;

        for (let i = 0; i < this.stats.pellets; i++) {
            const spreadAngle = angle + Phaser.Math.FloatBetween(-halfSpread, halfSpread);
            // Add some speed variance to pellets
            const speedVariance = this.stats.speed * Phaser.Math.FloatBetween(0.8, 1.2);
            new Projectile(this.scene, x, y, spreadAngle, speedVariance, this.stats.damage, this.stats.texture, this.stats.frame);
        }

        this.scene.sound.play(this.stats.sound);
    }
}
