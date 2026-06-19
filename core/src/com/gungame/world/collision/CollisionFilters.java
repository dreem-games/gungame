package com.gungame.world.collision;

import com.badlogic.gdx.physics.box2d.Filter;

public class CollisionFilters {
    public static final Filter LOW_LIGHT_CONTACT_FILTER = createFilter(CollisionCategory.LOW_LIGHT, CollisionCategory.NORMAL_OBJECTS);
    public static final Filter HIGH_LIGHT_CONTACT_FILTER = createFilter(CollisionCategory.HIGH_LIGHT, CollisionCategory.HEIGHT_OBJECTS);

    public static void initBulletFilter(Filter filter, short groupId) {
        filter.categoryBits = CollisionCategory.MICRO_OBJECTS.getBits();
        filter.maskBits = CollisionCategory.ALL_PHYSICAL.getBits();
        filter.groupIndex = groupId;
    }

    public static void initBoxFilter(Filter filter, short groupId) {
        filter.categoryBits = CollisionCategory.SMALL_OBJECTS.getBits();
        filter.maskBits = (short) (CollisionCategory.ALL_PHYSICAL.getBits() | CollisionCategory.LOW_LIGHT.getBits());
        filter.groupIndex = groupId;
    }

    public static void initGrenadeFilter(Filter filter) {
        filter.categoryBits = CollisionCategory.MICRO_OBJECTS.getBits();
        filter.maskBits = (short) (CollisionCategory.HEIGHT_OBJECTS.getBits() | CollisionCategory.MICRO_OBJECTS.getBits());
    }

    public static Filter createFilter(CollisionCategory category, CollisionCategory mask, short groupId) {
        var result = new Filter();
        result.categoryBits = category.getBits();
        result.maskBits = mask.getBits();
        result.groupIndex = groupId;
        return result;
    }

    public static Filter createFilter(CollisionCategory category, CollisionCategory mask) {
        return createFilter(category, mask, (short) 0);
    }
}
