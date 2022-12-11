package com.gungame.world.hero;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.gungame.world.GameObject;
import com.gungame.world.GameWorldConfig;

public abstract class HeroController {

    protected final GameObject hero;
    protected final Camera camera;

    public HeroController(GameObject hero, Camera camera) {
        this.hero = hero;
        this.camera = camera;
    }

    public abstract void control();

    protected void move(float x, float y) {
        Vector2 vel = hero.getVelocity();
        hero.applyImpulse(getImpulse(vel.x, x), getImpulse(vel.y, y));
    }

    private float getImpulse(float velocity, float accseleration) {
        if (Math.sin(velocity) != Math.sin(accseleration)
                || Math.abs(velocity + accseleration) > GameWorldConfig.HERO_MAX_VELOCITY) {
            return GameWorldConfig.HERO_ACCELERATION * accseleration;
        }
        return 0f;
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
