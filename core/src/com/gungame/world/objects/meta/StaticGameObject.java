package com.gungame.world.objects.meta;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;

public class StaticGameObject extends VisibleGameObject {

    public StaticGameObject(GameObjectType type, Body body, Sprite sprite) {
        super(type, body, sprite);
        if (body.getType() != BodyDef.BodyType.StaticBody) {
            throw new IllegalStateException("StaticGameObject accepts only static bodies!");
        }
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void activate() {
        throw new IllegalStateException("Static object activated!");
    }
}
