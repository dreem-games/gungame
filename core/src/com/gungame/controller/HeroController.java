package com.gungame.controller;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.gungame.world.GameWorldConfig;
import com.gungame.world.objects.phisical.Hero;

public abstract class HeroController {
    protected final Hero hero;
    protected final Camera camera;

    public HeroController(Hero hero, Camera camera) {
        this.hero = hero;
        this.camera = camera;
    }

    /**
     * Выполняет управление персонажем.
     * @return true если контроллер используется в данный момент.
     */
    public abstract boolean control();

    /**
     * Поворачивает персонажа учитывая, что он стоит в начале координат,
     * а должен смотреть по направлению x, y.
     * Рекомендуется передавать координаты в нормализованном виде.
     */
    protected void rotate(float x, float y) {
        rotate(MathUtils.atan2(y, x));
    }

    /**
     * Поворачивает персонажа учитывая его положение
     *  и точку куда он должен смотреть.
     */
    protected void rotateToPoint(float targetX, float targetY) {
        if (isMouseOverBody(targetX, targetY)) {
            return;
        }
        Vector2 firePos = hero.getFirePosition(); // мировая позиция дула
        rotate(targetX - firePos.x, targetY - firePos.y);
    }

    private void rotate(float targetAngle) {
        float currentAngle = hero.getAngle();
        float delta = targetAngle - currentAngle;

        // Нормализуем угол от -π до +π
        while (delta > MathUtils.PI) delta -= MathUtils.PI2;
        while (delta < -MathUtils.PI) delta += MathUtils.PI2;
        if (delta > GameWorldConfig.HERO_MAX_ROTATION_SPEED) {
            delta = GameWorldConfig.HERO_MAX_ROTATION_SPEED;
        }
        if (delta < -GameWorldConfig.HERO_MAX_ROTATION_SPEED) {
            delta = -GameWorldConfig.HERO_MAX_ROTATION_SPEED;
        }
        if (Math.abs(delta) < 0.01f) {
            return;
        }

        // Установка угловой скорости
        hero.setAngularVelocity(delta * GameWorldConfig.HERO_ROTATION_SPEED);
    }

    protected boolean isMouseOverBody(float mouseX, float mouseY) {
        Vector2 pos = hero.getPosition();
        float radius = hero.getBodyRadius();

        float dx = mouseX - pos.x;
        float dy = mouseY - pos.y;
        return dx * dx + dy * dy < radius * radius;
    }
}
