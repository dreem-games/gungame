package com.gungame.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.world.GameWorld;

import java.util.ArrayList;
import java.util.List;

public class UiEngine implements Disposable {

    private final List<Ui> uis;

    public UiEngine() {
        uis = new ArrayList<>();
    }

    @Override
    public void dispose() {
        uis.forEach(Ui::dispose);
    }

    public void draw(SpriteBatch batch, Camera camera, GameWorld gameWorld) {
        uis.forEach(it -> it.draw(batch, camera, gameWorld));
    }
}
