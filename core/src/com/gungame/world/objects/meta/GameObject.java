package com.gungame.world.objects.meta;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.MassData;
import com.gungame.world.objects.collision.CollisionCategory;

import static com.gungame.world.objects.meta.GameObjectUtils.createMassData;

public abstract class GameObject {
    public static final MassData DEFAULT_MASS_DATA = createMassData(10, .5f, .5f);

    protected final GameObjectType type;
    protected final Body body;
    protected final Object parent;  // объект, порадевший данный, например, фабрика
    private boolean toDestroy = false;

    public GameObject(GameObjectType type, Body body, Object parent) {
        this.type = type;
        this.body = body;
        this.parent = parent;
    }

    public abstract boolean isActive();

    public abstract void activate();

    public MassData getDefaultMassData() {
        return DEFAULT_MASS_DATA;
    }

    public void setupCollisionFilter(Filter filter) {
        filter.categoryBits = CollisionCategory.ALL.getBitMask();
        filter.maskBits = CollisionCategory.ALL.getBitMask();
    }

    public void postConstruct() {
        body.resetMassData();
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

    public void setAngularVelocity(float velocity) {
        activate();
        body.setAngularVelocity(velocity);
    }

    public GameObjectType getType() {
        return type;
    }

    public Object getParent() {
        return parent;
    }

    public boolean isToDestroy() {
        return toDestroy;
    }

    public void markForDestroy() {
        toDestroy = true;
    }
}
