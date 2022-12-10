package com.gungame.world;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.gungame.GameObject;
import com.gungame.GameObjectType;

import java.util.ArrayList;
import java.util.List;

public class GameObjectUtils {

    public static GameObject createObject(World world, BodyEditorLoader bodyLoader, String name,
                                          Texture texture, Vector2 size, GameObjectType type,
                                          float x, float y, float rotation, Object parent) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = type.getBodyType();
        bodyDef.position.set(x, y);
        bodyDef.linearDamping = 5;
        bodyDef.angle = rotation * MathUtils.degreesToRadians;

        Body body = world.createBody(bodyDef);
        if (type.getMassData() != null) {
            body.setMassData(type.getMassData());
        }
        bodyLoader.attachFixture(body, name, new FixtureDef(), size, texture, type.getMassData());

        Sprite sprite = new Sprite(texture);
        sprite.setSize(size.x, size.y);
        sprite.setPosition(x, y);
        Vector2 localCenter = body.getLocalCenter();
        sprite.setOrigin(localCenter.x, localCenter.y);
        sprite.setRotation(rotation);

        GameObject gameObject = new GameObject(type, body, sprite, parent);
        body.setUserData(gameObject);
        return gameObject;
    }

    public static List<GameObject> getGameObjects(World world) {
        var bodies = new Array<Body>();
        world.getBodies(bodies);
        List<GameObject> result = new ArrayList<>(bodies.size);
        for (var body : bodies) {
            result.add((GameObject) body.getUserData());
        }
        return result;
    }

    public static void updateGameObjects(World world) {
        getGameObjects(world).stream()
                .filter(GameObject::isToDestroy)
                .forEach(gameObject -> world.destroyBody(gameObject.getBody()));
    }

    public static List<Sprite> getSyncedBodySprites(World world) {
        var result = new ArrayList<Sprite>();
        getGameObjects(world).forEach(gameObject -> {
            Sprite sprite = gameObject.getSprite();
            if (sprite != null) {
                if (gameObject.isToDestroy()) {
                    if (GameWorldConfig.SOW_MARKED_FOR_DELETE_OBJECTS) {
                        sprite.setColor(Color.BLACK);
                    } else {
                        return;
                    }
                }
                var center = gameObject.getBody().getWorldCenter();
                var localCenter = gameObject.getBody().getLocalCenter();
                float spriteX = center.x - localCenter.x, spriteY = center.y - localCenter.y;
                var angle = gameObject.getBody().getAngle();
                sprite.setPosition(spriteX, spriteY);
                sprite.setRotation(MathUtils.radiansToDegrees * angle);
                result.add(sprite);
            }
        });
        return result;
    }
}
