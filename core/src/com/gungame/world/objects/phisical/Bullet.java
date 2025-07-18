package com.gungame.world.objects.phisical;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.gungame.world.GameWorld;
import com.gungame.world.collision.CollisionCategory;
import com.gungame.world.objects.meta.GameObjectType;
import com.gungame.world.objects.meta.VisibleGameObject;
import lombok.Setter;

public class Bullet extends VisibleGameObject {

    private short groupIndex = 0;
    public @Setter int damage;

    public Bullet(GameWorld gameWorld, GameObjectType type, Body body, Sprite sprite) {
        super(gameWorld, type, body, sprite);
    }

    public Bullet(GameWorld gameWorld, GameObjectType type, Body body, Sprite sprite, int damage) {
        super(gameWorld, type, body, sprite);
        this.damage = damage;
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
        sprite.setColor(Color.RED);
        super.postConstruct();
    }

    @Override
    public int getDrawLevel() {
        return 2;
    }

    @Override
    public void dispose() {
    }
}
