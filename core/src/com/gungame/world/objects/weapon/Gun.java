package com.gungame.world.objects.weapon;

import com.badlogic.gdx.graphics.Texture;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.math.MathUtils.random;
import static com.gungame.world.GameWorldConfig.ENABLE_AUTO_RELOADING;

public class Gun {
    private final GunData gunData;

    private @Getter int magazine;
    private @Getter int ammo;
    private @Getter long reloadingTimer;
    private @Getter boolean reloading = false;

    public Gun(GunData gunData) {
        this.gunData = gunData;
        this.magazine = gunData.getMagazineSize();
        this.ammo = gunData.getMaxAmmo();
    }

    public List<BulletData> fire() {
        List<BulletData> bullets = new ArrayList<>();
        if (reloading) {
            return null;
        }
        if (magazine == 0) {
            if (ammo > 0 && ENABLE_AUTO_RELOADING) {
                reloadStart();
            }
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - reloadingTimer < gunData.getRateOfFire()) {
            return null;
        }
        int id = random.nextInt(Short.MIN_VALUE, -1); //Создаем id выстрела
        for (int i = 0; i < gunData.getBulletCountInOneShot(); i++) {
            float bulletDeviation = (float) random.nextGaussian() * gunData.getBulletSpread();
            bullets.add(new BulletData(gunData.getBulletSpeed(), bulletDeviation, gunData.getBulletDamage(), (short) id));
        }
        magazine--;
        gunData.getShootSound().play();
        reloadingTimer = now;
        return bullets;
    }

    public void reloadStart() {
        if (magazine < gunData.getMagazineSize() && ammo > 0 && !reloading) {
            reloading = true;
            gunData.getReloadingSound().play();
            reloadingTimer = System.currentTimeMillis();
        }
    }

    public void reloadEnd() {
        int ammoToFillMagazine = Math.min(gunData.getMagazineSize() - magazine, ammo);
        ammo -= ammoToFillMagazine;
        magazine += ammoToFillMagazine;
        reloading = false;
    }

    public void checkReloadingComplete(long now) {
        if (reloading && now - reloadingTimer > gunData.getReloadingTime()) {
            reloadEnd();
        }
    }

    public boolean isAutomatic() {
        return gunData.isAutomatic();
    }

    public void dispose() {
        gunData.dispose();
    }

    public Texture getTexture() {
        return gunData.getWeaponIcon();
    }

    public boolean hasHeavyBullets() {
        return gunData.hasHeavyBullets();
    }
}
