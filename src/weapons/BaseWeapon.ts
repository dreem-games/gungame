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
    piercing?: boolean; // Whether the bullet can pierce through thin walls
    texture: string;
    frame: string;
    sound: string;
}

export abstract class BaseWeapon {
    protected scene: Phaser.Scene;
    public stats: WeaponStats;
    public currentAmmo: number;
    protected lastFiredTime: number = 0;

    // Reload state
    public isReloading: boolean = false;
    private reloadTimer: Phaser.Time.TimerEvent | null = null;
    private reloadDuration: number = 2700; // 2.7 seconds

    constructor(scene: Phaser.Scene, stats: WeaponStats) {
        this.scene = scene;
        this.stats = stats;
        this.currentAmmo = stats.maxAmmo;
    }

    public canFire(time: number): boolean {
        return !this.isReloading && time > this.lastFiredTime + this.stats.fireRate && this.currentAmmo > 0;
    }

    public abstract fire(x: number, y: number, angle: number, time: number): void;

    public reload() {
        if (!this.isReloading && this.currentAmmo < this.stats.maxAmmo) {
            this.isReloading = true;
            this.scene.sound.play('reload');

            this.reloadTimer = this.scene.time.delayedCall(this.reloadDuration, () => {
                this.currentAmmo = this.stats.maxAmmo;
                this.isReloading = false;
                this.scene.game.events.emit('ammoChanged', this.currentAmmo, this.stats.maxAmmo);
            });
        }
    }

    public cancelReload() {
        if (this.isReloading) {
            this.isReloading = false;
            if (this.reloadTimer) {
                this.reloadTimer.remove();
            }
            this.scene.sound.removeByKey('reload');
        }
    }
}
