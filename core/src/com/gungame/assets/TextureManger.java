package com.gungame.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class TextureManger {
    private TextureManger() {}

    public static final AssetManager am = new AssetManager();

    public static Texture getOrLoadTexture(String path) {
        if (!am.isLoaded(path)) {
            am.load(path, Texture.class);
            am.finishLoadingAsset(path); // блокирующая загрузка ТОЛЬКО этого ассета
        }
        return am.get(path, Texture.class);
    }

    public static void initHeroAndProjectiles() {
        am.load("assets/texture/projectiles.atlas", TextureAtlas.class);
        am.load("assets/texture/hero.atlas", TextureAtlas.class);
    }

    public static void initLvlOne() {
        am.load("assets/texture/level1.atlas", TextureAtlas.class);
    }

    public static TextureRegion getRegion(String atlasPath, String regionName) {
        if (!am.isLoaded(atlasPath)) {
            throw new IllegalStateException("Atlas is not loaded " + atlasPath);
        }
        TextureAtlas atlas = am.get(atlasPath, TextureAtlas.class);
        TextureRegion region = atlas.findRegion(regionName);
        if (region == null) {
            throw new IllegalArgumentException("Region '" + regionName + "' not found in region:  " + atlasPath);
        }
        return region;
    }

    public static boolean update() { return am.update(); }  // зови каждый кадр на экране загрузки
    public static float progress() { return am.getProgress(); }
    public static void finishAll() { am.finishLoading(); }  // блокирующая — вся очередь

    public static void unload(String file) {
        if (am.isLoaded(file)) am.unload(file);
    }

    public static void dispose() { am.dispose(); }
}
