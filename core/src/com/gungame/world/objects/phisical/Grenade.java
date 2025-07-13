package com.gungame.world.objects.phisical;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.World;
import com.gungame.world.GameWorld;
import com.gungame.world.collision.CollisionCategory;
import com.gungame.world.collision.ExplosionUtils;
import com.gungame.world.objects.meta.GameObjectType;
import com.gungame.world.objects.meta.VisibleGameObject;

import static com.gungame.world.GameWorldConfig.BULLET_SPEED;

public class Grenade extends VisibleGameObject {
    private short groupIndex = 0;
    private long createdTime = System.currentTimeMillis();
    private final Sound explosionSpund = Gdx.audio.newSound(Gdx.files.internal("sound/barrelExplosion.wav"));

    public Grenade(GameWorld gameWorld, GameObjectType type, Body body, Sprite sprite) {
        super(gameWorld, type, body, sprite);
    }

    public void setGroupIndex(short groupIndex) {
        this.groupIndex = groupIndex;
    }

    public short getGroupIndex() {
        return groupIndex;
    }

    public void grenadeLifeCycle(long timeOfLife) {
        if (timeOfLife < 1500) {
            return;
        }
        if (timeOfLife < 1600 && timeOfLife > 1500) {
            setVelocity(0,0);
        }
        if (timeOfLife > 3000) {
            explode();
        }
    }

    public void explode() {
        Vector2 center = body.getWorldCenter();
        ExplosionUtils.createExplosion(getWorld().getPhisicsWorld(), center.x, center.y, 15f, 1500f);
        explosionSpund.play();
        markForDestroy();
    }


    @Override
    public void setupCollisionFilter(Filter filter) {
        filter.groupIndex = groupIndex;
        filter.categoryBits = CollisionCategory.HEIGHT_OBJECTS.getBitMask();
        filter.maskBits = CollisionCategory.HEIGHT_OBJECTS.getBitMask();
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
        applyImpulse(impulseX * BULLET_SPEED, impulseY * BULLET_SPEED);
        body.setBullet(true);
    }

    @Override
    public void update() {
        super.update();
        long now = System.currentTimeMillis();
        grenadeLifeCycle(now - createdTime);
    }

    @Override
    public int getDrawLevel() {
        return 2;
    }

    @Override
    public void dispose() {
    }
}
