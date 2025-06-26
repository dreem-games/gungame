package com.gungame.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gungame.world.objects.phisical.Hero;

public class Ammo implements  Ui {

    BitmapFont font = new BitmapFont();

    @Override
    public void draw(SpriteBatch batch, Camera camera, Hero hero) {
        float x = camera.position.x - camera.viewportWidth / 2;
        float y = camera.position.y + camera.viewportHeight / 2 - 2;
        font.getData().setScale(0.2f);
        font.draw(batch, String.format((" %d  /  %d "), hero.getMagazine(), hero.getAmmo()), x, y);
    }

    @Override
    public void dispose() {
        font.dispose();
    }
}
