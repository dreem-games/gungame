package com.gungame.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public class SoundManager {
    private SoundManager() {}

    public enum Sfx {
        SHOTGUN_SHOT,
        SMG_SHOT,
        RIFLE_SHOT,
        DAMAGE,
        DEATH,
        DASH,
        EXPLOSION,
        RELOADING
    }

    private static final AssetManager am = new AssetManager();
    private static final EnumMap<Sfx, List<String>> paths = new EnumMap<>(Sfx.class);

    static {
        // укажи пути без "assets/"
        paths.put(Sfx.SHOTGUN_SHOT, Collections.singletonList("assets/sound/shotgunshootsound.wav"));
        paths.put(Sfx.RIFLE_SHOT, Collections.singletonList("assets/sound/rifleshotsound.wav"));
        paths.put(Sfx.SMG_SHOT, Collections.singletonList("assets/sound/smgshotsound.wav"));
        paths.put(Sfx.EXPLOSION,   Collections.singletonList("assets/sound/barrelExplosion.wav"));
        paths.put(Sfx.RELOADING,    Collections.singletonList("assets/sound/reload.wav"));
        paths.put(Sfx.DEATH,        Collections.singletonList("assets/sound/death.wav"));
        paths.put(Sfx.DAMAGE,        Arrays.asList(
                "assets/sound/dash1.wav",
                "assets/sound/dash2.wav"
        ));
        paths.put(Sfx.DASH,        Arrays.asList(
                "assets/sound/damage1.wav",
                "assets/sound/damage2.wav"
        ));
    }

    public static void loadAll() {
        for (List<String> list : paths.values()) {
            for (String p : list) am.load(p, Sound.class);
        }
        finishLoading();
    }

    public static void finishLoading() {
        am.finishLoading();
    }

    public static long play(Sfx sfx) {
        List<String> list = paths.get(sfx);
        if (list == null || list.isEmpty()) return -1;

        int idx = list.size() == 1 ? 0 : MathUtils.random(list.size() - 1);
        String path = list.get(idx);

        Sound sound = am.get(path, Sound.class);
        return sound.play(1f);
    }

    public static void dispose() {
        am.dispose();
    }
}
