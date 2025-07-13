package com.gungame.world.objects.phisical;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.gungame.world.GameWorld;
import com.gungame.world.collision.CollisionCategory;
import com.gungame.world.collision.ExplosionUtils;
import com.gungame.world.objects.meta.DynamicVisibleGameObject;
import com.gungame.world.objects.meta.GameObjectType;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ThreadLocalRandom;

@Getter
public class Box extends DynamicVisibleGameObject {
    private static final int DEFAULT_DAMPING = 10;
    private static short minGroupIndex = 0;// заготовочка для пуль
    private static long now = System.currentTimeMillis();
    private static final int DAMPING_RESTORE_TIME = 100; //Время в миллисекундах, после которого начинается восстановление сопротивления.
    private long timeWhenDampingTimeChanged = 0;

    private @Setter short groupIndex;

    public Box(GameWorld gameWorld, GameObjectType type, Body body, Sprite sprite) {
        super(gameWorld, type, body, sprite);
        groupIndex = (short) ThreadLocalRandom.current().nextInt(Short.MIN_VALUE, 0);
    }

    @Override
    public void update() {

        super.update();
        now = System.currentTimeMillis();
        timeWhenDampingTimeChanged = ExplosionUtils.checkDamping
                (body, now, DEFAULT_DAMPING, timeWhenDampingTimeChanged, DAMPING_RESTORE_TIME);
    }

    @Override
    public void setupCollisionFilter(Filter filter) {
        filter.groupIndex = groupIndex;
        filter.categoryBits = CollisionCategory.SMALL_OBJECTS.getBitMask();
        filter.maskBits = CollisionCategory.SMALL_OBJECTS.getBitMask();
    }

    @Override
    public void dispose() {
    }
}
