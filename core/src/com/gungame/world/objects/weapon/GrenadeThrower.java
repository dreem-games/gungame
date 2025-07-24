package com.gungame.world.objects.weapon;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.gungame.world.explosion.ExplosionAnimation;
import com.gungame.world.objects.phisical.Hero;
import lombok.Getter;

public class GrenadeThrower {

    public @Getter int ammo = 4;
    private @Getter long throwTimer;
    public Hero hero;
    public int cooldown = 1000;
    public long cooldownTimer = 0;
    public float stateTime = 0f;
    public Texture throwIndicator = new Texture("ui/rifle.png");

    public GrenadeThrower(Hero hero) {
        this.hero = hero;
    }

    public void throwGrenadeStart() {
        if (ammo <= 0 || (System.currentTimeMillis() - cooldownTimer < cooldown && cooldownTimer != 0)) {
            return;
        }
        System.out.println(System.currentTimeMillis() - cooldownTimer > cooldown);
        cooldownTimer = 0;
        throwTimer = System.currentTimeMillis();
    }

    public float drawIndicator() {
        long timer = hero.getGrenadeThrower().getThrowTimer();
        if (timer == 0) {
            return 0;
        }
        if ((System.currentTimeMillis() - timer) / 1000f > 1) {
            return 1;
        } else return ((System.currentTimeMillis() - timer) / 1000f);
    }

    public void render(SpriteBatch batch, Vector2 firePosition, float angle) {
        if(throwTimer == 0) {
            return;
        }
        float scale = 1f + 0.05f * (float)Math.sin(stateTime++ / 10f);
        float reverseScale = 1 / scale;
        var firePositionX = firePosition.x + drawIndicator() * 15 * (float)Math.cos(angle);
        var firePositionY = firePosition.y  + drawIndicator() * 15 * (float)Math.sin(angle);
        batch.draw(ExplosionAnimation.targetTextures[0], firePositionX , firePositionY, 0.25f, 0.25f, .5f, .5f, scale, scale, stateTime);
        batch.draw(ExplosionAnimation.targetTextures[1], firePositionX , firePositionY, 0.25f, 0.25f, .5f,.5f, reverseScale, reverseScale, -stateTime);
        batch.draw(ExplosionAnimation.targetTextures[2], firePositionX , firePositionY, 0.5f, 0.5f);
    }

    public void throwGrenadeEnd() {
        if (ammo <= 0 || (System.currentTimeMillis() - cooldownTimer < cooldown && cooldownTimer != 0)) {
            return;
        }
        float throwPower;
        throwPower = (System.currentTimeMillis() - throwTimer) / 10f;
        if (throwPower > 100) {
            throwPower = 100;
        }
        hero.throwGrenade(throwPower);
        ammo--;
        throwTimer = 0;
        cooldownTimer = System.currentTimeMillis();
    }

}
