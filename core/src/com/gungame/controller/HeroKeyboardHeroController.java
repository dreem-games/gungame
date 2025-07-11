package com.gungame.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.gungame.world.objects.phisical.Hero;
import com.gungame.world.objects.phisical.MovingMode;

public class HeroKeyboardHeroController extends HeroController {

    private float lastMouseX, lastMouseY;

    public HeroKeyboardHeroController(Hero hero, Camera camera) {
        super(hero, camera);
    }

    public boolean control() {
        boolean used = false;
        var impulse = new Vector2();
        MovingMode movingMode = MovingMode.NORMAL;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            impulse.x -= 1f;
            used = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            impulse.x += 1f;
            used = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            impulse.y -= 1f;
            used = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            impulse.y += 1f;
            used = true;
        }

        if (used) {
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
                movingMode = MovingMode.JUMPING;
            } else if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                movingMode = MovingMode.RUNNING;
            }
            hero.tryChangeMovingMode(movingMode);
        } else {
            hero.tryChangeMovingMode(MovingMode.STANDING);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.R)) {
            hero.reloadStart();
        }

        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        if (lastMouseX != mousePos.x || lastMouseY != mousePos.y) {
            used = true;
            lastMouseX = mousePos.x;
            lastMouseY = mousePos.y;
        }
        camera.unproject(mousePos);
        rotateToPoint(mousePos.x, mousePos.y);

        impulse = impulse.nor();

        used |= hero.move(impulse.x, impulse.y);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            hero.fire();
            used = true;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        return used;
    }
}
