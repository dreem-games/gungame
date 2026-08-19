import Phaser from 'phaser';
import { IEntity } from '../types/interfaces';
import { InputManager } from '../core/InputManager';
import { WeaponManager } from '../weapons/WeaponManager';
import { EventDispatcher } from '../core/EventBus';

export class Hero extends Phaser.Physics.Matter.Sprite implements IEntity {
    public id: string;
    public gameObject: Phaser.GameObjects.GameObject;
    public isDestroyed: boolean = false;

    private inputManager: InputManager;
    private weaponManager: WeaponManager;

    // Movement config
    private baseSpeed: number = 5;
    private runSpeed: number = 9;
    private dashSpeed: number = 50;
    private dashDuration: number = 250; // ms

    // State
    private isDashing: boolean = false;
    private dashTimer: number = 0;
    private dashCooldown: number = 0;

    // Stamina config
    public maxStamina: number = 100;
    public currentStamina: number = 100;
    private runStaminaCost: number = 20; // per second
    private dashStaminaCost: number = 30; // flat cost
    private staminaRegen: number = 15; // per second

    // Laser pointer MVP
    private laserGraphics: Phaser.GameObjects.Graphics;

    // Health System
    public hp: number = 100;
    public isDead: boolean = false;

    constructor(scene: Phaser.Scene, x: number, y: number, inputManager: InputManager) {
        super(scene.matter.world, x, y, 'hero', 'hero');

        this.laserGraphics = scene.add.graphics();
        this.laserGraphics.setDepth(1);

        this.id = Phaser.Math.RND.uuid();
        this.gameObject = this;
        this.inputManager = inputManager;

        scene.add.existing(this);

        // Setup physics body
        // Body needs to be smaller and offset towards the head
        // The original sprite is 212x152. By default the origin is 0.5, 0.5.
        // We make the physics body a much smaller circle, offset heavily to the left
        // (because in the sprite, the head is on the left side facing right).
        const radius = 30;
        this.setBody({
            type: 'circle',
            radius: radius
        });

        // In Matter.js with Phaser, setting a new body resets origin to center of mass.
        // We shift the visual sprite relative to the physical body.
        // The hero's head in the sprite (facing right) is at roughly X=50, Y=76
        // This means the center of the physics circle should be offset to the left of the sprite center.
        this.setOrigin(0.2, 0.5);

        this.setFrictionAir(0.1);
        this.setFixedRotation();
        this.setMass(100);

        this.weaponManager = new WeaponManager(scene);
    }

    public getWeaponManager(): WeaponManager {
        return this.weaponManager;
    }

    public takeDamage(amount: number) {
        if (this.isDead) return;

        this.hp -= amount;
        if (this.hp <= 0) {
            this.hp = 0;
            this.die();
        }

        EventDispatcher.emit('hero-damage', this.hp);
    }

    private die() {
        this.isDead = true;

        EventDispatcher.emit('hero-death');
        this.scene.sound.play('death');

        // Switch sprite to dead
        this.setFrame('hero_dead');
        this.setOrigin(0.5, 0.5); // Center origin

        // Make body a sensor so projectiles pass through, but we still keep it around
        this.setSensor(true);
        this.setFrictionAir(0.99); // stop movement
        this.laserGraphics.clear();
    }

    update(_time: number, delta: number) {
        if (this.isDestroyed || this.isDead) return;

        // Decrease cooldowns
        if (this.dashCooldown > 0) this.dashCooldown -= delta;

        // Handle Stamina Regen
        if (!this.inputManager.isRunning() && !this.isDashing) {
            this.currentStamina = Math.min(this.maxStamina, this.currentStamina + this.staminaRegen * (delta / 1000));
        }

        const moveVector = this.inputManager.getMovementVector();

        // Handle Dash initialization
        if (this.inputManager.isDashing() && !this.isDashing && this.dashCooldown <= 0 && this.currentStamina >= this.dashStaminaCost) {
            this.isDashing = true;
            this.dashTimer = this.dashDuration;
            this.currentStamina -= this.dashStaminaCost;
            this.dashCooldown = 1000; // 1 second cooldown

            // If no movement vector, dash forward (towards cursor)
            if (moveVector.x === 0 && moveVector.y === 0) {
                const angle = this.rotation;
                moveVector.x = Math.cos(angle);
                moveVector.y = Math.sin(angle);
            }
        }

        // Apply movement
        if (this.isDashing) {
            this.dashTimer -= delta;
            if (this.dashTimer <= 0) {
                this.isDashing = false;
            } else {
                // Ignore other inputs while dashing, maintain high velocity
                // We keep the vector from when dash started, but normalize it
                if (moveVector.length() === 0) {
                     moveVector.x = Math.cos(this.rotation);
                     moveVector.y = Math.sin(this.rotation);
                }
                this.setVelocity(moveVector.x * this.dashSpeed, moveVector.y * this.dashSpeed);
            }
        }

        if (!this.isDashing) {
            let currentSpeed = this.baseSpeed;

            if (this.inputManager.isRunning() && this.currentStamina > 0 && moveVector.length() > 0) {
                currentSpeed = this.runSpeed;
                this.currentStamina = Math.max(0, this.currentStamina - this.runStaminaCost * (delta / 1000));
            }

            this.setVelocity(moveVector.x * currentSpeed, moveVector.y * currentSpeed);
        }

        // Rotation
        const angle = Phaser.Math.Angle.Between(
            this.x,
            this.y,
            this.inputManager.pointerWorldX,
            this.inputManager.pointerWorldY
        );
        this.setRotation(angle);

        // Weapon Switching
        const switchIdx = this.inputManager.getWeaponSwitch();
        if (switchIdx !== null) {
            this.weaponManager.switchWeapon(switchIdx);
        } else if (this.inputManager.wheelDirection > 0) {
            this.weaponManager.switchNext();
        } else if (this.inputManager.wheelDirection < 0) {
            this.weaponManager.switchPrev();
        }

        // Reloading
        if (this.inputManager.isReloading()) {
            this.weaponManager.reload();
        }

        // Calculate Bullet Spawn Position
        const FIRE_POSITION_DX = 1.7 * 100; // Increased X to reach the end of the barrel
        const FIRE_POSITION_DY = 0.36 * 100; // Increased Y slightly

        const cos = Math.cos(angle);
        const sin = Math.sin(angle);

        const rotatedX = FIRE_POSITION_DX * cos - FIRE_POSITION_DY * sin;
        const rotatedY = FIRE_POSITION_DX * sin + FIRE_POSITION_DY * cos;

        const spawnX = this.x + rotatedX;
        const spawnY = this.y + rotatedY;

        // Пуля вылетает чуть впереди точки, откуда рисуется лазер
        const BULLET_SPAWN_OFFSET = 20;
        const bulletX = spawnX + cos * BULLET_SPAWN_OFFSET;
        const bulletY = spawnY + sin * BULLET_SPAWN_OFFSET;

        // Draw MVP Laser Pointer
        this.laserGraphics.clear();
        this.laserGraphics.lineStyle(2, 0xff0000, 0.5); // 2px red, 50% opacity
        this.laserGraphics.beginPath();
        this.laserGraphics.moveTo(spawnX, spawnY);
        // Draw laser out far along the angle
        const laserEndX = spawnX + cos * 2000;
        const laserEndY = spawnY + sin * 2000;
        this.laserGraphics.lineTo(laserEndX, laserEndY);
        this.laserGraphics.strokePath();

        // Shooting
        if (this.inputManager.isShooting) {
            const fireResult = this.weaponManager.fire(bulletX, bulletY, angle, _time);

            if (fireResult === false && this.inputManager.justPressedShoot) {
                // Out of ammo
                this.scene.sound.play('empty_gun_shot');
            }
        }
    }

    destroy(fromScene?: boolean) {
        this.isDestroyed = true;
        this.laserGraphics.destroy();
        super.destroy(fromScene);
    }
}