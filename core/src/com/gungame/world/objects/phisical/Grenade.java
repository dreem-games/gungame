package com.gungame.world.objects.phisical;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.gungame.world.GameWorld;
import com.gungame.world.explosion.ExplosionUtils;
import com.gungame.world.objects.meta.GameObjectType;
import com.gungame.world.objects.meta.VisibleGameObject;

import static com.gungame.world.collision.CollisionFilters.initGrenadeFilter;

public class Grenade extends VisibleGameObject {
    private static final Sound explosionSpund = Gdx.audio.newSound(Gdx.files.internal("sound/barrelExplosion.wav"));

    private final long createdTime = System.currentTimeMillis();

    public Grenade(GameWorld gameWorld, GameObjectType type, Body body, Sprite sprite) {
        super(gameWorld, type, body, sprite);
    }

    public void grenadeLifeCycle(long timeOfLife) {
        if (timeOfLife < 1000) {
            return;
        }
        if (timeOfLife < 1100 && timeOfLife > 1000) {
            setVelocity(0,0);
        }
        if (timeOfLife > 2000) {
            explode();
        }
    }

    public void explode() {
        Vector2 center = body.getWorldCenter();
        ExplosionUtils.createExplosion(getWorld(), center.x, center.y, 5f, 100f);
        explosionSpund.play();
        markForDestroy();
    }

    @Override
    public void setupCollisionFilter(Filter filter) {
        initGrenadeFilter(filter);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void activate() {
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
