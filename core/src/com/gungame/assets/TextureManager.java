package com.gungame.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class TextureManager {
    private TextureManager() {}

    @Getter
    @RequiredArgsConstructor
    public enum AtlasType {
        HERO("assets/texture/hero.atlas"),
        PROJECTILES("assets/texture/projectiles.atlas"),
        EXPLOSION("assets/texture/explosion.atlas"),
        TARGET("assets/ui/target.atlas"),
        GUNS("assets/ui/guns.atlas"),
        LEVEL_1("assets/texture/level1.atlas");
        private final String path;
    }

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
        am.load(AtlasType.PROJECTILES.path, TextureAtlas.class);
        am.load(AtlasType.HERO.path, TextureAtlas.class);
        finishAll();
    }

    public static void initLvlOne() {
        am.load(AtlasType.LEVEL_1.path, TextureAtlas.class);
        am.load(AtlasType.EXPLOSION.path, TextureAtlas.class);
        finishAll();
        initExplosionAnimation();
    }

    public static void initUi() {
        am.load(AtlasType.GUNS.path, TextureAtlas.class);
        am.load(AtlasType.TARGET.path, TextureAtlas.class);
        finishAll();
    }

    public static void initExplosionAnimation() {
        TextureAtlas atlas = TextureManager.am.get("assets/texture/explosion.atlas", TextureAtlas.class);
        Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions("explosion");
        explosionAnimation = new Animation<>(0.4f, frames, Animation.PlayMode.NORMAL);
    }


    public static TextureRegion getRegion(AtlasType atlasType, String regionName) {
        if (!am.isLoaded(atlasType.path)) {
            throw new IllegalStateException("Atlas is not loaded " + atlasType.path);
        }
        TextureAtlas atlas = am.get(atlasType.path, TextureAtlas.class);
        TextureRegion region = atlas.findRegion(regionName);
        if (region == null) {
            throw new IllegalArgumentException("Region '" + regionName + "' not found in region:  " + atlasType.path);
        }
        return region;
    }

    public static void finishAll() {
        am.finishLoading(); }

    public static boolean update() { //методы для экрана загрузки
        return am.update(); }

    public static float progress() {
        return am.getProgress(); }

    public static void unload(String file) {
        if (am.isLoaded(file)) {
            am.unload(file);
        }
    }

    public static void dispose() {
        am.dispose();
    }
}
