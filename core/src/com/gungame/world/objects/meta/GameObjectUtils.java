package com.gungame.world.objects.meta;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

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
        Sprite sprite = new Sprite(texture);
        var gameObject = type.createInstance(body, sprite, parent);

        // инициализируем body
        var fixtureDef = createFixture(gameObject);
        bodyLoader.attachFixture(body, name, fixtureDef, size, texture, gameObject.getDefaultMassData());
        body.setUserData(gameObject);

        // инициализируем спрайт
        sprite.setSize(size.x, size.y);
        sprite.setPosition(x, y);
        Vector2 localCenter = body.getLocalCenter();
        sprite.setOrigin(localCenter.x, localCenter.y);
        sprite.setRotation(rotation);

        gameObject.postConstruct();
        return gameObject;
    }

    public static FixtureDef createFixture(GameObject gameObject) {
        var fixtureDef = new FixtureDef();
        fixtureDef.friction = 0.99f;
        fixtureDef.density = 50f;
        gameObject.setupCollisionFilter(fixtureDef.filter);
        return fixtureDef;
    }

    public static List<GameObject> getGameObjects(World world) {
        var bodies = new Array<Body>();
        world.getBodies(bodies);
        List<GameObject> result = new ArrayList<>(bodies.size);
        for (var body : bodies) {
            var userData = (GameObject) body.getUserData();
            if (userData != null)
                result.add(userData);
        }
        return result;
    }

    public static Stream<VisibleGameObject> getVisibleGameObjects(World world) {
        return getGameObjects(world).stream()
                .filter(it -> it instanceof VisibleGameObject)
                .map(it -> (VisibleGameObject) it)
                .sorted(Comparator.comparing(VisibleGameObject::getDrawLevel));
    }

    public static MassData createMassData(float mass, float originX, float originY) {
        var res = new MassData();
        res.mass = mass;
        res.center.set(originX, originY);
        return res;
    }
}
