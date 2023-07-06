package com.gungame.world.controller;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.gungame.world.GameWorldConfig;
import com.gungame.world.objects.phisical.Hero;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public abstract class HeroController {
    protected final Hero hero;
    protected final Camera camera;

    private @NonNull MovingMode movingMode;
    private long lastMovingModeChange;

    public HeroController(Hero hero, Camera camera) {
        this.hero = hero;
        this.camera = camera;
        this.movingMode = MovingMode.NORMAL;
    }

    /**
     * Выполняет управление персонажем.
     * @return true если контроллер используется в данный момент.
     */
    public abstract boolean control();

    protected void tryChangeMovingMode(MovingMode newMovingMode) {
        if (movingMode == newMovingMode || System.nanoTime() - lastMovingModeChange < newMovingMode.minDuration) {
            return;
        }
        if (hero.tryUseStamina(newMovingMode.staminaCost * newMovingMode.maxDuration / 1_000)) {
            movingMode = newMovingMode;
            lastMovingModeChange = System.nanoTime();
        }
    }

    /**
     * Применяет к персонажу силу для его передвижения.
     *
     * @param x направление (часть вектора)
     * @param y направление (часть вектора)
     * @return true если сила применена
     */
    protected boolean move(float x, float y) {
        if (x * x + y * y < .1f) {
            return false;  // может быть небольшая погрешность
        }

        long time = System.nanoTime();
        long delta = time - lastMovingModeChange;
        if (movingMode.maxDuration > 0 && delta > movingMode.maxDuration * 1000) {
            movingMode = MovingMode.NORMAL;
        } else if (delta > movingMode.minDuration * 1000 && !hero.tryUseStamina(movingMode.staminaCost * delta / 1_000_000)) {
            movingMode = MovingMode.NORMAL;
        }

        float impulseX = getImpulse(x, movingMode);
        float impulseY = getImpulse(y, movingMode);
        hero.applyImpulse(impulseX, impulseY);
        return true;
    }

    private float getImpulse(float acceleration, MovingMode movingMode) {
        float potentialResult = GameWorldConfig.HERO_ACCELERATION * acceleration;
        if (movingMode == MovingMode.RUNNING) {
            potentialResult *= GameWorldConfig.HERO_RUNNING_ACCELERATION_SCALE;
        } else if (movingMode == MovingMode.JUMPING) {
            potentialResult *= GameWorldConfig.HERO_JUMPING_ACCELERATION_SCALE;
        }
        return potentialResult;
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

    @Getter
    @RequiredArgsConstructor
    protected enum MovingMode {
        NORMAL (0, 0, 0),
        RUNNING(0, 1000, 1),
        JUMPING(100, 100, 900);  // как рывок в сторону

        private final long minDuration;
        private final long maxDuration;
        private final float staminaCost;  // стоимость в секундах
    }
}
