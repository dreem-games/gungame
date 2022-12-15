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

    public abstract void control();

    protected void move(float x, float y, boolean isRunning) {
        var vel = hero.getVelocity();
        float impulseX = getImpulse(vel.x, x, isRunning);
        float impulseY = getImpulse(vel.y, y, isRunning);
        hero.applyImpulse(impulseX, impulseY);
    }

    private float getImpulse(float velocity, float accseleration, boolean isRunning) {
        float potentialResult = GameWorldConfig.HERO_ACCELERATION * accseleration;
        float maxSpeed = GameWorldConfig.HERO_MAX_VELOCITY * Math.abs(accseleration);
        if (isRunning) {
            potentialResult *= GameWorldConfig.HERO_RUNNING_ACCELERATION_SCALE;
            maxSpeed *= GameWorldConfig.HERO_RUNNING_ACCELERATION_SCALE;
        }
        float maxEnabledImpulse = maxSpeed - velocity;
        float minEnabledImpulse = -maxSpeed - velocity;
        return Math.min(
                Math.max(potentialResult, minEnabledImpulse),
                maxEnabledImpulse);
    }

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
