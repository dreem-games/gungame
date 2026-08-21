import { BaseWeapon } from './BaseWeapon';
import { Rifle } from './Rifle';
import { AssaultRifle } from './AssaultRifle';
import { Shotgun } from './Shotgun';
import Phaser from 'phaser';

export class WeaponManager {
    private scene: Phaser.Scene;
    public weapons: BaseWeapon[];
    public currentWeaponIndex: number = 0;

    constructor(scene: Phaser.Scene) {
        this.scene = scene;
        this.weapons = [
            new Rifle(scene),
            new AssaultRifle(scene),
            new Shotgun(scene)
        ];
    }

    public getCurrentWeapon(): BaseWeapon {
        return this.weapons[this.currentWeaponIndex];
    }

    public switchWeapon(index: number) {
        const currentWeapon = this.getCurrentWeapon();
        if (currentWeapon && currentWeapon.isReloading) {
            // Cannot switch while reloading
            return;
        }

        if (index >= 0 && index < this.weapons.length && index !== this.currentWeaponIndex) {
            this.currentWeaponIndex = index;
            // Emit event so UI can update
            this.scene.game.events.emit('weaponChanged', this.getCurrentWeapon());
        }
    }

    public switchNext() {
        let nextIndex = this.currentWeaponIndex + 1;
        if (nextIndex >= this.weapons.length) nextIndex = 0;
        this.switchWeapon(nextIndex);
    }

    public switchPrev() {
        let prevIndex = this.currentWeaponIndex - 1;
        if (prevIndex < 0) prevIndex = this.weapons.length - 1;
        this.switchWeapon(prevIndex);
    }

    public fire(x: number, y: number, angle: number, time: number) {
        const weapon = this.getCurrentWeapon();

        if (weapon.isReloading) {
            return null; // Ignore firing while reloading
        }

        if (weapon.currentAmmo <= 0) {
            // Initiate auto-reload on empty if allowed, otherwise just return empty status
            return false; // indicating out of ammo
        }

        if (weapon.canFire(time)) {
            weapon.fire(x, y, angle, time);
            this.scene.game.events.emit('ammoChanged', weapon.currentAmmo, weapon.stats.maxAmmo);

            // Auto-reload
            if (weapon.currentAmmo === 0) {
                 weapon.reload();
            }
            return true;
        }
        return null; // indicating on cooldown
    }

    public reload() {
        const weapon = this.getCurrentWeapon();
        weapon.reload();
    }

    public cancelReload() {
        this.getCurrentWeapon().cancelReload();
    }
}
