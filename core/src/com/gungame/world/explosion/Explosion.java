package com.gungame.world.explosion;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import lombok.Getter;

public class Explosion {
    public @Getter float x, y, drawX, drawY;
    float stateTime = 0;

    public Explosion(float x, float y) {
        this.x = x;
        this.y = y;
        drawX = x - ExplosionAnimation.explosionAnimation.getKeyFrame(0).getRegionHeight() / 2f;
        drawY = y - ExplosionAnimation.explosionAnimation.getKeyFrame(0).getRegionWidth() / 2f;

    }

    public TextureRegion play() {
        return ExplosionAnimation.explosionAnimation.getKeyFrame(stateTime++, false);
    }

    public boolean isFinished() {
        return stateTime > 25;
    }
}