package com.gungame.world.objects.meta;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class GameObjectUtils {

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
}
