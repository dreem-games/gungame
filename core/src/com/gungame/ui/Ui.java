package com.gungame.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.world.World;

public interface Ui extends Disposable {

    void draw(SpriteBatch batch, Camera camera, World world);
}
