package com.gungame.world.explosion;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.gungame.world.GameWorld;
import com.gungame.world.objects.phisical.Barrel;
import com.gungame.world.objects.phisical.Hero;
import lombok.Getter;

public class ExplosionUtils {
    private @Getter static final Array<Explosion> EXPLOSIONS = new Array<>();

    public static void createExplosion(GameWorld gameWorld, float x, float y, float radius, float power) {
        final Vector2 explosionCenter = new Vector2(x, y);

        final Array<Body> affectedBodies = new Array<>();

        EXPLOSIONS.add(new Explosion(gameWorld, x, y));
        // 1. Поиск тел в области через QueryAABB
        gameWorld.getPhisicsWorld().QueryAABB(fixture -> {
            Body body = fixture.getBody();
            Vector2 bodyPos = body.getWorldCenter();
            // Проверяем, что тело в круге (реальный радиус)
            if (bodyPos.dst(explosionCenter) <= radius) {
                if (!affectedBodies.contains(body, true)) { // избегаем дублирования
                    affectedBodies.add(body);
                }
            }
            return true; // продолжаем искать
        },
                x - radius, y - radius,
                x + radius, y + radius
        );

        // 2. Применяем импульс ко всем подходящим телам
        for (Body body : affectedBodies) {
            Object gameObject = body.getUserData();
            Vector2 bodyPos = body.getWorldCenter();
            Vector2 forceDir = bodyPos.cpy().sub(explosionCenter).nor(); // направление от центра
            float distance = bodyPos.dst(explosionCenter);
            body.setLinearDamping(0);

            // Мягкое затухание импульса по расстоянию
            float forceMag = power * (float)Math.pow(1 - distance/radius, 3);
            if (forceMag < 0) forceMag = 0;
            Vector2 impulse = forceDir.scl(forceMag);
            body.applyLinearImpulse(impulse, body.getWorldCenter(), true);

            if (body.getUserData() instanceof Hero) {
                ((Hero) gameObject).takeDamage((int)forceMag / 5);
            }

            if (body.getUserData() instanceof Barrel) {
                ((Barrel) gameObject).explode(gameWorld);
            }
        }
    }

    /**
     * Метод отвечает за возврат
     * LinearDamping к дефолтному значению
     * после взрыва.
     * Можно еще менять переменные, что бы работало.
     */
    public static long checkDamping(Body body, long now, int defaultDamping, long changeDampingTime, int dampingRestoreTime) {
        long outputTime = changeDampingTime;
        if (body.getLinearDamping() != defaultDamping && changeDampingTime == 0) {
            outputTime = now;
        }
        long timer = now - changeDampingTime;
        if (timer > dampingRestoreTime && timer < dampingRestoreTime + 1000) {
            body.setLinearDamping(body.getLinearDamping() + (((defaultDamping - body.getLinearDamping()))) / 2000);// Медленно возвращаем дефолтное значение.
        } else if (changeDampingTime != 0 && timer > 1500) {  //Через полторы строго возвращаем назад в дефолту
            outputTime = 0;
            body.setLinearDamping(defaultDamping);
        }
        return outputTime;
    }
}