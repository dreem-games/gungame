import { BaseWeapon } from './BaseWeapon';
import { Projectile } from '../objects/Projectile';
import Phaser from 'phaser';

export class AssaultRifle extends BaseWeapon {
    constructor(scene: Phaser.Scene) {
        super(scene, {
            name: 'Assault Rifle',
            iconFrame: 'smg',
            maxAmmo: 24, // Matches Java
            fireRate: 100,
            damage: 10,
            spread: 0.035,
            pellets: 1,
            speed: 30,
            texture: 'projectiles',
            frame: 'bullet',
            sound: 'smg_shot_sound'
        });
    }

    public fire(x: number, y: number, angle: number, time: number): void {
        if (!this.canFire(time)) return;

        this.currentAmmo--;
        this.lastFiredTime = time;

        const gaussian = (Math.random() + Math.random() + Math.random() - 1.5) * 2;
        const spreadAngle = angle + (gaussian * this.stats.spread);

        new Projectile(this.scene, x, y, spreadAngle, this.stats.speed, this.stats.damage, this.stats.texture, this.stats.frame, this.stats.piercing);

        this.scene.sound.play(this.stats.sound);

        // Light shake for Assault Rifle
        this.scene.cameras.main.shake(50, 0.002);
        if ((this.scene as any).flashManager) {
            (this.scene as any).flashManager.createShotFlash(x, y, angle, 200);
        }
    }
}
