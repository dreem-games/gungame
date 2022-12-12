package com.gungame.world.objects.collision;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.gungame.world.objects.meta.GameObject;
import com.gungame.world.objects.walls.WallsGenerationUtils;

public class GameContactListener implements ContactListener {

    @Override
    public void beginContact(Contact contact) {
        var objectA = (GameObject) contact.getFixtureA().getBody().getUserData();
        var objectB = (GameObject) contact.getFixtureB().getBody().getUserData();

        if (objectA.isToDestroy() || objectB.isToDestroy()) {
            return;
        }

        WallsGenerationUtils.recreateBoxIfNesseseryOnCollision(objectA, objectB);
    }

    @Override
    public void endContact(Contact contact) {

    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }
}
