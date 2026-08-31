import Phaser from 'phaser';

import { EventDispatcher } from '../core/EventBus';
import { BaseWeapon } from '../weapons/BaseWeapon';

export class UIScene extends Phaser.Scene {
    private ammoText!: Phaser.GameObjects.Text;
    private weaponIcon!: Phaser.GameObjects.Sprite;
    private bloodOverlay!: Phaser.GameObjects.Graphics;

    constructor() {
        super({ key: 'UIScene' });
    }

    create() {
        // UI Layout in top-left
        const padding = 20;

        // Weapon Icon Background
        this.add
            .rectangle(padding + 40, padding + 40, 80, 80, 0x000000, 0.8)
            .setStrokeStyle(2, 0xffffff, 0.8)
            .setScrollFactor(0);

        // Add the blood overlay behind the weapon
        this.bloodOverlay = this.add.graphics();
        this.bloodOverlay.setScrollFactor(0);

        // Weapon Icon Sprite (scaled to fit)
        this.weaponIcon = this.add.sprite(padding + 40, padding + 40, 'guns', 'rifle');
        this.weaponIcon.setScrollFactor(0);

        // Scale logic depends on the specific weapon sprite aspect ratio,
        // let's adjust it in `onWeaponChanged` to keep it proportional,
        // but set a default scale first.
        this.weaponIcon.setScale(0.15);

        // Ammo Text
        this.ammoText = this.add.text(padding + 90, padding + 25, '5 / ∞', {
            fontFamily: 'Orbitron, Arial',
            fontSize: '28px',
            color: '#ffffff',
            stroke: '#000000',
            strokeThickness: 4
        });
        this.ammoText.setScrollFactor(0);

        // Listen for events from GameScene via the global game EventBus
        this.game.events.on('weaponChanged', this.onWeaponChanged, this);
        this.game.events.on('ammoChanged', this.onAmmoChanged, this);

        EventDispatcher.on('hero-damage', this.onHeroDamage, this);
    }

    private onWeaponChanged(weapon: BaseWeapon) {
        this.weaponIcon.setFrame(weapon.stats.iconFrame);

        // Adjust scale dynamically so it fits nicely inside the 80x80 box
        if (weapon.stats.iconFrame === 'rifle' || weapon.stats.iconFrame === 'shotgun') {
            this.weaponIcon.setScale(0.08); // These sprites are very long (~900px wide)
        } else if (weapon.stats.iconFrame === 'smg') {
            this.weaponIcon.setScale(0.12); // Slightly shorter (~600px wide)
        }

        // Let's ensure the sprite uses its actual colors but maybe make it brighter
        this.weaponIcon.clearTint();

        this.ammoText.setText(`${weapon.currentAmmo} / ∞`);
    }

    private onAmmoChanged(currentAmmo: number, _maxAmmo: number) {
        this.ammoText.setText(`${currentAmmo} / ∞`);
    }

    private onHeroDamage(currentHp: number) {
        const maxHp = 100;
        const damagePercent = 1 - currentHp / maxHp;

        this.bloodOverlay.clear();

        if (damagePercent > 0) {
            // Draw a red rectangle over the background, with opacity scaling by damage taken
            this.bloodOverlay.fillStyle(0xff0000, damagePercent * 0.8);
            this.bloodOverlay.fillRect(20 + 40 - 40, 20 + 40 - 40, 80, 80); // padding is 20, rect is at 60,60 center with 80x80 size. Top left is 20, 20.
        }
    }

    destroy() {
        this.game.events.off('weaponChanged', this.onWeaponChanged, this);
        this.game.events.off('ammoChanged', this.onAmmoChanged, this);
        EventDispatcher.off('hero-damage', this.onHeroDamage, this);
    }
}
