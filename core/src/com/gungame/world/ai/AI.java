package com.gungame.world.ai;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import com.gungame.controller.HeroAIController;
import com.gungame.world.GameWorld;
import com.gungame.world.objects.meta.GameObject;
import com.gungame.world.objects.meta.GameObjectType;
import com.gungame.world.objects.phisical.Box;
import com.gungame.world.objects.phisical.Hero;


public class AI {
    final GameWorld world;
    Array<Body> bodies = new Array<>();
    final Camera camera;
    final Hero hero;
    HeroAIController controller;
    Body enemy;
    Vector2 targetPos = new Vector2();
    Body nearestHide;


    public AI(GameWorld world, Camera camera, Hero hero) {
        this.world = world;
        this.camera = camera;
        this.hero = hero;
        controller = new HeroAIController(hero, camera);
    }

    public void update(float delta) {
        world.getPhisicsWorld().getBodies(bodies);
        for (Body body : bodies) {
            if (body.getUserData() instanceof Hero && !body.equals(hero.getBody())) {
                enemy = body;
            }
            if(body.getUserData() instanceof Box) {
               checkNearestHide(body);
            }
        }
        var direction = calculateTargetPos();
        controller.move(direction.x,direction.y);
        controller.rotate(enemy.getPosition());
        controller.fireOnce();
        controller.control();
    }

    private float getDistance(Body bodyA, Body bodyB) {
        Vector2 posA = bodyA.getPosition();
        Vector2 posB = bodyB.getPosition();
        return posA.dst(posB);
    }

    private void checkNearestHide(Body body) {
        if (nearestHide == null) {
            nearestHide = body;
        }  else if ((getDistance(hero.getBody(), body)) < getDistance(nearestHide, body)) {
            nearestHide = body;
        }
    }

    private Vector2 calculateTargetPos() {
        var direction1 = nearestHide.getPosition().cpy().sub(hero.getPosition()).nor();
        var point = hero.getPosition().cpy().add(direction1.scl(0.001f));
        var direction = point.cpy().sub(hero.getPosition()).nor();
        return direction;
    }
}
