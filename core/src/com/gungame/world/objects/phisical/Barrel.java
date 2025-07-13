package com.gungame.world.objects.phisical;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.gungame.world.GameWorld;
import com.gungame.world.collision.CollisionCategory;
import com.gungame.world.collision.ExplosionUtils;
import com.gungame.world.objects.meta.DynamicVisibleGameObject;
import com.gungame.world.objects.meta.GameObjectType;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ThreadLocalRandom;

@Getter
public class Barrel extends DynamicVisibleGameObject {

    private final Sound explosionSpund = Gdx.audio.newSound(Gdx.files.internal("sound/barrelExplosion.wav"));
    private static short minGroupIndex = 0; // заготовочка для пуль
    public @Getter boolean isExploded = false;

    private @Setter short groupIndex;


    public Barrel(GameWorld gameWorld, GameObjectType type, Body body, Sprite sprite) {
        super(gameWorld, type, body, sprite);
        groupIndex = (short) ThreadLocalRandom.current().nextInt(Short.MIN_VALUE, 0);
    }

    public void explode(World world) {
        if (isExploded) {
            return;
        }
        isExploded = true; //защита от повторного взрыва
        // Координаты центра бочки
        Vector2 center = body.getWorldCenter();
        // Параметры взрыва (например, радиус 5, мощность 200)
        ExplosionUtils.createExplosion(world, center.x, center.y, 15f, 1500f);
        // Здесь же можно запустить анимацию, частицы, удалить бочку из мира и т.д.
        explosionSpund.play();
        markForDestroy();
    }

    @Override
    public void setupCollisionFilter(Filter filter) {
        filter.groupIndex = groupIndex;
        filter.categoryBits = CollisionCategory.SMALL_OBJECTS.getBitMask();
        filter.maskBits = CollisionCategory.SMALL_OBJECTS.getBitMask();
    }
    @Override
    public void dispose() {
    }
}
