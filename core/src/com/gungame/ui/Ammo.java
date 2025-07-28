package com.gungame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.gungame.world.explosion.AssetManager;
import com.gungame.world.objects.phisical.Hero;

public class Ammo implements Ui {
    private final BitmapFont font;
    {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font/orbitron.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
        params.size = 24;
        params.color = Color.WHITE;
        params.minFilter = Texture.TextureFilter.Linear;
        params.magFilter = Texture.TextureFilter.Nearest;

        font = generator.generateFont(params);
        generator.dispose();
    }
    private float lastDisplayPixels = 2073600f;

    public Ammo(boolean isMainHero) {
        this.isMainHero = isMainHero;
    }

    private final boolean isMainHero;

    @Override
    public void draw(SpriteBatch batch, Camera camera, Hero hero) {
        float pixels = camera.viewportHeight * camera.viewportWidth;
        if (pixels != lastDisplayPixels) {
            font.getData().scale(pixels / lastDisplayPixels / 10f);
            lastDisplayPixels = pixels;
        }

        float x;
        float y;
        float xIcon;
        float yIcon;
        float width;
        float xGrenade;
        float yGrenade;

        if (isMainHero) {
            x = camera.position.x - camera.viewportWidth * 0.37f;
            xIcon = x - camera.viewportWidth * 0.1f;
            width = camera.viewportWidth * 0.1f;
            xGrenade = camera.viewportWidth * 0.06f;

        } else {
            x = camera.position.x + camera.viewportWidth * 0.42f;
            xIcon = x - camera.viewportWidth * 0.1f;
            width = camera.viewportWidth * 0.1f;
            xGrenade = camera.viewportWidth * 0.93f;
        }
        y = camera.position.y + camera.viewportHeight * 0.45f;
        yIcon = y - camera.viewportHeight * 0.05f;
        yGrenade = y - camera.viewportHeight * 0.07f;
        font.draw(batch, String.format(("%d /  %d"),
                hero.getCurrentGun().getMagazine(), hero.getCurrentGun().getAmmo()), x, y);
        batch.draw(hero.getCurrentGun().getTexture(), xIcon, yIcon, width, camera.viewportHeight * 0.1f);

        font.draw(batch, String.format((" - %d"), hero.getGrenadeThrower().ammo), xGrenade, yGrenade + camera.viewportHeight * 0.03f);
        batch.draw(AssetManager.grenadeTexture, xGrenade - camera.viewportWidth * 0.02f, yGrenade, width / 4, width / 4);
    }

    @Override
    public void dispose() {
        font.dispose();
    }
}
