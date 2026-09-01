import Phaser from 'phaser';

export class Projectile {
    public gameObject: Phaser.Physics.Matter.Sprite;

    constructor(
        scene: Phaser.Scene,
        x: number,
        y: number,
        angle: number,
        speed: number,
        damage: number,
        texture: string,
        frame: string,
        piercing: boolean = false,
        isRemote: boolean = false
    ) {
        // Create the projectile
        this.gameObject = scene.matter.add.sprite(x, y, texture, frame);

        // Projectiles should be sensors (don't push objects, just detect collision)
        // Or if we want them to push things, we can give them low mass and a small shape.
        // For now, let's use a small circle body.
        this.gameObject.setBody({ type: 'circle', radius: 4 });
        this.gameObject.setFrictionAir(0);
        this.gameObject.setBounce(0);
        this.gameObject.setSensor(true);

        // Calculate velocity
        const vx = Math.cos(angle) * speed;
        const vy = Math.sin(angle) * speed;
        this.gameObject.setVelocity(vx, vy);
        this.gameObject.setRotation(angle);

        // Check for any bodies at the spawn location
        const allBodies = scene.matter.world.getAllBodies();
        const spawnBodies = allBodies.filter((body) => scene.matter.containsPoint(body, x, y));
        const ignoredBodies = spawnBodies
            .map((body) => (body as any).gameObject)
            .filter((obj) => obj !== undefined && obj !== null);

        // Custom data for collision handling
        this.gameObject.setData('isProjectile', true);
        this.gameObject.setData('damage', damage);
        this.gameObject.setData('ignoredBodies', ignoredBodies);
        this.gameObject.setData('isPiercing', piercing);
        this.gameObject.setData('isRemote', isRemote);

        if (!isRemote) {
            scene.events.emit('projectileFired', { x, y, angle, speed, damage, texture, frame, piercing });
        }

        // Destroy after a certain time to prevent memory leaks (e.g., 3 seconds)
        scene.time.delayedCall(3000, () => {
            if (this.gameObject && this.gameObject.active) {
                this.gameObject.destroy();
            }
        });

        // Add collision event listener for projectiles
        // We'll handle this generically or in a separate collision manager.
        // For simplicity, we can do it here on the scene's matter world.
        // It's better to set up a collision group or use scene.matter.world.on('collisionstart')
    }
}
