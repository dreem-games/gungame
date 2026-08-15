import Phaser from 'phaser';

export class Hero extends Phaser.Physics.Matter.Sprite {
    private keys: {
        W: Phaser.Input.Keyboard.Key;
        A: Phaser.Input.Keyboard.Key;
        S: Phaser.Input.Keyboard.Key;
        D: Phaser.Input.Keyboard.Key;
    };
    private speed: number = 5;

    constructor(scene: Phaser.Scene, x: number, y: number) {
        super(scene.matter.world, x, y, 'hero', 'hero');

        scene.add.existing(this);

        // Setup physics body
        this.setCircle(70); // approximate radius of the hero based on 212x152 size
        this.setFrictionAir(0.1);
        this.setFixedRotation(); // Stop rotating on collisions
        this.setMass(100);

        // Setup input
        this.keys = scene.input.keyboard!.addKeys('W,A,S,D') as any;

        // Listen for pointer move to rotate hero towards mouse
        scene.input.on('pointermove', (pointer: Phaser.Input.Pointer) => {
            const cursorPoint = scene.cameras.main.getWorldPoint(pointer.x, pointer.y);
            const angle = Phaser.Math.Angle.Between(this.x, this.y, cursorPoint.x, cursorPoint.y);
            // In libgdx, 0 degrees is right. Phaser's Math.Angle.Between also returns 0 for right.
            this.setRotation(angle);
        });
    }

    update() {
        const force = { x: 0, y: 0 };

        if (this.keys.W.isDown) force.y -= this.speed;
        if (this.keys.S.isDown) force.y += this.speed;
        if (this.keys.A.isDown) force.x -= this.speed;
        if (this.keys.D.isDown) force.x += this.speed;

        // Normalize force if moving diagonally
        if (force.x !== 0 && force.y !== 0) {
            const length = Math.sqrt(force.x * force.x + force.y * force.y);
            force.x = (force.x / length) * this.speed;
            force.y = (force.y / length) * this.speed;
        }

        this.setVelocity(force.x, force.y);
    }
}