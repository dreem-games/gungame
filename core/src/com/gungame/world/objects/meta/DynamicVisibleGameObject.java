package com.gungame.world.objects.meta;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.Body;

public class DynamicVisibleGameObject extends VisibleGameObject {

    private boolean isActive;  // становится true при первом взаимодействии, например, передвижении

    public DynamicVisibleGameObject(GameObjectType type, Body body, Sprite sprite) {
        super(type, body, sprite);
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

    @Override
    protected void onPositionChanged() {
        super.onPositionChanged();
        isActive = true;
    }
}
