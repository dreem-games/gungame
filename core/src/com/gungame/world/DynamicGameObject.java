package com.gungame.world;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.Body;

public class DynamicGameObject extends GameObject {

    private boolean isActive;  // становится true при первом взаимодействии, например, передвижении

    public DynamicGameObject(GameObjectType type, Body body, Sprite sprite, Object parent) {
        super(type, body, sprite, parent);
        isActive = false;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void activate() {
        isActive = true;
    }
}
