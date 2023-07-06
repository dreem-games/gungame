package com.gungame.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gungame.world.objects.phisical.Hero;

class HeroStaminaBar implements Ui {
    private final Texture staminaTexture;
    private final Texture staminaBackgroundTexture;

    HeroStaminaBar() {
        // Создаем простую текстуру с определенным цветом
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.GREEN); // Задаем цвет прямоугольника
        pixmap.fill(); // Заливаем прямоугольник цветом
        staminaTexture = new Texture(pixmap);
        pixmap.dispose(); // Освобождаем ресурсы пиксельной карты

        pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK); // Задаем цвет прямоугольника
        pixmap.fill(); // Заливаем прямоугольник цветом
        staminaBackgroundTexture = new Texture(pixmap);
        pixmap.dispose(); // Освобождаем ресурсы пиксельной карты
    }

    @Override
    public void draw(SpriteBatch batch, Camera camera, Hero hero) {
        float width = camera.viewportWidth / 6;
        float height = camera.viewportHeight / 100;

        float x = camera.position.x - camera.viewportWidth / 2;
        float y = camera.position.y + camera.viewportHeight / 2 - height * 2;

        batch.draw(staminaBackgroundTexture, x, y, width, height);
        batch.draw(staminaTexture, x, y, width * hero.getStamina() / hero.getMaxStamina(), height);
    }

    @Override
    public void dispose() {
        staminaTexture.dispose();
        staminaBackgroundTexture.dispose();
    }
}
