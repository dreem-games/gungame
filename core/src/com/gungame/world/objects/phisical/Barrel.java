package com.gungame.world.objects.phisical;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.gungame.assets.SoundManager;
import com.gungame.world.GameWorld;
import com.gungame.world.explosion.ExplosionUtils;
import com.gungame.world.objects.meta.DynamicVisibleGameObject;
import com.gungame.world.objects.meta.GameObjectType;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ThreadLocalRandom;

import static com.gungame.world.collision.CollisionFilters.initBoxFilter;

@Getter
public class Barrel extends DynamicVisibleGameObject implements FirePoint {

    public @Getter boolean isExploded = false;
    private @Setter short groupIndex;

    public Barrel(GameWorld gameWorld, GameObjectType type, Body body, Sprite sprite) {
        super(gameWorld, type, body, sprite);
        groupIndex = (short) ThreadLocalRandom.current().nextInt(Short.MIN_VALUE, 0);
    }

    public void explode(GameWorld world) {
        if (isExploded) {
            return;
        }
        isExploded = true; //защита от повторного взрыва
        // Координаты центра бочки
        Vector2 center = body.getWorldCenter();
        // Параметры взрыва (например, радиус 5, мощность 200)
        ExplosionUtils.createExplosion(world, center.x, center.y, 7f, 100f, SoundManager.Sfx.EXPLOSION);
        // Здесь же можно запустить анимацию, частицы, удалить бочку из мира и т.д.
        markForDestroy();
    }

    @Override
    public void setupCollisionFilter(Filter filter) {
        initBoxFilter(filter, groupIndex);
    }

    @Override
    public void dispose() {
    }
}
