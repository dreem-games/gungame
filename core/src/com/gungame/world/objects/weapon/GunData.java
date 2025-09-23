package com.gungame.world.objects.weapon;

import com.gungame.assets.SoundManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GunData {
    RIFLE(0.01f,
            55,
            4,
            3000,
            1000,
            5,
            99,
            1,
            false,
            true,
            "rifle",
            SoundManager.Sfx.RELOADING,
            SoundManager.Sfx.RIFLE_SHOT
    ),
    SHOTGUN(0.1f,
            5,
            1.5f,
            3000,  // TODO: вероятно, надо ускорить перезарядку
            300,
            1,
            10,
            32,
            false,
            false,
            "shotgun",
            SoundManager.Sfx.RELOADING,
            SoundManager.Sfx.SHOTGUN_SHOT
    ),
    SMG(0.035f,
            10,
            2,
            3000,
            100,
            24,
            60,
            1,
            true,
            false,
            "smg",
            SoundManager.Sfx.RELOADING,
            SoundManager.Sfx.SMG_SHOT
    );

    private final float bulletSpread;
    private final int bulletDamage;
    private final float bulletSpeed;
    private final int reloadingTime;
    private final int rateOfFire;
    private final int magazineSize;
    private final int maxAmmo;
    private final int bulletCountInOneShot;
    private final boolean isAutomatic;
    private final boolean hasHeavyBullets;

    private final String weaponName;
    private final SoundManager.Sfx reloadingSound;
    private final SoundManager.Sfx shootSound;

    public boolean hasHeavyBullets() {
        return hasHeavyBullets;
    }
}
