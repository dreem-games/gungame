package com.gungame.world.objects.phisical;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.gungame.world.collision.CollisionCategory;
import com.gungame.world.objects.meta.GameObjectType;
import com.gungame.world.objects.meta.VisibleGameObject;

public class Bullet extends VisibleGameObject {

    private short groupIndex = 0;

    public Bullet(GameObjectType type, Body body, Sprite sprite) {
        super(type, body, sprite);
    }

    public void setGroupIndex(short groupIndex) {
        this.groupIndex = groupIndex;
    }

    public short getGroupIndex() {
        return groupIndex;
    }

    @Override
    public void setupCollisionFilter(Filter filter) {
        filter.groupIndex = groupIndex;
        filter.categoryBits = CollisionCategory.ALL.getBitMask();
        filter.maskBits = CollisionCategory.ALL.getBitMask();
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void activate() {
    }

    @Override
    public void postConstruct() {
        float angle = getAngle();
        float impulseX = MathUtils.cos(angle), impulseY = MathUtils.sin(angle);
        applyImpulse(impulseX * 100, impulseY * 100);
    }

    @Override
    public int getDrawLevel() {
        return 2;
    }
}
