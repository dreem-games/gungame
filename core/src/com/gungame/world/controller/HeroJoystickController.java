package com.gungame.world.controller;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.gungame.world.objects.phisical.Hero;

public class HeroJoystickController extends HeroController {

    Vector2 lastRightVec = new Vector2();
    boolean r1WasPressed;

    private final Controller controller;

    public HeroJoystickController(Hero hero, Camera camera) {
        super(hero, camera);
        controller = Controllers.getCurrent();
    }

    @Override
    public boolean control() {
        if (controller == null) {
            return false;
        }

        boolean used = false;
        var mapping = controller.getMapping();

        var rightVec = new Vector2(normalized(controller.getAxis(mapping.axisRightX)),
                -normalized(controller.getAxis(mapping.axisRightY)));
        float length = rightVec.x * rightVec.x + rightVec.y * rightVec.y;
        if (length < .1f) {
            rightVec = lastRightVec;
        } else if (length > .15f) {
            lastRightVec = rightVec = rightVec.nor();
            used = true;
        }

        MovingMode movingMode = MovingMode.NORMAL;
        if (controller.getButton(mapping.buttonB)) {
            movingMode = MovingMode.JUMPING;
        } else if (controller.getButton(mapping.buttonA)) {
            movingMode = MovingMode.RUNNING;
        }
        tryChangeMovingMode(movingMode);

        used |= move(normalized(controller.getAxis(mapping.axisLeftX)),
                -normalized(controller.getAxis(mapping.axisLeftY)));

        var r1Pressed = controller.getButton(controller.getMapping().buttonR1);
        if (r1Pressed && !r1WasPressed) {
            hero.fire();
            used = true;
        }
        r1WasPressed = r1Pressed;

        if (used) {
            rotate(rightVec.x, rightVec.y);
        }

        return used;
    }

    private float normalized(float joystickValue) {
        return joystickValue - joystickValue % 0.1f;
    }
}
