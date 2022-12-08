package com.gungame;

import com.badlogic.gdx.physics.box2d.BodyDef;

public enum GameObjectType {
    WALL(BodyDef.BodyType.StaticBody),
    BOX(BodyDef.BodyType.DynamicBody),
    HERO(BodyDef.BodyType.DynamicBody),
    BULLET(BodyDef.BodyType.KinematicBody);

    private final BodyDef.BodyType bodyType;

    GameObjectType(BodyDef.BodyType bodyType) {
        this.bodyType = bodyType;
    }

    public BodyDef.BodyType getBodyType() {
        return bodyType;
    }
}
