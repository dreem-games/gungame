package com.gungame.world.objects.meta;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class GameObjectUtils {
    private static final Comparator<VisibleGameObject> drowLevelComparator
            = Comparator.comparing(VisibleGameObject::getDrawLevel);

    public static Stream<GameObject> getGameObjectsStream(World world) {
        var bodies = new Array<Body>();
        world.getBodies(bodies);
        return StreamSupport.stream(bodies.spliterator(), false)
                .mapMulti((body, consumer) -> {
                    var userData = (GameObject) body.getUserData();
                    if (userData != null)
                        consumer.accept(userData);
                });
    }

    public static Stream<VisibleGameObject> getVisibleGameObjects(World world) {
        return getGameObjectsStream(world)
                .mapMulti((GameObject it, Consumer<VisibleGameObject> consumer) -> {
                    if (it instanceof VisibleGameObject visible) {
                        consumer.accept(visible);
                    }
                })
                .sorted(drowLevelComparator);
    }
}
