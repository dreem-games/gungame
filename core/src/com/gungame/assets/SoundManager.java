package com.gungame.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SoundManager {
    private SoundManager() {}

    @Getter
    @RequiredArgsConstructor
    public enum Sfx {
        SHOTGUN_SHOT(Collections.singletonList("assets/sound/shotgun_shot_sound.wav")),
        SMG_SHOT(Collections.singletonList("assets/sound/smg_shot_sound.wav")),
        RIFLE_SHOT(Collections.singletonList("assets/sound/rifle_shot_sound.wav")),
        DAMAGE(Arrays.asList("assets/sound/dash_1.wav", "assets/sound/dash_2.wav")),
        DEATH(Collections.singletonList("assets/sound/death.wav")),
        DASH(Arrays.asList("assets/sound/damage_1.wav", "assets/sound/damage_2.wav")),
        EXPLOSION(Collections.singletonList("assets/sound/barrel_explosion.wav")),
        RELOADING(Collections.singletonList("assets/sound/reload.wav")),
        EMPTY_GUN_SHOT(Collections.singletonList("assets/sound/empty_gun_shot.wav"));
        private final List<String> path;
    }

    private static final AssetManager am = new AssetManager();

    public static void loadAll() {
        for (List<String> list : Arrays.stream(Sfx.values()).map(Sfx::getPath).toList()) {
            for (String p : list){
                am.load(p, Sound.class);
            }
        }
        finisAll();
    }

    public static void finisAll() {
        am.finishLoading();
    }

    public static boolean update() { //методы для экрана загрузки
        return am.update(); }

    public static float progress() {
        return am.getProgress(); }

    public static long play(Sfx sfx) {
        List<String> list = sfx.getPath();
        if (list == null || list.isEmpty()) {
            return -1;
        }

        int idx = list.size() == 1 ? 0 : MathUtils.random(list.size() - 1);
        String path = list.get(idx);

        Sound sound = am.get(path, Sound.class);

        return sound.play(1f);
    }

    public static void dispose() {
        am.dispose();
    }
}
