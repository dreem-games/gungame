import Phaser from 'phaser';
import { IEntity } from '../types/interfaces';
import { Hero } from './Hero';

export class Barrel extends Phaser.Physics.Matter.Sprite implements IEntity {
    public id: string;
    public gameObject: Phaser.GameObjects.GameObject;
    public isDestroyed: boolean = false;
    private exploded: boolean = false;

    // Damage settings
    private maxDamage: number = 100;
    private explosionRadius: number = 500;

    constructor(scene: Phaser.Scene, x: number, y: number) {
        super(scene.matter.world, x, y, 'level1', 'barrel');

        this.id = Phaser.Math.RND.uuid();
        this.gameObject = this;
        scene.add.existing(this);

        this.setBody({ type: 'circle', radius: 128 });
        this.setScale(0.5);
        this.setFrictionAir(0.1);
        this.setMass(50);
        this.setDepth(1);
    }

    public explode(hero: Hero, allBarrels: Barrel[]) {
        if (this.exploded) return;
        this.exploded = true;

        // Play visual and sound
        const explosionSprite = this.scene.add.sprite(this.x, this.y, 'explosion', 'explosion_10');
        explosionSprite.setScale(3); // make explosion bigger
        explosionSprite.setDepth(5);
        explosionSprite.play('explosion_anim');

        explosionSprite.once('animationcomplete', () => {
            explosionSprite.destroy();
        });

        this.scene.sound.play('barrel_explosion');

        // Strong shake — explosion is the heaviest impact in the game
        this.scene.cameras.main.shake(400, 0.008);

        // Apply damage to hero based on distance
        const distToHero = Phaser.Math.Distance.Between(this.x, this.y, hero.x, hero.y);
        if (distToHero <= this.explosionRadius) {
            // Quadratic falloff: damage stays high at range so a distant blast can still kill
            const falloff = distToHero / this.explosionRadius;
            const damagePercentage = 1 - falloff * falloff;
            const damage = Math.round(this.maxDamage * damagePercentage);
            if (damage > 0) {
                hero.takeDamage(damage);
            }
        }

        // Trigger chain reaction on nearby barrels
        for (const barrel of allBarrels) {
            if (barrel === this || barrel.exploded || barrel.isDestroyed) continue;

            const distToBarrel = Phaser.Math.Distance.Between(this.x, this.y, barrel.x, barrel.y);
            if (distToBarrel <= this.explosionRadius) {
                // Instantly blow up other barrels in radius
                // Use a tiny delay to make the chain reaction look better
                this.scene.time.delayedCall(100, () => {
                    barrel.explode(hero, allBarrels);
                });
            }
        }

        this.destroy();
    }

    update(_time: number, _delta: number): void {
        // Barrels are mostly static, nothing to update actively
    }

    destroy(fromScene?: boolean) {
        if (this.isDestroyed) return;
        this.isDestroyed = true;
        super.destroy(fromScene);
    }
}
