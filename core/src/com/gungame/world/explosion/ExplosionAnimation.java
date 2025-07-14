package com.gungame.world.explosion;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class ExplosionAnimation {
    public static Animation<TextureRegion> explosionAnimation;

    public static void init() {
        final Texture explosionSheet = new Texture("texture/explosion.png");
        TextureRegion[][] tmp = TextureRegion.split(explosionSheet, 64, 64);
        TextureRegion[] explosionFrames = new TextureRegion[25];
        int index = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                explosionFrames[index++] = tmp[i][j];
            }
        }
        explosionAnimation = new Animation<>(0.8f, explosionFrames);
    }
}
