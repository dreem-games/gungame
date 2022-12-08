package com.gungame.world;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.gungame.GameObject;
import com.gungame.world.walls.WallsGenerationUtils;

public class GameContactListener implements ContactListener {

    @Override
    public void beginContact(Contact contact) {
        var userDataA = (GameObject) contact.getFixtureA().getBody().getUserData();
        var userDataB = (GameObject) contact.getFixtureB().getBody().getUserData();

        if (userDataA.isToDestroy() || userDataB.isToDestroy()) {
            return;
        }

        WallsGenerationUtils.recreateBoxIfNesseseryOnCollision(userDataA, userDataB);
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
