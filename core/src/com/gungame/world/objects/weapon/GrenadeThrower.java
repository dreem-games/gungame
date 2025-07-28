package com.gungame.world.objects.weapon;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.gungame.world.explosion.AssetManager;
import com.gungame.world.objects.meta.CustomObjectInitializationConfig;
import com.gungame.world.objects.phisical.Hero;
import lombok.Getter;

public class GrenadeThrower {
    private @Getter int ammo = 2;
    private @Getter long throwTimer;
    private final Hero hero;
    private final int cooldown = 1000;
    private long cooldownTimer = 0;
    private float stateTime = 0f;

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

    public void render(SpriteBatch batch, Vector2 firePosition, float angle) {
        if(throwTimer == 0) {
            return;
        }
        float scale = 1f + 0.05f * (float)Math.sin(stateTime++ / 10f);
        float reverseScale = 1 / scale;
        var firePositionX = firePosition.x + throwPower() * .15f * (float)Math.cos(angle);
        var firePositionY = firePosition.y  + throwPower() * .15f * (float)Math.sin(angle);
        batch.draw(AssetManager.targetTextures[0], firePositionX , firePositionY, 0.25f, 0.25f, .5f, .5f, scale, scale, stateTime);
        batch.draw(AssetManager.targetTextures[1], firePositionX , firePositionY, 0.25f, 0.25f, .5f,.5f, reverseScale, reverseScale, -stateTime);
        batch.draw(AssetManager.targetTextures[2], firePositionX , firePositionY, 0.5f, 0.5f);
    }

    public float throwPower() {
        float throwPower;
        throwPower = (System.currentTimeMillis() - throwTimer) / 10f;
        if (throwPower > 100) {
            throwPower = 100;
        }
        return throwPower;
    }

    public void throwGrenadeEnd() {
        if (ammo <= 0 || (System.currentTimeMillis() - cooldownTimer < cooldown && cooldownTimer != 0)) {
            return;
        }
        throwGrenade(throwPower());
        ammo--;
        throwTimer = 0;
        cooldownTimer = System.currentTimeMillis();
    }

    public void throwGrenade(float throwPower) {
        var grenadeFactory = hero.getWorld().getPhysicalObjectFactoryManager().getGrenadeFactory();
        float angle = hero.getAngle();
        Vector2 firePosition = hero.getFirePosition();
        CustomObjectInitializationConfig customInitConfig = new CustomObjectInitializationConfig();
        grenadeFactory.create(firePosition, angle * MathUtils.radiansToDegrees, customInitConfig,
                grenade -> {
                    float powerPrecent = throwPower / 100;
                    grenade.setVelocity(
                            MathUtils.cos(angle) * powerPrecent,
                            MathUtils.sin(angle) * powerPrecent);
                    grenade.setAngularVelocity(powerPrecent);
                });
    }
}
