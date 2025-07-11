package com.gungame.world.objects.weapon;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Gun {
    private final float bulletSpread;
    private final int reloadingTime;
    private final int rateOfFire;
    private final int magazineSize;
    private final int maxAmmo;
    private final int damage;
    private final int bulletSpeed;
    private final Random random = new Random();
    private final boolean isAutomatic;
    private final int bulletsCountInOneShot;//Автоматическое оружие или нет

    private final Texture weaponIcon;
    private final Sound reloadingSound;
    private final Sound shootSound;

    private @Getter int magazine;
    private int ammo;
    private @Getter long reloadingTimer;
    private boolean reloading = false;

    public Gun(GunData gunData) {
        this.bulletSpread = gunData.getBulletSpread();
        this.reloadingTime = gunData.getReloadingTime();
        this.rateOfFire = gunData.getRateOfFire();
        this.magazineSize = gunData.getMagazineSize();
        this.maxAmmo = gunData.getMaxAmmo();
        this.damage = gunData.getBulletDamage();
        this.bulletSpeed = gunData.getBulletSpeed();
        this.isAutomatic = gunData.isAutomatic();
        this.bulletsCountInOneShot = gunData.getBulletCountInOneShot();
        this.weaponIcon = gunData.getWeaponIcon();
        this.reloadingSound = gunData.getReloadingSound();
        this.shootSound = gunData.getShootSound();
        magazine = magazineSize;
        ammo = maxAmmo;
    }

    public List<BulletData> fire() {
        List<BulletData> bullets = new ArrayList<>();
        if (reloading) {
            return null;
        }
        if (magazine == 0) {
            if (ammo > 0) {
                reloadStart();
            }
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - reloadingTimer < rateOfFire) {
            return null;
        }
        int id = random.nextInt(Short.MIN_VALUE, -1); //Создаем id выстрела
        for (int i = 0; i < bulletsCountInOneShot; i++) {
            float bulletDeviation = (float) random.nextGaussian() * bulletSpread;
            bullets.add(new BulletData(bulletSpeed, bulletDeviation, damage, (short) id));
        }
        magazine--;
        shootSound.play();
        reloadingTimer = now;
        return bullets;
    }

    public void reloadStart() {
        if (magazine < magazineSize && ammo > 0 && !reloading) {
            reloading = true;
            reloadingSound.play();
            reloadingTimer = System.currentTimeMillis();
        }
    }

    public void reloadEnd() {
        int ammoToFillMagazine = Math.min(magazineSize - magazine, ammo);
        ammo -= ammoToFillMagazine;
        magazine += ammoToFillMagazine;
        reloading = false;
    }

    public void isReloadingComplete(long now) {
        if (reloading && now - reloadingTimer > reloadingTime) {
            reloadEnd();
        }
    }

    public int getMagazineSize() {
        return magazine;
    }

    public boolean isAutomatic() {
        return isAutomatic;
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

    public Texture getTexture() {
        return weaponIcon;
    }
}
