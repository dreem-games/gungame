package com.gungame.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gungame.world.objects.phisical.Hero;

public class Ammo implements Ui {
    private final BitmapFont font = new BitmapFont();

    public Ammo(boolean isMainHero) {
        this.isMainHero = isMainHero;
    }

    private final boolean isMainHero;

    @Override
    public void draw(SpriteBatch batch, Camera camera, Hero hero) {
        float x;
        float y;
        float xIcon;
        float width;
        if (isMainHero) {
            x = camera.position.x - camera.viewportWidth / 2 + 3;
            xIcon = x + 1f;
            width = 12f;
        } else {
            x = camera.position.x - camera.viewportWidth / 2 + 78;
            xIcon = x + 14f;
            width = -12f;
        }
        y = camera.position.y + camera.viewportHeight / 2 - 3;
        font.getData().setScale(0.2f);
        font.draw(batch, String.format((" %d  /  %d "),
                hero.getCurrentGun().getMagazine(), hero.getCurrentGun().getAmmo()), x, y);
        batch.draw(hero.getCurrentGun().getTexture(), xIcon, y - 9.5f, width, 12f);

    }

    @Override
    public void dispose() {
        font.dispose();
    }
}
