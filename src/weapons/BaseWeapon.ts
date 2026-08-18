import Phaser from 'phaser';

export interface WeaponStats {
    name: string;
    iconFrame: string;
    maxAmmo: number;
    fireRate: number; // in milliseconds
    damage: number;
    spread: number; // in radians
    pellets: number; // For shotgun
    speed: number;
    texture: string;
    frame: string;
    sound: string;
}

export abstract class BaseWeapon {
    protected scene: Phaser.Scene;
    public stats: WeaponStats;
    public currentAmmo: number;
    protected lastFiredTime: number = 0;

    constructor(scene: Phaser.Scene, stats: WeaponStats) {
        this.scene = scene;
        this.stats = stats;
        this.currentAmmo = stats.maxAmmo;
    }

    public canFire(time: number): boolean {
        return time > this.lastFiredTime + this.stats.fireRate && this.currentAmmo > 0;
    }

    public abstract fire(x: number, y: number, angle: number, time: number): void;

    public reload() {
        if (this.currentAmmo < this.stats.maxAmmo) {
            this.currentAmmo = this.stats.maxAmmo;
            this.scene.sound.play('reload');
        }
    }
}
