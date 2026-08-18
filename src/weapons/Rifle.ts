import { BaseWeapon } from './BaseWeapon';
import { Projectile } from '../objects/Projectile';
import Phaser from 'phaser';

export class Rifle extends BaseWeapon {
    constructor(scene: Phaser.Scene) {
        super(scene, {
            name: 'Rifle',
            iconFrame: 'rifle',
            maxAmmo: 5,
            fireRate: 1000,
            damage: 55,
            spread: 0.01,
            pellets: 1,
            speed: 40, // Base java speed 4 * 10
            texture: 'projectiles',
            frame: 'bullet',
            sound: 'rifle_shot_sound'
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
