package com.gungame.world.light;

import com.badlogic.gdx.physics.box2d.Filter;
import com.gungame.world.collision.CollisionCategory;

public class Lights {
    public static final Filter RAY_CONTACT_FILTER = new Filter();
    static {
        RAY_CONTACT_FILTER.categoryBits = CollisionCategory.ILLUMINABLE.getBitMask();
        RAY_CONTACT_FILTER.maskBits = CollisionCategory.ILLUMINABLE.getBitMask();
    }
}
