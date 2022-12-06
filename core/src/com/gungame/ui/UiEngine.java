package com.gungame.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.world.World;

import java.util.ArrayList;
import java.util.List;

public class UiEngine implements Disposable {

    private final List<Ui> uis;

    public UiEngine(boolean isDebugEnabled) {
        uis = new ArrayList<>();
        if (isDebugEnabled) {
            uis.add(new DebugUi());
        }
    }

    @Override
    public void dispose() {
        uis.forEach(Ui::dispose);
    }

    public void draw(SpriteBatch batch, Camera camera, World world) {
        uis.forEach(it -> it.draw(batch, camera, world));
    }
}
