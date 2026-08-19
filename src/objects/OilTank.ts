import Phaser from 'phaser';

export class OilTank extends Phaser.Physics.Matter.Sprite {
    private tankHealth: number = 100;
    private isTankDestroyed: boolean = false;

    constructor(scene: Phaser.Scene, x: number, y: number) {
        // We'll reuse the barrel texture, but tint it dark or change scale to make it look like a big tank
        super(scene.matter.world, x, y, 'level1', 'barrel');
        this.scene.add.existing(this as any);

        this.setBody({ type: 'circle', radius: 128 });
        this.setScale(0.8); // Bigger than a standard barrel
        this.setTint(0x444444); // Dark tint to look like an oil tank
        this.setFrictionAir(0.1);
        this.setMass(200);

        this.setData('isOilTank', true);
    }

    public takeDamage(amount: number) {
        if (this.isTankDestroyed) return;
        this.tankHealth -= amount;

        // Flash red
        this.setTint(0xff0000);
        this.scene.time.delayedCall(100, () => {
            if (!this.isTankDestroyed) this.setTint(0x444444);
        });

        if (this.tankHealth <= 0) {
            this.explode();
        }
    }

    private explode() {
        this.isTankDestroyed = true;

        // Create the puddle
        this.createPuddle();

        // Push away nearby objects (shockwave)
        const allBodies = this.scene.matter.world.getAllBodies();
        const shockwaveRadius = 500;
        const forceMagniture = 0.5;

        for (const body of allBodies) {
            if (body === this.body) continue;

            const dx = body.position.x - this.x;
            const dy = body.position.y - this.y;
            const dist = Math.sqrt(dx * dx + dy * dy);

            if (dist < shockwaveRadius && !body.isStatic) {
                // Normalize and apply force
                const forceX = (dx / dist) * forceMagniture;
                const forceY = (dy / dist) * forceMagniture;
                this.scene.matter.body.applyForce(body as MatterJS.BodyType, {x: this.x, y: this.y}, {x: forceX, y: forceY});
            }
        }

        // Destroy the tank itself
        this.destroy();
    }

    private createPuddle() {
        const PUDDLE_RADIUS = 600; // Large puddle

        // Procedurally draw a splat
        const graphics = this.scene.add.graphics();
        graphics.fillStyle(0x1a2b1a, 0.8); // Dark greenish-black oil color

        // Draw main circle
        graphics.fillCircle(this.x, this.y, PUDDLE_RADIUS * 0.7);

        // Draw some smaller intersecting circles around the edges for a "splat" look
        const numSplats = 8;
        for(let i = 0; i < numSplats; i++) {
            const angle = (Math.PI * 2 / numSplats) * i + Math.random() * 0.5;
            const dist = PUDDLE_RADIUS * 0.5 + Math.random() * PUDDLE_RADIUS * 0.3;
            const splatX = this.x + Math.cos(angle) * dist;
            const splatY = this.y + Math.sin(angle) * dist;
            const splatRadius = PUDDLE_RADIUS * 0.2 + Math.random() * PUDDLE_RADIUS * 0.3;
            graphics.fillCircle(splatX, splatY, splatRadius);
        }
        graphics.setDepth(-0.5); // Just above ground (-1), below other objects (0)

        // Create a sensor body for the puddle
        const puddleBody = this.scene.matter.add.circle(this.x, this.y, PUDDLE_RADIUS, {
            isSensor: true,
            isStatic: true
        });

        // Add some data to the puddle body so we can detect it
        (puddleBody as any).isPuddle = true;
    }
}
