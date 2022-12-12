package com.gungame.world.objects.hero;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.gungame.world.objects.meta.GameObject;

public class HeroKeyboardHeroController extends HeroController {

    public HeroKeyboardHeroController(GameObject hero, Camera camera) {
        super(hero, camera);
    }

    public void control() {
        var impulse = new Vector2();
        boolean isRunning = false;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            impulse.x -= 1f;
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            impulse.x += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            impulse.y -= 1f;
        } else if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            impulse.y += 1f;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            isRunning = true;
        }

        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        var heroPosition = hero.getPosition();
        mousePos.x -= heroPosition.x;
        mousePos.y -= heroPosition.y;

        mousePos = mousePos.nor();
        impulse = impulse.nor();

        rotate(mousePos.x, mousePos.y);
        move(impulse.x, impulse.y, isRunning);
    }
}
