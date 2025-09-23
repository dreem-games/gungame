package com.gungame.world.objects.weapon;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.gungame.assets.TextureManager;
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
        cooldownTimer = 0;
        throwTimer = System.currentTimeMillis();
    }

    public void render(SpriteBatch batch, Vector2 firePosition, float angle) {
        String atlasPath = "assets/ui/target.atlas";
        if (throwTimer == 0) return;
        TextureRegion r1 = TextureManager.getRegion(atlasPath, "target_1");
        TextureRegion r2 = TextureManager.getRegion(atlasPath, "target_2");
        TextureRegion r3 = TextureManager.getRegion(atlasPath, "target_3");
        final float PPM = 1f / 512f; // пример
        float w1 = r1.getRegionWidth()  * PPM, h1 = r1.getRegionHeight() * PPM;
        float w2 = r2.getRegionWidth()  * PPM, h2 = r2.getRegionHeight() * PPM;
        float w3 = r3.getRegionWidth()  * PPM, h3 = r3.getRegionHeight() * PPM;

        float scale = 1f + 0.05f * (float)Math.sin(stateTime++ / 10f);
        float reverseScale = 1f / scale;

        float cx = firePosition.x + throwPower() * .15f * (float)Math.cos(angle);
        float cy = firePosition.y + throwPower() * .15f * (float)Math.sin(angle);

        batch.draw(r1, cx - w1/2f, cy - h1/2f, w1/2f, h1/2f, w1, h1, scale,        scale,        stateTime);
        batch.draw(r2, cx - w2/2f, cy - h2/2f, w2/2f, h2/2f, w2, h2, reverseScale, reverseScale, -stateTime);

        batch.draw(r3, cx - w3/2f, cy - h3/2f, w3, h3);
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
