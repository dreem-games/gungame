package com.gungame.world.explosion;

import box2dLight.Light;
import box2dLight.PointLight;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gungame.world.GameWorld;
import lombok.Getter;

import static com.gungame.world.light.Lights.RAY_CONTACT_FILTER;

public class Explosion {
    public @Getter float x, y, drawX, drawY;
    private float stateTime = 0;
    private Light light;

    public Explosion(GameWorld gameWorld, float x, float y) {
        this.x = x;
        this.y = y;
        drawX = x - ExplosionAnimation.explosionAnimation.getKeyFrame(0).getRegionHeight() / 2f;
        drawY = y - ExplosionAnimation.explosionAnimation.getKeyFrame(0).getRegionWidth() / 2f;

        light = new PointLight(gameWorld.getRayHandler(), 512);
        light.setPosition(x, y);
        light.setDistance(15f);
        light.setSoft(true);
        light.setSoftnessLength(7.5f);
        light.setContactFilter(RAY_CONTACT_FILTER);
        light.setColor(Color.RED);
    }

    public TextureRegion play() {
        light.setDistance(15f - stateTime / 2);
        return ExplosionAnimation.explosionAnimation.getKeyFrame(stateTime++, false);
    }

    public boolean isFinished() {
        return stateTime > 25;
    }

    public void destroy() {
        light.remove();
    }
}