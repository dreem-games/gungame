package com.gungame.world.collision;

import com.badlogic.gdx.physics.box2d.ContactFilter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.gungame.world.objects.meta.GameObject;
import com.gungame.world.objects.meta.GameObjectType;
import com.gungame.world.objects.phisical.Bullet;

public class GameContactFilter implements ContactFilter {
    @Override
    public boolean shouldCollide(Fixture fixtureA, Fixture fixtureB) {
        var objectA = (GameObject) fixtureA.getBody().getUserData();
        var objectB = (GameObject) fixtureB.getBody().getUserData();
        if (objectB.getType() == GameObjectType.BULLET && objectA.getType() == GameObjectType.BULLET) { //если пули от одного выстрела, они не уничтожаются
            boolean result = ((Bullet) objectA).getShotID() != ((Bullet) objectB).getShotID();
            return ((Bullet) objectA).getShotID() != ((Bullet) objectB).getShotID();
        }
        return true;
    }
}
