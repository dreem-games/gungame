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
    RIFLE(0.035f,
            35,
            170,
            3000,
            1000,
            9,
            99,
            1,
            false,
            new Texture("ui/rifle.png"),
            Gdx.audio.newSound(Gdx.files.internal("sound/reload.wav")),
            Gdx.audio.newSound(Gdx.files.internal("sound/rifleshotsound.wav"))
            ),
    SHOTGUN(0.15f,
            4,
            50,
            3000,  // TODO: вероятно, надо ускорить перезарядку
            300,
            1,
            10,
            30,
            false,
            new Texture("ui/shotgun.png"),
            Gdx.audio.newSound(Gdx.files.internal("sound/reload.wav")),
            Gdx.audio.newSound(Gdx.files.internal("sound/shotgunshootsound.wav"))
            ),
    SMG(0.01f,
            4,
            75,
            3000,
            100,
            30,
            60,
            1,
            true,
            new Texture("ui/SMG.png"),
            Gdx.audio.newSound(Gdx.files.internal("sound/reload.wav")),
            Gdx.audio.newSound(Gdx.files.internal("sound/smgshootsound.wav"))
    );

    private final float bulletSpread;
    private final int bulletDamage;
    private final int bulletSpeed;
    private final int reloadingTime;
    private final int rateOfFire;
    private final int magazineSize;
    private final int maxAmmo;
    private final int bulletCountInOneShot;
    private final boolean isAutomatic;

    private final Texture weaponIcon;
    private final Sound reloadingSound;
    private final Sound shootSound;


    @Override
    public void dispose() {
        weaponIcon.dispose();
        reloadingSound.dispose();
        shootSound.dispose();
    }
}
