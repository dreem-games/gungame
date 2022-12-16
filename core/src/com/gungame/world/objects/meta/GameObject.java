package com.gungame.world.objects.meta;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.World;
import com.gungame.world.collision.CollisionCategory;

public abstract class GameObject {

    protected final GameObjectType type;
    protected final Body body;
    private boolean toDestroy = false;

    public GameObject(GameObjectType type, Body body) {
        this.type = type;
        this.body = body;
    }

    public abstract boolean isActive();

    public abstract void activate();

    public void setupCollisionFilter(Filter filter) {
        filter.groupIndex = 0;
        filter.categoryBits = CollisionCategory.ALL.getBitMask();
        filter.maskBits = CollisionCategory.ALL.getBitMask();
    }

    public void postConstruct() {
    }

    public void update() {
        if (toDestroy) {
            destroy();
        }
    }

    protected void destroy() {
        body.getWorld().destroyBody(body);
    }

    public Vector2 getPosition() {
        return body.getWorldCenter();
    }

    public Vector2 getVelocity() {
        return body.getLinearVelocity();
    }

    public float getAngle() {
        return body.getAngle();
    }

    public void applyImpulse(float x, float y) {
        activate();
        Vector2 impulsePoint = getPosition();
        body.applyLinearImpulse(x, y, impulsePoint.x, impulsePoint.y, true);
    }

    public void setVelocity(float x, float y) {
        activate();
        body.setLinearVelocity(x, y);
    }

    public void setAngularVelocity(float velocity) {
        activate();
        body.setAngularVelocity(velocity);
    }

    public World getWorld() {
        return body.getWorld();
    }

    public GameObjectType getType() {
        return type;
    }

    public boolean isToDestroy() {
        return toDestroy;
    }

    public void markForDestroy() {
        toDestroy = true;
    }
}
