package com.gungame.world;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;

public class GameObjectUtils {

    public static GameObject createGameObject(World world, BodyEditorLoader bodyLoader, String name,
                                              Texture texture, Vector2 size, GameObjectType type,
                                              float x, float y, float rotation, Object parent) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = type.getBodyType();
        bodyDef.position.set(x, y);
        bodyDef.linearDamping = 5;
        bodyDef.angularDamping = 1;
        bodyDef.angle = rotation * MathUtils.degreesToRadians;

        var body = world.createBody(bodyDef);
        if (type.getMassData() != null) {
            body.setMassData(type.getMassData());
        }
        var fixtureDef = new FixtureDef();
        //        fixtureDef.filter
        bodyLoader.attachFixture(body, name, fixtureDef, size, texture, type.getMassData());

        Sprite sprite = new Sprite(texture);
        sprite.setSize(size.x, size.y);
        sprite.setPosition(x, y);
        Vector2 localCenter = body.getLocalCenter();
        sprite.setOrigin(localCenter.x, localCenter.y);
        sprite.setRotation(rotation);

        GameObject gameObject = type.createInstance(body, sprite, parent);
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
}
