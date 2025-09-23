package com.gungame.controller;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.gungame.world.GameWorldConfig;
import com.gungame.world.objects.phisical.Hero;
import com.gungame.world.objects.phisical.MovingMode;

public class HeroAIController extends HeroController {

    private float moveX = 0;
    private float moveY = 0;
    private boolean fire;
    private boolean reload;
    private MovingMode movingMode = MovingMode.STANDING;

    public HeroAIController(Hero hero, Camera camera) {
        super(hero, camera);
    }

    @Override
    public boolean control() {
        System.out.println(fire);
        boolean used = false;

        // движение
        if (moveX != 0 || moveY != 0) {
            used |= hero.move(moveX, moveY);
        }

        // смена режима движения
        hero.tryChangeMovingMode(movingMode);

        // стрельба
        if (fire) {
            System.out.println(2);
            hero.fire();
            fire = false; // сбросим, чтобы не стрелял каждый кадр
            used = true;
        }

        // перезарядка
        if (reload) {
            hero.reloadStart();
            reload = false;
            used = true;
        }

        return used;
    }

    // ---------- Методы для AI ----------
    public void move(float x, float y) {
        hero.move(x, y);
    }
    public void rotate(Vector2 target) {
        rotateToPoint(target.x, target.y);
    }

    public void stop() {
        this.moveX = 0;
        this.moveY = 0;
        this.movingMode = MovingMode.STANDING;
    }

    public void setMovingMode(MovingMode mode) {
        this.movingMode = mode;
    }

    public void fireOnce() {
        this.fire = true;
    }

    public void reloadOnce() {
        this.reload = true;
    }
}