package com.gungame.world.hero;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.gungame.world.GameObject;

public class HeroKeyboardHeroController extends HeroController {

    public HeroKeyboardHeroController(GameObject hero, Camera camera) {
        super(hero, camera);
    }

    public void control() {
        float xImpulse = 0;
        float yImpulse = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            xImpulse -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            xImpulse += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            yImpulse -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            yImpulse += 1f;
        }

        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        var heroPosition = hero.getPosition();
        mousePos.x -= heroPosition.x;
        mousePos.y -= heroPosition.y;
        mousePos = mousePos.nor();


        rotate(mousePos.x, mousePos.y);
        move(xImpulse, yImpulse);
    }
}
