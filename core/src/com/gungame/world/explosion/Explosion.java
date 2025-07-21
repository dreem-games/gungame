package com.gungame.world.explosion;

import box2dLight.Light;
import box2dLight.PointLight;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gungame.world.GameWorld;
import com.gungame.world.collision.CollisionFilters;
import lombok.Getter;


public class Explosion {
    private static final long STATE_CHANGE_TIME = 16;

    public @Getter float x, y;
    private float stateTime = 0;
    private final Light light;
    private long lastStateChange;

    public Explosion(GameWorld gameWorld, float x, float y, float radius) {
        this.x = x;
        this.y = y;
        lastStateChange = System.currentTimeMillis();

        light = new PointLight(gameWorld.getRayHandler(), 512);
        light.setPosition(x, y);
        light.setDistance(radius);
        light.setSoft(true);
        light.setSoftnessLength(2f);
        light.setContactFilter(CollisionFilters.LOW_LIGHT_CONTACT_FILTER);
        light.setColor(Color.RED);
    }

    public TextureRegion play() {
        long now = System.currentTimeMillis();
        if (now - lastStateChange > STATE_CHANGE_TIME) {
            stateTime++;
            light.setDistance(25f - stateTime);
            lastStateChange = now;
        }

        return ExplosionAnimation.explosionAnimation.getKeyFrame(stateTime, false);
    }

    public boolean isFinished() {
        return stateTime > 25;
    }

    public void destroy() {
        light.remove();
    }
}