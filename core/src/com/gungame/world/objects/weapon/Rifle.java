package com.gungame.world.objects.weapon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Rifle implements Gun {
    private static final float BULLET_SPREAD = 0.035f;
    private static final int RELOADING_TIME = 3000;
    private static final int RATE_OF_FIRE = 1000;
    private static final int MAGAZINE_SIZE = 9;
    private static final int MAX_AMMO = 99;
    private static final int DAMAGE = 20;
    private static final int BULLET_SPEED = 70;
    private static final Random random = new Random();
    private static final boolean IS_AUTOMATIC = false;    //Автоматическое оружие или нет

    private static final Texture weaponIcon = new Texture("ui/rifle.png");
    private static final Sound reloadingSound = Gdx.audio.newSound(Gdx.files.internal("sound/reload.wav"));
    private static final Sound shootSound = Gdx.audio.newSound(Gdx.files.internal("sound/rifleshotsound.wav"));

    private @Getter int magazine = MAGAZINE_SIZE;
    private int ammo = MAX_AMMO;
    private @Getter long reloadingTimer;
    private boolean reloading = false;


    public List<BulletData> fire() {
        long now = System.currentTimeMillis();
        List<BulletData> bullets = new ArrayList<>();
        if (!isAbleToShoot(reloading, magazine, ammo, reloadingTimer, RATE_OF_FIRE, now)) {
            return null;
        }
        var id = random.nextInt(); //Создаем id выстрела
        float bulletDeviation = (float) random.nextGaussian() * BULLET_SPREAD;
        bullets.add(new BulletData(BULLET_SPEED, bulletDeviation, DAMAGE, id));
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

    public int getMagazineSize() {
        return magazine;
    }

    public boolean isAutomatic() {
        return IS_AUTOMATIC;
    }

    public int getAmmo() {
        return ammo;
    }

    public boolean isReloading() {
        return reloading;
    }

    public void dispose() {
        weaponIcon.dispose();
        reloadingSound.dispose();
        shootSound.dispose();
    }

    @Override
    public Texture getTexture() {
        return weaponIcon;
    }
}
