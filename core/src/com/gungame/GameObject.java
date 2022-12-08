package com.gungame;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.Body;

public class GameObject {
    private final GameObjectType type;
    private final Body body;
    private final Sprite sprite;
    private final Object parent;
    private boolean toDestroy = false;

    public GameObject(GameObjectType type, Body body, Sprite sprite, Object parent) {
        this.type = type;
        this.body = body;
        this.sprite = sprite;
        this.parent = parent;
    }

    public GameObjectType getType() {
        return type;
    }

    public Body getBody() {
        return body;
    }

    public Sprite getSprite() {
        return sprite;
    }

    public Object getParent() {
        return parent;
    }

    public boolean isToDestroy() {
        return toDestroy;
    }

    public void markForDestroy() {
        toDestroy = true;
    }
}
