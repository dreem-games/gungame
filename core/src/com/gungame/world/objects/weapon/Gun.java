package com.gungame.world.objects.weapon;

import com.badlogic.gdx.graphics.Texture;

import java.util.List;

public interface Gun {
    List<BulletData> fire();
    void reloadStart();
    void reloadEnd();
    void isReloadingComplete(long now);
    int getAmmo();
    int getMagazine();
    boolean isAutomatic();
    boolean isReloading();
    Texture getTexture();

    default boolean isAbleToShoot(boolean reloading, int magazine, int ammo, long reloadingTimer, int rateOfFire, long now) {
        if (reloading) {
            return false;
        }
        if (magazine == 0) {
            if (ammo > 0) {
                reloadStart();
            }
            return false;
        }
        return now - reloadingTimer >= rateOfFire;
    }

}
