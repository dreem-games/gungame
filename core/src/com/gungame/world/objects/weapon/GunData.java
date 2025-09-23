package com.gungame.world.objects.weapon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GunData implements Disposable {
    RIFLE(0.01f,
            55,
            4,
            1000,
            5,
            99,
            1,
            false,
            true,
            new Texture("ui/rifle.png"),
            Gdx.audio.newSound(Gdx.files.internal("sound/rifleshotsound.wav"))),
    SHOTGUN(0.1f,
            5,
            1.5f,
            300,
            1,
            10,
            32,
            false,
            false,
            new Texture("ui/shotgun.png"),
            Gdx.audio.newSound(Gdx.files.internal("sound/shotgunshootsound.wav"))),
    SMG(0.035f,
            10,
            2,
            100,
            24,
            60,
            1,
            true,
            false,
            new Texture("ui/SMG.png"),
            Gdx.audio.newSound(Gdx.files.internal("sound/smgshootsound.wav")));

    private final float bulletSpread;
    private final int bulletDamage;
    private final float bulletSpeed;
    private final int rateOfFire;
    private final int magazineSize;
    private final int maxAmmo;
    private final int bulletCountInOneShot;
    private final boolean isAutomatic;
    private final boolean hasHeavyBullets;

    private final Texture weaponIcon;
    private final Sound shootSound;

    private final float reloadingTime = 2700;
    private final Sound reloadingSound = Gdx.audio.newSound(Gdx.files.internal("sound/reload.wav"));

    private final float emptyShotTickTime = 500;
    private final Sound emptyShotSound = Gdx.audio.newSound(Gdx.files.internal("sound/emptyGunShot.wav"));

    @Override
    public void dispose() {
        weaponIcon.dispose();
        reloadingSound.dispose();
        shootSound.dispose();
        emptyShotSound.dispose();
    }

    public boolean hasHeavyBullets() {
        return hasHeavyBullets;
    }
}
