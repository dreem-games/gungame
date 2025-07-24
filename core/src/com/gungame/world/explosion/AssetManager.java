package com.gungame.world.explosion;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import lombok.Getter;

public class AssetManager {
    public static Animation<TextureRegion> explosionAnimation;
    public @Getter static TextureRegion[] targetTextures = new TextureRegion[3];
    public static @Getter Texture grenadeTexture = new Texture("texture/grenade.png");

    public static void init() {
        Texture sheet = new Texture("texture/explosion.png");
        TextureRegion[][] tmp = TextureRegion.split(sheet, 64, 64);
        TextureRegion[] explosionFrames = new TextureRegion[25];
        int index = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                explosionFrames[index++] = tmp[i][j];
            }
        }
        explosionAnimation = new Animation<>(0.8f, explosionFrames);

        sheet = new Texture("ui/target.png");
        int w = sheet.getWidth() / 3;
        int h = sheet.getHeight();
        for (int i = 0; i < 3; i++) {
            targetTextures[i] = new TextureRegion(sheet, i * w, 0, w, h);
        }
    }

    public void dispose() {
        grenadeTexture.dispose();
    }
}
