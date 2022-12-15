package com.gungame.world.collision;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.gungame.world.objects.meta.GameObject;
import com.gungame.world.objects.meta.GameObjectType;
import com.gungame.world.objects.phisical.WallsGenerationUtils;

public class GameContactListener implements ContactListener {

    @Override
    public void beginContact(Contact contact) {
        var objectA = (GameObject) contact.getFixtureA().getBody().getUserData();
        var objectB = (GameObject) contact.getFixtureB().getBody().getUserData();

        if (objectA.isToDestroy() || objectB.isToDestroy()) {
            return;
        }

        // мб как-нибудь потом нормальную генерацию сделаем...
        WallsGenerationUtils.recreateBoxIfNecessaryOnCollision(objectA, objectB);

        if (objectA.getType() == GameObjectType.BULLET) {
            objectA.markForDestroy();
        }
        if (objectB.getType() == GameObjectType.BULLET) {
            objectB.markForDestroy();
        }
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
