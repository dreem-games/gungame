import Phaser from 'phaser';
import { IEntity } from '../types/interfaces';
import { InputManager } from '../core/InputManager';

export class Hero extends Phaser.Physics.Matter.Sprite implements IEntity {
    public id: string;
    public gameObject: Phaser.GameObjects.GameObject;
    public isDestroyed: boolean = false;

    private speed: number = 5;
    private inputManager: InputManager;

    constructor(scene: Phaser.Scene, x: number, y: number, inputManager: InputManager) {
        super(scene.matter.world, x, y, 'hero', 'hero');

        this.id = Phaser.Math.RND.uuid();
        this.gameObject = this;
        this.inputManager = inputManager;

        scene.add.existing(this);

        // Setup physics body
        this.setCircle(70);
        this.setFrictionAir(0.1);
        this.setFixedRotation();
        this.setMass(100);
    }

    update(_time: number, _delta: number) {
        if (this.isDestroyed) return;

        // Movement
        const moveVector = this.inputManager.getMovementVector();
        this.setVelocity(moveVector.x * this.speed, moveVector.y * this.speed);

        // Rotation
        const angle = Phaser.Math.Angle.Between(
            this.x,
            this.y,
            this.inputManager.pointerWorldX,
            this.inputManager.pointerWorldY
        );
        this.setRotation(angle);
    }

    destroy(fromScene?: boolean) {
        this.isDestroyed = true;
        super.destroy(fromScene);
    }
}