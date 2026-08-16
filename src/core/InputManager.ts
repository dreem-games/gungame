import Phaser from 'phaser';

export class InputManager {
    private keys!: {
        W: Phaser.Input.Keyboard.Key;
        A: Phaser.Input.Keyboard.Key;
        S: Phaser.Input.Keyboard.Key;
        D: Phaser.Input.Keyboard.Key;
        SHIFT: Phaser.Input.Keyboard.Key;
        SPACE: Phaser.Input.Keyboard.Key;
    };

    private scene: Phaser.Scene;

    // Extracted pointer coordinates (world space)
    public pointerWorldX: number = 0;
    public pointerWorldY: number = 0;

    constructor(scene: Phaser.Scene) {
        this.scene = scene;

        if (this.scene.input.keyboard) {
            this.keys = this.scene.input.keyboard.addKeys('W,A,S,D,SHIFT,SPACE') as any;
        }

        this.scene.input.on('pointermove', (pointer: Phaser.Input.Pointer) => {
            const worldPoint = this.scene.cameras.main.getWorldPoint(pointer.x, pointer.y);
            this.pointerWorldX = worldPoint.x;
            this.pointerWorldY = worldPoint.y;
        });
    }

    /**
     * Returns a normalized movement vector based on currently pressed keys
     */
    public getMovementVector(): Phaser.Math.Vector2 {
        const vector = new Phaser.Math.Vector2(0, 0);

        if (!this.keys) return vector;

        if (this.keys.W.isDown) vector.y -= 1;
        if (this.keys.S.isDown) vector.y += 1;
        if (this.keys.A.isDown) vector.x -= 1;
        if (this.keys.D.isDown) vector.x += 1;

        if (vector.x !== 0 || vector.y !== 0) {
            vector.normalize();
        }

        return vector;
    }

    public isRunning(): boolean {
        return this.keys && this.keys.SHIFT.isDown;
    }

    public isDashing(): boolean {
        // We use JustDown to only trigger once per press
        return this.keys && Phaser.Input.Keyboard.JustDown(this.keys.SPACE);
    }

    public update() {
        // Fallback for pointer if it doesn't move but camera moves
        const pointer = this.scene.input.activePointer;
        const worldPoint = this.scene.cameras.main.getWorldPoint(pointer.x, pointer.y);
        this.pointerWorldX = worldPoint.x;
        this.pointerWorldY = worldPoint.y;
    }
}