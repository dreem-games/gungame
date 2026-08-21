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
        this.setData('blocksVision', true);
    }

    public explode(hero: Hero, allBarrels: Barrel[]) {
        if (this.exploded) return;
        this.exploded = true;

        // Play visual and sound
        const explosionSprite = this.scene.add.sprite(this.x, this.y, 'explosion', 'explosion_10');
        // Масштаб под радиус урона, чтобы визуально было видно, куда бьёт взрыв
        const FRAME_SIZE = 64; // базовый размер фрейма анимации (px)
        explosionSprite.setScale((this.explosionRadius * 2) / FRAME_SIZE);
        explosionSprite.setDepth(5);
        explosionSprite.play('explosion_anim');

        explosionSprite.once('animationcomplete', () => {
            explosionSprite.destroy();
        });

        this.scene.sound.play('barrel_explosion');

        // Тёмный след (scorch mark) на месте взрыва — мягкий radial gradient
        const SCORCH_RADIUS = this.explosionRadius * 0.7;
        const scorch = this.scene.add.sprite(this.x, this.y, 'scorch');
        scorch.setDisplaySize(SCORCH_RADIUS * 2, SCORCH_RADIUS * 2);
        scorch.setDepth(-1); // поверх травы, под объектами

        // Strong shake — explosion is the heaviest impact in the game
        this.scene.cameras.main.shake(400, 0.008);
        if ((this.scene as any).flashManager) {
            (this.scene as any).flashManager.createExplosionFlash(this.x, this.y, 500);
        }

        // Shockwave: отбрасывает ящики и героя. Импульс скорости (а не сила)
        // — мгновенный и предсказуемый. Бочки не отбрасываем — они уничтожаются
        // при попадании, а не разлетаются.
        const KNOCKBACK_RADIUS = 700;
        const KNOCKBACK_SPEED = 40;
        const allBodies = this.scene.matter.world.getAllBodies();
        for (const body of allBodies) {
            if (body === this.body || body.isStatic) continue;
            if (body.isSensor) continue; // пули (сенсоры) не отбрасываем
            if ((body as any).parent instanceof Barrel) continue; // бочки уничтожаются, не разлетаются

            const dx = body.position.x - this.x;
            const dy = body.position.y - this.y;
            const dist = Math.sqrt(dx * dx + dy * dy);
            if (dist === 0 || dist > KNOCKBACK_RADIUS) continue;

            // Чем ближе, тем сильнее; на краю радиуса почти ноль
            const falloff = 1 - dist / KNOCKBACK_RADIUS;
            const speed = KNOCKBACK_SPEED * falloff;
            const vel = (body as any).velocity || { x: 0, y: 0 };
            this.scene.matter.body.setVelocity(body as MatterJS.BodyType, {
                x: vel.x + (dx / dist) * speed,
                y: vel.y + (dy / dist) * speed,
            });
        }

        // Apply damage to hero based on distance
        const distToHero = Phaser.Math.Distance.Between(this.x, this.y, hero.x, hero.y);
        if (distToHero <= this.explosionRadius) {
            // Quadratic falloff: damage stays high at range so a distant blast can still kill
            const falloff = distToHero / this.explosionRadius;
            const damagePercentage = 1 - falloff * falloff;
            const damage = Math.round(this.maxDamage * damagePercentage);
            if (damage > 0) {
                hero.takeDamage(damage);
                hero.setKnockback(500);
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
