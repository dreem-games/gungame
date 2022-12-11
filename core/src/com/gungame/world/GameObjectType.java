package com.gungame.world;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.MassData;

public enum GameObjectType {
    WALL(BodyDef.BodyType.StaticBody, StaticGameObject::new),
    BOX(BodyDef.BodyType.DynamicBody, createMassData(100, .5f, .5f), DynamicGameObject::new),
    HERO(BodyDef.BodyType.DynamicBody, createMassData(100, .3f, .4f), DynamicGameObject::new),
    BULLET(BodyDef.BodyType.KinematicBody, DynamicGameObject::new);

    private final BodyDef.BodyType bodyType;
    private final MassData massData;
    private final GameObjectCreator creator;

    GameObjectType(BodyDef.BodyType bodyType, GameObjectCreator creator) {
        this.bodyType = bodyType;
        this.massData = null;
        this.creator = creator;
    }

    GameObjectType(BodyDef.BodyType bodyType, MassData massData, GameObjectCreator creator) {
        this.bodyType = bodyType;
        this.massData = massData;
        this.creator = creator;
    }

    public BodyDef.BodyType getBodyType() {
        return bodyType;
    }

    public MassData getMassData() {
        return massData;
    }

    public GameObject createInstance(Body body, Sprite sprite, Object parent) {
        return creator.createInstance(this, body, sprite, parent);
    }

    private static MassData createMassData(float mass, float originX, float originY) {
        var res = new MassData();
        res.mass = mass;
        res.center.set(originX, originY);
        return res;
    }

    @FunctionalInterface
    private interface GameObjectCreator {
        GameObject createInstance(GameObjectType type, Body body, Sprite sprite, Object parent);
    }
}
