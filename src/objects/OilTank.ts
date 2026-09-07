import Phaser from 'phaser';

export class OilTank extends Phaser.Physics.Matter.Sprite {
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
}
