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
        float angle = hero.getAngle();
        if (angle < 0) {
            angle += MathUtils.PI2;
        }

        var heroVector = new Vector2(1, 0);
        float targetAngle = MathUtils.acos(heroVector.dot(x, y));
        if (y < 0) {
            targetAngle = MathUtils.PI2 - targetAngle;
        }

        float targetVel = targetAngle - angle;
        while (targetVel > MathUtils.PI) {
            targetVel -= MathUtils.PI2;
        }
        while (targetVel < -MathUtils.PI) {
            targetVel += MathUtils.PI2;
        }
        hero.setAngularVelocity(targetVel * GameWorldConfig.HERO_ROTATION_SPEED);
    }
}
