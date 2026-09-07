import Phaser from 'phaser';

import { IEntity } from '../types/interfaces';

export class Barrel extends Phaser.Physics.Matter.Sprite implements IEntity {
    public id: string;
    public gameObject: Phaser.GameObjects.GameObject;
    public isDestroyed: boolean = false;

    constructor(scene: Phaser.Scene, x: number, y: number) {
        super(scene.matter.world, x, y, 'level1', 'barrel');

        this.id = Phaser.Math.RND.uuid();
        this.gameObject = this;
        scene.add.existing(this);

        this.setBody({ type: 'circle', radius: 128 });
        this.setScale(0.5);
        this.setFrictionAir(0.1);
        this.setMass(50);
        this.setDepth(0);
        this.setData('blocksVision', true);
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
