import Phaser from 'phaser';

export class ThinWallSegment extends Phaser.Physics.Matter.Sprite {
    private health: number = 30;

    constructor(scene: Phaser.Scene, x: number, y: number) {
        // We will just create a tiny grey rectangle for the segment
        // Alternatively, we can load a texture, but drawing a graphics is easy
        super(scene.matter.world, x, y, 'thin_wall_tex');
        this.scene.add.existing(this);

        this.setBody({ type: 'rectangle', width: 32, height: 32 });
        this.setStatic(true);
        this.setData('isThinWall', true);
    }

    public takeDamage(amount: number) {
        this.health -= amount;

        // Darken based on damage
        const tint = Math.max(0x555555, 0xffffff - (30 - this.health) * 0x050505);
        this.setTint(tint);

        if (this.health <= 0) {
            this.destroy();
        }
    }
}

export class ThinWall {
    private segments: ThinWallSegment[] = [];

    constructor(scene: Phaser.Scene, startX: number, startY: number, length: number, isVertical: boolean) {
        // Create a graphics texture for the thin wall segment if it doesn't exist
        if (!scene.textures.exists('thin_wall_tex')) {
            const g = scene.make.graphics({x: 0, y: 0});
            g.fillStyle(0xa0a0a0);
            g.fillRect(0, 0, 32, 32);
            g.lineStyle(2, 0x505050);
            g.strokeRect(0, 0, 32, 32);
            g.generateTexture('thin_wall_tex', 32, 32);
            g.destroy();
        }

        const segmentsCount = Math.floor(length / 32);

        for (let i = 0; i < segmentsCount; i++) {
            const x = startX + (isVertical ? 0 : i * 32);
            const y = startY + (isVertical ? i * 32 : 0);
            const segment = new ThinWallSegment(scene, x, y);
            this.segments.push(segment);
        }
    }
}
