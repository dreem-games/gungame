package com.gungame.world.objects.meta;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

public abstract class VisibleGameObject extends GameObject {

    private final Vector2 lastPosition;
    protected final Sprite sprite;
    private float lastAngle;

    public VisibleGameObject(GameObjectType type, Body body, Sprite sprite) {
        super(type, body);
        this.sprite = sprite;

        lastPosition = new Vector2(body.getWorldCenter());
        lastAngle = body.getAngle();
        syncSprite();
    }

    public void update() {
        super.update();
        if (isToDestroy()) {
            return;
        }

        if (Float.floatToIntBits(body.getAngle()) != Float.floatToIntBits(lastAngle)
                || !getPosition().equals(lastPosition)) {
            onPositionChanged();
        }
    }

    protected void onPositionChanged() {
        syncSprite();
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

    public int getDrawLevel() {
        return 0;
    }
}
