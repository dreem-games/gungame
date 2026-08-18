import Phaser from 'phaser';

export class InputManager {
    private keys!: {
        W: Phaser.Input.Keyboard.Key;
        A: Phaser.Input.Keyboard.Key;
        S: Phaser.Input.Keyboard.Key;
        D: Phaser.Input.Keyboard.Key;
        SHIFT: Phaser.Input.Keyboard.Key;
        SPACE: Phaser.Input.Keyboard.Key;
        R: Phaser.Input.Keyboard.Key;
        ONE: Phaser.Input.Keyboard.Key;
        TWO: Phaser.Input.Keyboard.Key;
        THREE: Phaser.Input.Keyboard.Key;
    };

    private scene: Phaser.Scene;

    // Extracted pointer coordinates (world space)
    public pointerWorldX: number = 0;
    public pointerWorldY: number = 0;

    // Shooting tracking
    public isShooting: boolean = false;
    public justPressedShoot: boolean = false;
    public wheelDirection: number = 0;

    constructor(scene: Phaser.Scene) {
        this.scene = scene;

        if (this.scene.input.keyboard) {
            this.keys = this.scene.input.keyboard.addKeys('W,A,S,D,SHIFT,SPACE,R,ONE,TWO,THREE') as any;
        }

        this.scene.input.on('pointermove', (pointer: Phaser.Input.Pointer) => {
            const worldPoint = this.scene.cameras.main.getWorldPoint(pointer.x, pointer.y);
            this.pointerWorldX = worldPoint.x;
            this.pointerWorldY = worldPoint.y;
        });

        // Track mouse shooting
        this.scene.input.on('pointerdown', (pointer: Phaser.Input.Pointer) => {
            if (pointer.primaryDown) {
                this.isShooting = true;
                this.justPressedShoot = true;
            }
        });

        this.scene.input.on('pointerup', (pointer: Phaser.Input.Pointer) => {
            if (!pointer.primaryDown) {
                this.isShooting = false;
            }
        });

        // Wheel switching
        this.scene.input.on('wheel', (_pointer: Phaser.Input.Pointer, _gameObjects: any, _deltaX: number, deltaY: number, _deltaZ: number) => {
            if (deltaY > 0) {
                this.wheelDirection = 1;
            } else if (deltaY < 0) {
                this.wheelDirection = -1;
            }
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

    public isReloading(): boolean {
        if (!this.keys) return false;

        const pad = this.scene.input.gamepad?.pad1;
        const padReload = pad ? pad.B : false; // Common reload button (B on Xbox / Circle on PS)

        return Phaser.Input.Keyboard.JustDown(this.keys.R) || padReload;
    }

    public getWeaponSwitch(): number | null {
        if (!this.keys) return null;

        if (Phaser.Input.Keyboard.JustDown(this.keys.ONE)) return 0;
        if (Phaser.Input.Keyboard.JustDown(this.keys.TWO)) return 1;
        if (Phaser.Input.Keyboard.JustDown(this.keys.THREE)) return 2;

        const pad = this.scene.input.gamepad?.pad1;
        if (pad) {
            // Usually D-pad or bumpers for switching
            if (pad.left) return 0;
            if (pad.up) return 1;
            if (pad.right) return 2;
        }

        return null;
    }

    public update() {
        // Fallback for pointer if it doesn't move but camera moves
        const pointer = this.scene.input.activePointer;
        const worldPoint = this.scene.cameras.main.getWorldPoint(pointer.x, pointer.y);
        this.pointerWorldX = worldPoint.x;
        this.pointerWorldY = worldPoint.y;

        // Reset justPressed per frame
        this.justPressedShoot = false;
        this.wheelDirection = 0;

        // Mouse shooting is handled via events, but we also check gamepad here
        const pad = this.scene.input.gamepad?.pad1;
        if (pad) {
            const isTriggerDown = pad.R2 > 0.1; // Right trigger
            if (isTriggerDown && !this.isShooting) {
                this.justPressedShoot = true;
            }
            if (isTriggerDown) {
                this.isShooting = true;
            } else if (!pointer.primaryDown) {
                this.isShooting = false;
            }

            // Override aim direction if right stick is used
            const rsX = pad.rightStick.x;
            const rsY = pad.rightStick.y;
            if (Math.abs(rsX) > 0.1 || Math.abs(rsY) > 0.1) {
                // If using right stick, calculate distance from center to project a target point
                // Assuming hero is in center of camera.
                const cx = this.scene.cameras.main.midPoint.x;
                const cy = this.scene.cameras.main.midPoint.y;
                // Extend the pointer distance
                this.pointerWorldX = cx + rsX * 500;
                this.pointerWorldY = cy + rsY * 500;
            }
        }
    }
}