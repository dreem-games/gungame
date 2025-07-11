package com.gungame.controller;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;
import com.gungame.world.objects.phisical.Hero;
import com.gungame.world.objects.phisical.MovingMode;
import lombok.Getter;

import java.util.Objects;

public class HeroJoystickController extends HeroController {

    private Vector2 lastRightVec = new Vector2();
    private boolean r1WasPressed;
    private boolean yWasPressed;

    private final @Getter Controller controller;

    public HeroJoystickController(Hero hero, Camera camera, @Null HeroJoystickController other) {
        super(hero, camera);
        if (other == null) {
            controller = Objects.requireNonNull(Controllers.getCurrent());
        } else {
            Controller found = null;
            Array<Controller> controllers = Controllers.getControllers();
            for (int i = 0; i < controllers.size; i++) {
                Controller it = controllers.get(i);
                if (it != other.controller) {
                    found = it;
                    break;
                }
            }
            if (found == null) {
                throw new RuntimeException("Gay pad not found");
            }
            controller = found;
        }
    }

    public static int connectedControllersCount() {
        return Controllers.getControllers().size;
    }

    @Override
    public boolean control() {
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
        } else if (!used) {
            movingMode = MovingMode.STANDING;
        }
        hero.tryChangeMovingMode(movingMode);

        used |= hero.move(normalized(controller.getAxis(mapping.axisLeftX)),
                -normalized(controller.getAxis(mapping.axisLeftY)));

        var r1Pressed = controller.getButton(controller.getMapping().buttonR1);
        if (r1Pressed && (!r1WasPressed || hero.getCurrentGun().isAutomatic())) {
            hero.fire();
            used = true;
        }
        r1WasPressed = r1Pressed;

        var yPressed = controller.getButton(controller.getMapping().buttonY);
        if(yPressed && !yWasPressed) {
            hero.switchWeapon();
        }
        yWasPressed = yPressed;

        if (controller.getButton(mapping.buttonX)) {
            hero.reloadStart();
        }

        if (used) {
            rotate(rightVec.x, rightVec.y);
        }

        return used;
    }

    private float normalized(float joystickValue) {
        return joystickValue - joystickValue % 0.1f;
    }
}
