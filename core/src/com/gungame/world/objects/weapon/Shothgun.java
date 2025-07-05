package com.gungame.world.objects.weapon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Shothgun implements Gun {
    private static final float BULLET_SPREAD = 0.15f;
    private static final int RELOADING_TIME = 3000;
    private static final int RATE_OF_FIRE = 300;
    private static final int MAGAZINE_SIZE = 1;
    private static final int MAX_AMMO = 10;
    private static final int DAMAGE = 4;
    private static final int BULLET_SPEED = 70;
    private static final Random random = new Random();
    private static final boolean IS_AUTOMATIC = false;

    private static final Texture weaponIcon = new Texture("ui/shotgun.png");
    private static final Sound reloadingSound = Gdx.audio.newSound(Gdx.files.internal("sound/reload.wav"));
    private static final Sound shootSound = Gdx.audio.newSound(Gdx.files.internal("sound/shotgunshootsound.wav"));

    private @Getter int magazine = MAGAZINE_SIZE;
    private @Getter int ammo = MAX_AMMO;
    private @Getter long reloadingTimer;
    private @Getter boolean reloading = false;

    public List<BulletData> fire() {
        List<BulletData> bullets = new ArrayList<>();
        long now = System.currentTimeMillis();
        if (!isAbleToShoot(reloading, magazine, ammo, reloadingTimer, RATE_OF_FIRE, now)) {
            return null;
        }
        var id = random.nextInt();
        for (int i = 0; i < 30; i++) {
            float bulletDeviation = (float) random.nextGaussian() * BULLET_SPREAD;
            bullets.add(new BulletData(BULLET_SPEED, bulletDeviation, DAMAGE, id));
        }
        magazine--;
        shootSound.play();
        reloadingTimer = now;
        return bullets;
    }

    public void reloadStart() {
        if (magazine < MAGAZINE_SIZE && ammo > 0 && !reloading) {
            reloading = true;
            reloadingSound.play();
            reloadingTimer = System.currentTimeMillis();
        }
    }

    public void reloadEnd() {
        int ammoToFillMagazine = Math.min(MAGAZINE_SIZE - magazine, ammo);
        ammo -= ammoToFillMagazine;
        magazine += ammoToFillMagazine;
        reloading = false;
    }

    public void isReloadingComplete(long now) {
        if (reloading && now - reloadingTimer > RELOADING_TIME) {
            reloadEnd();
        }
    }

    public boolean isAutomatic() {
        return IS_AUTOMATIC;
    }

    @Override
    public Texture getTexture() {
        return weaponIcon;
    }

    public void dispose() {
        reloadingSound.dispose();
        shootSound.dispose();
        weaponIcon.dispose();
    }

}
