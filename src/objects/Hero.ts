import Phaser from 'phaser';

import { EventDispatcher } from '../core/EventBus';
import { InputManager } from '../core/InputManager';
import { IEntity } from '../types/interfaces';
import { WeaponManager } from '../weapons/WeaponManager';

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

    private isSlowed: boolean = false;

    // Knockback: время, на которое взрыв вырывает управление из-под игрока
    private knockbackTime: number = 0;

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

    public isDashingNow(): boolean {
        return this.isDashing;
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
        // Смерть отменяет перезарядку — иначе звук перезагрузки затрет звук смерти
        this.weaponManager.cancelReload();
        this.scene.sound.play('death');

        // Switch sprite to dead
        this.setFrame('hero_dead');
        this.setOrigin(0.5, 0.5); // Center origin

        // Make body a sensor so projectiles pass through, but we still keep it around
        this.setSensor(true);
        this.setFrictionAir(0.99); // stop movement
        this.laserGraphics.clear();
    }

    public setSlowed(slowed: boolean) {
        this.isSlowed = slowed;
    }

    // Ударная волна от взрыва: физика сама разгоняет тело, на это время
    // отключаем обычное управление, чтобы setVelocity не сбивал импульс.
    public setKnockback(duration: number) {
        this.knockbackTime = Math.max(this.knockbackTime, duration);
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

        // Во время отброса от взрыва управление заморожено — физика гонит тело
        if (this.knockbackTime > 0) {
            this.knockbackTime -= delta;
            // Если дэш начался в момент взрыва, гасим его — иначе isDashing застрянет
            if (this.isDashing) this.isDashing = false;
        } else {
            // Handle Dash initialization
            if (
                this.inputManager.isDashing() &&
                !this.isDashing &&
                this.dashCooldown <= 0 &&
                this.currentStamina >= this.dashStaminaCost
            ) {
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

                if (this.isSlowed) {
                    currentSpeed *= 0.5;
                }

                this.setVelocity(moveVector.x * currentSpeed, moveVector.y * currentSpeed);
            }
        } // end knockback check

        // Reset slowed state; sensor collisions will reapply it if still inside
        this.isSlowed = false;

        // Смещение дула от центра спрайта (локальные координаты)
        const FIRE_POSITION_DX = 1.7 * 100; // Increased X to reach the end of the barrel
        const FIRE_POSITION_DY = 0.36 * 100; // Increased Y slightly
        const MUZZLE_REACH = Math.hypot(FIRE_POSITION_DX, FIRE_POSITION_DY);

        // Поворот спрайта: ствол смотрит на курсор.
        // angle = направление «центр → курсор» минус поправка на смещение дула,
        // чтобы лазер из дула проходил ровно через курсор.
        // Если курсор ближе, чем MUZZLE_REACH («за дулом» — решения нет),
        // держим текущий поворот, чтобы не было разворота на 180° и выстрела в спину.
        const pointerDist = Phaser.Math.Distance.Between(
            this.x,
            this.y,
            this.inputManager.pointerWorldX,
            this.inputManager.pointerWorldY
        );
        const angle =
            pointerDist > MUZZLE_REACH
                ? Phaser.Math.Angle.Between(
                      this.x,
                      this.y,
                      this.inputManager.pointerWorldX,
                      this.inputManager.pointerWorldY
                  ) - Math.asin(FIRE_POSITION_DY / pointerDist)
                : this.rotation;
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

        // Точка выстрела (дуло) — смещение от центра в сторону взгляда
        const fireCos = Math.cos(angle);
        const fireSin = Math.sin(angle);
        const spawnX = this.x + (FIRE_POSITION_DX * fireCos - FIRE_POSITION_DY * fireSin);
        const spawnY = this.y + (FIRE_POSITION_DX * fireSin + FIRE_POSITION_DY * fireCos);

        // Лазер и пули летят строго по направлению взгляда (из дула).
        // Прицел в курсор на дистанции — это поворот всего спрайта, а не отдельный огонь,
        // поэтому огонь всегда совпадает с направлением поворота спрайта.
        const fireAngle = angle;

        // Пуля вылетает чуть впереди точки, откуда рисуется лазер
        const BULLET_SPAWN_OFFSET = 20;
        const bulletX = spawnX + fireCos * BULLET_SPAWN_OFFSET;
        const bulletY = spawnY + fireSin * BULLET_SPAWN_OFFSET;

        // Draw MVP Laser Pointer
        this.laserGraphics.clear();
        this.laserGraphics.lineStyle(2, 0xff0000, 0.5); // 2px red, 50% opacity
        this.laserGraphics.beginPath();
        this.laserGraphics.moveTo(spawnX, spawnY);
        // Draw laser out far along the angle
        const laserEndX = spawnX + fireCos * 2000;
        const laserEndY = spawnY + fireSin * 2000;
        this.laserGraphics.lineTo(laserEndX, laserEndY);
        this.laserGraphics.strokePath();

        // Shooting
        if (this.inputManager.isShooting) {
            const fireResult = this.weaponManager.fire(bulletX, bulletY, fireAngle, _time);

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
