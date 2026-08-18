import { BaseWeapon } from './BaseWeapon';
import { Projectile } from '../objects/Projectile';
import Phaser from 'phaser';

export class AssaultRifle extends BaseWeapon {
    constructor(scene: Phaser.Scene) {
        super(scene, {
            name: 'Assault Rifle',
            iconFrame: 'smg', // Assuming 'smg' is the frame in guns.atlas
            maxAmmo: 30,
            fireRate: 100, // High fire rate
            damage: 25, // 25% damage
            spread: Phaser.Math.DegToRad(5), // Medium spread (5 degrees)
            pellets: 1,
            speed: 20,
            texture: 'projectiles',
            frame: 'bullet',
            sound: 'smg_shot_sound'
        });
    }

    public fire(x: number, y: number, angle: number, time: number): void {
        if (!this.canFire(time)) return;

        this.currentAmmo--;
        this.lastFiredTime = time;

        const spreadAngle = angle + Phaser.Math.FloatBetween(-this.stats.spread, this.stats.spread);

        new Projectile(this.scene, x, y, spreadAngle, this.stats.speed, this.stats.damage, this.stats.texture, this.stats.frame);

        this.scene.sound.play(this.stats.sound);
    }
}
