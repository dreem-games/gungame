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
        SHOTGUN_SHOT(Collections.singletonList("sound/shotgun_shot_sound.wav")),
        SMG_SHOT(Collections.singletonList("sound/smg_shot_sound.wav")),
        RIFLE_SHOT(Collections.singletonList("sound/rifle_shot_sound.wav")),
        DAMAGE(Arrays.asList("sound/dash_1.wav", "sound/dash_2.wav")),
        DEATH(Collections.singletonList("sound/death.wav")),
        DASH(Arrays.asList("sound/damage_1.wav", "sound/damage_2.wav")),
        EXPLOSION(Collections.singletonList("sound/barrel_explosion.wav")),
        RELOADING(Collections.singletonList("sound/reload.wav")),
        EMPTY_GUN_SHOT(Collections.singletonList("sound/empty_gun_shot.wav"));

        private final List<String> path;
    }

    private static final AssetManager am = new AssetManager();

    public static void loadAll() {
        Arrays.stream(Sfx.values())
                .flatMap(sfx -> sfx.path.stream())
                .forEach(path -> am.load(path, Sound.class));
        finisAll();
    }

    public static void finisAll() {
        am.finishLoading();
    }

    /**
     * Выполнение следующего действия фоновой загрузки
     *
     * @return true если загрузка завершена
     */
    public static boolean update() {
        return am.update();
    }

    /**
     * @return процент выполнения фоновой загрузки звуков
     */
    public static float progress() {
        return am.getProgress();
    }

    /**
     * Воспроизведение следующего звука указанного типа
     */
    public static void play(Sfx sfx) {
        List<String> list = sfx.getPath();
        int idx = list.size() == 1 ? 0 : MathUtils.random(list.size() - 1);
        String path = list.get(idx);

        Sound sound = am.get(path, Sound.class);
        sound.play(1f);
    }

    public static void dispose() {
        am.dispose();
    }
}
