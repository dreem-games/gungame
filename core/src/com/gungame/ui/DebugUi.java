package com.gungame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector3;
import com.gungame.world.World;

public class DebugUi implements Ui {

    private final BitmapFont font;
    private final Texture cursorTexture;
    private final Sprite cursor;

    public DebugUi() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font/FD.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 14; // font size
        font = generator.generateFont(parameter);

        cursorTexture = new Texture("texture/cursor.png");
        cursor = new Sprite(cursorTexture);
        cursor.setSize(40, 30);
    }

    @Override
    public void dispose() {
        font.dispose();
        cursorTexture.dispose();
    }

    @Override
    public void draw(SpriteBatch batch, Camera camera, World world) {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        dawCursorPos(batch, mousePos, world);
        drawCursor(batch, mousePos);
    }

    private void dawCursorPos(SpriteBatch batch, Vector3 mousePos, World world) {
        String overlapsText = world.getOverlapsWall(mousePos.x, mousePos.y)
                .map(floats -> " (wall x=" + floats[0] + " y=" + floats[1] + ")").orElse("");
        font.draw(batch, "x=" + (int) mousePos.x + " y=" + (int) mousePos.y + overlapsText, 10, 20);
    }

    private void drawCursor(SpriteBatch spriteBatch, Vector3 mousePos) {
        cursor.setPosition(mousePos.x - 11, mousePos.y - 29);
        cursor.draw(spriteBatch);
    }
}
