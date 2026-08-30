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
        this.setData('blocksVision', true);
    }

    public takeDamage(amount: number) {
        if (this.isTankDestroyed) return;
        this.tankHealth -= amount;

        if (this.tankHealth <= 0) {
            this.explode();
        }
    }

    private explode() {
        this.isTankDestroyed = true;

        // Create the puddle
        this.createPuddle();

        // Танк разрушается и разливает масло без взрывной волны.
        this.destroy();
    }

    public createPuddle() {
        const PUDDLE_RADIUS = 600; // Large puddle

        // Procedurally draw a splat
        const graphics = this.scene.add.graphics();
        graphics.fillStyle(0x1a2b1a, 0.8); // Dark greenish-black oil color

        // Helper to create a sensor body
        const createSensor = (x: number, y: number, radius: number) => {
            const body = this.scene.matter.add.circle(x, y, radius, {
                isSensor: true,
                isStatic: true
            });
            (body as any).isPuddle = true;
        };

        // Main center puddle part
        const mainRadius = PUDDLE_RADIUS * 0.7;
        graphics.fillCircle(this.x, this.y, mainRadius);
        createSensor(this.x, this.y, mainRadius);

        // Draw and create physics sensors for smaller intersecting circles around the edges
        const numSplats = 8;
        for (let i = 0; i < numSplats; i++) {
            const angle = ((Math.PI * 2) / numSplats) * i;
            const dist = PUDDLE_RADIUS * 0.65;
            const splatX = this.x + Math.cos(angle) * dist;
            const splatY = this.y + Math.sin(angle) * dist;
            const splatRadius = PUDDLE_RADIUS * 0.35;

            graphics.fillCircle(splatX, splatY, splatRadius);
            createSensor(splatX, splatY, splatRadius);
        }
        graphics.setDepth(-0.5); // Just above ground (-1), below other objects (0)
    }
}
