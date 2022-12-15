package com.gungame.world.objects.phisical;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.gungame.world.collision.CollisionCategory;
import com.gungame.world.objects.meta.DynamicVisibleGameObject;
import com.gungame.world.objects.meta.GameObjectType;

public class Box extends DynamicVisibleGameObject {
    private static short minGroupIndex = 0;  // заготовочка для пуль

    private final short groupIndex;

    public Box(GameObjectType type, Body body, Sprite sprite) {
        super(type, body, sprite);
        groupIndex = --minGroupIndex;
    }

    public short getGroupIndex() {
        return groupIndex;
    }

    @Override
    public void setupCollisionFilter(Filter filter) {
        filter.groupIndex = groupIndex;
        filter.categoryBits = CollisionCategory.SMALL_OBJECTS.getBitMask();
        filter.maskBits = CollisionCategory.SMALL_OBJECTS.getBitMask();
    }
}
