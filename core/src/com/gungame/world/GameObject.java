package com.gungame.world;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

public abstract class GameObject {
    protected final GameObjectType type;
    protected final Body body;
    protected final Sprite sprite;
    protected final Object parent;  // объект, порадевший данный, например, фабрика
    private boolean toDestroy = false;
    private final Vector2 lastPosition;
    private float lastAngle;

    public GameObject(GameObjectType type, Body body, Sprite sprite, Object parent) {
        this.type = type;
        this.body = body;
        this.sprite = sprite;
        this.parent = parent;

        lastPosition = new Vector2(body.getWorldCenter());
        lastAngle = body.getAngle();
        syncSprite();
    }

    public abstract boolean isActive();

    public abstract void activate();

    public void update() {
        if (toDestroy) {
            body.getWorld().destroyBody(body);
        } else if (Float.floatToIntBits(body.getAngle()) != Float.floatToIntBits(lastAngle)
                || !getPosition().equals(lastPosition)) {
            activate();
            syncSprite();
        }
    }

    /*
    Синхронизирует физическое и графическое представления, удаляет если требуется
     */
    private void syncSprite() {
        var center = body.getWorldCenter();
        var localCenter = body.getLocalCenter();
        float spriteX = center.x - localCenter.x, spriteY = center.y - localCenter.y;
        var angle = body.getAngle();
        sprite.setPosition(spriteX, spriteY);
        lastPosition.set(spriteX, spriteY);
        sprite.setRotation(MathUtils.radiansToDegrees * angle);
        lastAngle = angle;
    }

    public void draw(SpriteBatch batch) {
        sprite.draw(batch);
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
