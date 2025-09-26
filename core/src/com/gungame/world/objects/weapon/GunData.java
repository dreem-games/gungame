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
            1000,
            5,
            99,
            1,
            false,
            true,
            "rifle",
            SoundManager.Sfx.RIFLE_SHOT
    ),
    SHOTGUN(0.1f,
            5,
            1.5f,
            300,
            1,
            10,
            32,
            false,
            false,
            "shotgun",
            SoundManager.Sfx.SHOTGUN_SHOT
    ),
    SMG(0.035f,
            10,
            2,
            100,
            24,
            60,
            1,
            true,
            false,
            "smg",
            SoundManager.Sfx.SMG_SHOT
    );

    private final float bulletSpread;
    private final int bulletDamage;
    private final float bulletSpeed;
    private final int rateOfFire;
    private final int magazineSize;
    private final int maxAmmo;
    private final int bulletCountInOneShot;
    private final boolean isAutomatic;
    private final boolean hasHeavyBullets;
    private final float reloadingTime = 2700;

    private final String weaponName;
    private final SoundManager.Sfx shootSound;
    private final SoundManager.Sfx reloadingSound =  SoundManager.Sfx.RELOADING;
    private final SoundManager.Sfx emptyShotSound = SoundManager.Sfx.EMPTY_GUN_SHOT;
    private final float emptyShotTickTime = 500;


    public boolean hasHeavyBullets() {
        return hasHeavyBullets;
    }
}
