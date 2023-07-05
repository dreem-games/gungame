package com.gungame.world.objects.meta;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.gungame.world.objects.phisical.Bullet;
import com.gungame.world.objects.phisical.Hero;
import com.gungame.world.objects.phisical.Box;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public enum GameObjectType {
    WALL(BodyDef.BodyType.StaticBody, StaticGameObject.class),
    BOX(BodyDef.BodyType.DynamicBody, Box.class),
    HERO(BodyDef.BodyType.DynamicBody, Hero.class),
    BULLET(BodyDef.BodyType.DynamicBody, Bullet.class);

    private final BodyDef.BodyType bodyType;
    private final GameObjectCreator instanceCreator;

    GameObjectType(BodyDef.BodyType bodyType, Class<? extends GameObject> subjectClass) {
        this.bodyType = bodyType;
        this.instanceCreator = getInstanceCreator(subjectClass);
    }

    private GameObjectCreator getInstanceCreator(Class<? extends GameObject> subjectClass) {
        var lookup = MethodHandles.publicLookup();

        try {
            try {
                var methodType = MethodType.methodType(void.class, GameObjectType.class, Body.class, Sprite.class);
                var constructorHandle = lookup.findConstructor(subjectClass, methodType);
                return (body, sprite) -> (GameObject) constructorHandle.invoke(this, body, sprite);
            } catch (NoSuchMethodException e) {
                try {
                    var methodType = MethodType.methodType(void.class, GameObjectType.class, Body.class);
                    var constructorHandle = lookup.findConstructor(subjectClass, methodType);
                    return (InvisibleGameObjectCreator) (body) -> (GameObject) constructorHandle.invoke(this, body);
                } catch (NoSuchMethodException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public BodyDef.BodyType getBodyType() {
        return bodyType;
    }

    public GameObject createInstance(Body body, Sprite sprite) {
        try {
            return instanceCreator.createInstance(body, sprite);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface GameObjectCreator {

        GameObject createInstance(Body body, Sprite sprite) throws Throwable;
    }

    @FunctionalInterface
    public interface InvisibleGameObjectCreator extends GameObjectCreator {

        @Override
        default GameObject createInstance(Body body, Sprite sprite) throws Throwable {
            return createInstance(body);
        }

        GameObject createInstance(Body body) throws Throwable;
    }
}
