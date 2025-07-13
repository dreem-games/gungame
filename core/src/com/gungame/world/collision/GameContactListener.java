package com.gungame.world.collision;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.gungame.world.objects.meta.GameObject;
import com.gungame.world.objects.meta.GameObjectType;
import com.gungame.world.objects.phisical.Barrel;
import com.gungame.world.objects.phisical.Bullet;
import com.gungame.world.objects.phisical.Grenade;
import com.gungame.world.objects.phisical.Hero;

public class GameContactListener implements ContactListener {

    @Override
    public void beginContact(Contact contact) {
        var objectA = (GameObject) contact.getFixtureA().getBody().getUserData();
        var objectB = (GameObject) contact.getFixtureB().getBody().getUserData();

        // приведём к нормальному виду - пусть
        // если есть взаимодействие с пулей, то она будет в objectA
        if (objectB.getType() == GameObjectType.BULLET) {
            if (objectA.getType() == GameObjectType.BULLET) {
                objectB.markForDestroy();
                objectA.markForDestroy();
                return;  // ну две пули столкнулись, гг им
            }
            var tmp = objectA;
            objectA = objectB;
            objectB = tmp;
        }

        if (objectA.getType() == GameObjectType.BULLET) {
            objectA.markForDestroy();

            // теперь проверяем с чем же взаимодействует пуля
            if (objectB.getType() == GameObjectType.HERO) {
                ((Hero) objectB).takeDamage(((Bullet) objectA).damage);
            }
            if (objectB.getType() == GameObjectType.BARREL) {
                ((Barrel) objectB).explode(objectB.getWorld().getPhisicsWorld());
            }
            if (objectB.getType() == GameObjectType.GRENADE) {
                ((Grenade) objectB).grenadeLifeCycle(Long.MAX_VALUE);
            }
        }

        if (objectA.getType() == GameObjectType.BULLET && objectB.getType() == GameObjectType.GRENADE) {
            ((Grenade) objectB).explode();
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
