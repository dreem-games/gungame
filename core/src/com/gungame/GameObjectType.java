package com.gungame;

import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.MassData;

public enum GameObjectType {
    WALL(BodyDef.BodyType.StaticBody),
    BOX(BodyDef.BodyType.DynamicBody),
    HERO(BodyDef.BodyType.DynamicBody, createMassData(100, .3f, .4f)),
    BULLET(BodyDef.BodyType.KinematicBody);

    private final BodyDef.BodyType bodyType;
    private final MassData massData;

    GameObjectType(BodyDef.BodyType bodyType) {
        this.bodyType = bodyType;
        this.massData = null;
    }

    GameObjectType(BodyDef.BodyType bodyType, MassData massData) {
        this.bodyType = bodyType;
        this.massData = massData;
    }

    public BodyDef.BodyType getBodyType() {
        return bodyType;
    }

    public MassData getMassData() {
        return massData;
    }

    private static MassData createMassData(float mass, float originX, float originY) {
        var res = new MassData();
        res.mass = mass;
        res.center.set(originX, originY);
        return res;
    }
}
