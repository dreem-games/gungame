package com.gungame.world.objects.weapon;

import com.gungame.assets.SoundManager;
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
    private @Getter long emptyShotTickTimer;
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

        long now = System.currentTimeMillis();
        if (magazine == 0) {
            if (ammo > 0 && ENABLE_AUTO_RELOADING) {
                reloadStart();
            }

            if (now - emptyShotTickTimer > gunData.getEmptyShotTickTime()) {
                gunData.getEmptyShotSound().play();
                emptyShotTickTimer = now;
            }
            return null;
        }
        if (now - reloadingTimer < gunData.getRateOfFire()) {
            return null;
        }
        int id = random.nextInt(Short.MIN_VALUE, -1); //Создаем id выстрела
        for (int i = 0; i < gunData.getBulletCountInOneShot(); i++) {
            float bulletDeviation = (float) random.nextGaussian() * gunData.getBulletSpread();
            bullets.add(new BulletData(gunData.getBulletSpeed(), bulletDeviation, gunData.getBulletDamage(), (short) id));
        }
        magazine--;
        SoundManager.play(gunData.getShootSound());
        reloadingTimer = now;
        return bullets;
    }

    public void reloadStart() {
        if (magazine < gunData.getMagazineSize() && ammo > 0 && !reloading) {
            reloading = true;
            SoundManager.play(gunData.getReloadingSound());
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

    public String getWeaponName() {
        return gunData.getWeaponName();
    }

    public boolean hasHeavyBullets() {
        return gunData.hasHeavyBullets();
    }
}
