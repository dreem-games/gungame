package com.gungame.world.objects.meta;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class GameObjectUtils {
    private static final Comparator<VisibleGameObject> drawLevelComparator
            = Comparator.comparing(VisibleGameObject::getDrawLevel);

    // Пул Array<Body> через ThreadLocal, чтобы не аллоцировать новый массив
    // каждый кадр — getGameObjectsStream вызывается дважды за кадр в renderWorld.
    // libGDX Array не аллоцирует нативный буфер при создании, поэтому оверхед
    // нового экземпляра — это java-объект + GC pressure.
    private static final ThreadLocal<Array<Body>> BODY_ARRAY_POOL
            = ThreadLocal.withInitial(Array::new);

    public static Stream<GameObject> getGameObjectsStream(World world) {
        Array<Body> bodies = BODY_ARRAY_POOL.get();
        bodies.clear();
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
                .sorted(drawLevelComparator);
    }
}
