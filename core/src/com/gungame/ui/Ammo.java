package com.gungame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
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

    public Ammo(boolean isMainHero) {
        this.isMainHero = isMainHero;
    }

    private final boolean isMainHero;

    @Override
    public void draw(SpriteBatch batch, Camera camera, Hero hero) {
        float x;
        float y;
        float xIcon;
        float yIcon;
        float width;

        if (isMainHero) {
            x = camera.position.x - camera.viewportWidth * 0.37f;
            xIcon = x - camera.viewportWidth * 0.1f;
            width = camera.viewportWidth * 0.1f;
        } else {
            x = camera.position.x + camera.viewportWidth * 0.42f;
            xIcon = x - camera.viewportWidth * 0.1f;
            width = camera.viewportWidth * 0.1f;
        }
        y = camera.position.y + camera.viewportHeight * 0.45f;
        yIcon = y - camera.viewportHeight * 0.05f;
        font.draw(batch, String.format(("%d /  %d"),
                hero.getCurrentGun().getMagazine(), hero.getCurrentGun().getAmmo()), x, y);
        batch.draw(hero.getCurrentGun().getTexture(), xIcon, yIcon, width, camera.viewportHeight * 0.1f);
    }

    @Override
    public void dispose() {
        font.dispose();
    }
}
