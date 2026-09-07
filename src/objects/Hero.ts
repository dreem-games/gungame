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

    // Movement state (состояние для HUD и sendInput; сами позиции приходят из снимков сервера)
    private isDashing: boolean = false;
    private dashTimer: number = 0;
    private dashCooldown: number = 0;
    private dashDuration: number = 250; // ms

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
        // Круг r=30 — паритет с серверным хитбоксом (playerRadius=30).
        // frictionAir: 0 — как у серверного тела; позицию двигают только снапшоты.
        this.setExistingBody(scene.matter.bodies.circle(0, 0, 30, { frictionAir: 0 }));

        this.setFixedRotation();

        // Matter.js сбрасывает origin в центр массы при замене тела.
        // Центр спрайта (физический круг) — не геометрический центр текстуры,
        // поэтому картинка смещена относительно тела (как и раньше).
        this.setOrigin(0.2, 0.5);
        this.setPosition(x, y);

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

    public applyServerHealth(hp: number) {
        if (hp < this.hp) this.takeDamage(this.hp - hp);
    }

    public applyServerStamina(stamina: number) {
        this.currentStamina = stamina;
    }

    private die() {
        this.isDead = true;

        // Смерть отменяет перезарядку — иначе звук перезагрузки затрет звук смерти
        this.weaponManager.cancelReload();
        this.scene.sound.play('death');

        // Switch sprite to dead
        this.setFrame('hero_dead');
        this.setOrigin(0.5, 0.5);
        this.setDepth(-0.5);
        this.setVelocity(0, 0);

        // Make body a sensor so projectiles pass through, but we still keep it around
        if (this.body) this.scene.matter.world.remove(this.body);
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

        // Дэш: фиксируем состояние (HUD, sendInput); саму скорость даёт серверная физика
        if (this.isDashing) {
            this.dashTimer -= delta;
            if (this.dashTimer <= 0) {
                this.isDashing = false;
            }
        } else if (
            this.inputManager.isDashing() &&
            this.dashCooldown <= 0 &&
            this.currentStamina >= this.dashStaminaCost
        ) {
            this.isDashing = true;
            this.dashTimer = this.dashDuration;
            this.currentStamina -= this.dashStaminaCost;
            this.dashCooldown = 1000; // 1 second cooldown
        }

        // Бег расходует стамину, пока задано движение (для HUD; движение решает сервер)
        if (!this.isDashing && this.inputManager.isRunning() && this.currentStamina > 0 && moveVector.length() > 0) {
            this.currentStamina = Math.max(0, this.currentStamina - this.runStaminaCost * (delta / 1000));
        }

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
