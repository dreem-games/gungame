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
        if (weapon.currentAmmo <= 0) {
            // Play empty sound if trying to fire empty gun
            // We use justPressed to not spam the sound on auto weapons
            // We will let the Hero handle the "just pressed" check before calling fire,
            // or handle it here if we pass a boolean. Let's just do it in Hero.
            return false; // indicating out of ammo
        }

        if (weapon.canFire(time)) {
            weapon.fire(x, y, angle, time);
            this.scene.game.events.emit('ammoChanged', weapon.currentAmmo, weapon.stats.maxAmmo);
            return true;
        }
        return null; // indicating on cooldown
    }

    public reload() {
        const weapon = this.getCurrentWeapon();
        if (weapon.currentAmmo < weapon.stats.maxAmmo) {
            weapon.reload();
            this.scene.game.events.emit('ammoChanged', weapon.currentAmmo, weapon.stats.maxAmmo);
        }
    }
}
