package com.gungame.world.controller;

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
     * Применяет к персонажу силу для его передвижения.
     *
     * @param x направление (часть вектора)
     * @param y направление (часть вектора)
     * @param movingMode режим передвижения
     * @return true если сила применена
     */
    protected boolean move(float x, float y, MovingMode movingMode) {
        if (x * x + y * y < .1f) {
            return false;
        }

        var vel = hero.getVelocity();
        float impulseX = getImpulse(vel.x, x, movingMode);
        float impulseY = getImpulse(vel.y, y, movingMode);
        hero.applyImpulse(impulseX, impulseY);
        return true;
    }

    private float getImpulse(float velocity, float accseleration, MovingMode movingMode) {
        float potentialResult = GameWorldConfig.HERO_ACCELERATION * accseleration;
        float maxSpeed = GameWorldConfig.HERO_MAX_VELOCITY * Math.abs(accseleration);
        if (movingMode == MovingMode.RUNNING) {
            potentialResult *= GameWorldConfig.HERO_RUNNING_ACCELERATION_SCALE;
            maxSpeed *= GameWorldConfig.HERO_RUNNING_ACCELERATION_SCALE;
        } else if (movingMode == MovingMode.JUMPING) {
            potentialResult *= GameWorldConfig.HERO_JUMPING_ACCELERATION_SCALE;
            maxSpeed *= GameWorldConfig.HERO_JUMPING_ACCELERATION_SCALE;
        }
        float maxEnabledImpulse = maxSpeed - velocity;
        float minEnabledImpulse = -maxSpeed - velocity;
        return Math.min(
                Math.max(potentialResult, minEnabledImpulse),
                maxEnabledImpulse);
    }

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

    protected enum MovingMode {
        NORMAL,
        RUNNING,
        JUMPING  // как рывок в сторону
    }
}
