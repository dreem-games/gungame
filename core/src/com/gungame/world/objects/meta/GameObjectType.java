package com.gungame.world.objects.meta;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.gungame.world.objects.hero.Hero;
import com.gungame.world.objects.walls.Box;

import java.lang.reflect.InvocationTargetException;

public enum GameObjectType {
    WALL(BodyDef.BodyType.StaticBody, StaticGameObject.class),
    BOX(BodyDef.BodyType.DynamicBody, Box.class),
    HERO(BodyDef.BodyType.DynamicBody, Hero.class),
    BULLET(BodyDef.BodyType.KinematicBody, DynamicVisibleGameObject.class);

    private final BodyDef.BodyType bodyType;

    private final Class<? extends GameObject> subjectClass;

    GameObjectType(BodyDef.BodyType bodyType, Class<? extends GameObject> subjectClass) {
        this.bodyType = bodyType;
        this.subjectClass = subjectClass;
    }

    public BodyDef.BodyType getBodyType() {
        return bodyType;
    }

    public GameObject createInstance(Body body, Sprite sprite, Object parent) {
        // TODO: мб можно оптимальнее через MethodHandle?
        try {
            try {
                var constructor = subjectClass.getConstructor(GameObjectType.class, Body.class, Object.class);
                return constructor.newInstance(this, body, parent);
            } catch (NoSuchMethodException e) {
                try {
                    var constructor = subjectClass.getConstructor(GameObjectType.class, Body.class, Sprite.class, Object.class);
                    return constructor.newInstance(this, body, sprite, parent);
                } catch (NoSuchMethodException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
