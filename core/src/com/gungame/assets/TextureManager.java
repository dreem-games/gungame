package com.gungame.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class TextureManager {
    private TextureManager() {}

    public static final AssetManager am = new AssetManager();

    public static Animation<TextureRegion> explosionAnimation;


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
        finishAll();
    }

    public static void initLvlOne() {
        am.load("assets/texture/level1.atlas", TextureAtlas.class);
        am.load("assets/texture/explosion.atlas", TextureAtlas.class);
        finishAll();
        initExplosionAnimation();
    }

    public static void initUi() {
        am.load("assets/ui/guns.atlas", TextureAtlas.class);
        am.load("assets/ui/target.atlas", TextureAtlas.class);
        finishAll();
    }

    public static void initExplosionAnimation() {
        TextureAtlas atlas = TextureManager.am.get("assets/texture/explosion.atlas", TextureAtlas.class);
        Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions("explosion");
        explosionAnimation = new Animation<>(0.4f, frames, Animation.PlayMode.NORMAL);
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

    public static boolean update() { return am.update(); }
    public static float progress() { return am.getProgress(); }
    public static void finishAll() { am.finishLoading(); }

    public static void unload(String file) {
        if (am.isLoaded(file)) {
            am.unload(file);
        }
    }

    public static void dispose() {
        am.dispose();
    }
}
