package com.gungame.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.world.GameWorld;

public interface Ui extends Disposable {

    void draw(SpriteBatch batch, Camera camera, GameWorld gameWorld);
}
