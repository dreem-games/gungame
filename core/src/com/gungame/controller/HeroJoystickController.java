package com.gungame.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.gungame.world.objects.phisical.Hero;
import com.gungame.world.objects.phisical.MovingMode;
import lombok.Getter;

public class HeroJoystickController extends HeroController {

    private Vector2 lastRightVec = new Vector2();
    private boolean r1WasPressed;
    private boolean yWasPressed;

    private final @Getter Controller controller;

    public HeroJoystickController(Hero hero, Camera camera, boolean isFirst) {
        super(hero, camera);
        this.controller = Controllers.getControllers().get(isFirst ? 0 : 1);
    }

    public static int connectedControllersCount() {
        return Controllers.getControllers().size;
    }

    @Override
    public boolean control() {
        boolean used = false;
        var mapping = controller.getMapping();

        var rightVec = new Vector2(controller.getAxis(mapping.axisRightX),
                -controller.getAxis(mapping.axisRightY));
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

        used |= hero.move(controller.getAxis(mapping.axisLeftX),
                -controller.getAxis(mapping.axisLeftY));

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

        if (controller.getButton(controller.getMapping().buttonDpadRight)) {
            hero.setWeapon(0);
        }
        if (controller.getButton(controller.getMapping().buttonDpadUp)) {
            hero.setWeapon(1);
        }
        if (controller.getButton(controller.getMapping().buttonDpadLeft)) {
            hero.setWeapon(2);
        }

        if (controller.getButton(mapping.buttonX)) {
            hero.reloadStart();
        }

        if (used) {
            rotate(rightVec.x, rightVec.y);
        }

        if (controller.getButton(controller.getMapping().buttonBack)) {
            Gdx.app.exit();
        }

        return used;
    }
}
