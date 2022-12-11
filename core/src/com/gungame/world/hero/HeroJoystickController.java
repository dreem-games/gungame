package com.gungame.world.hero;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Camera;
import com.gungame.world.GameObject;

public class HeroJoystickController extends HeroController {

    private final Controller controller;

    public HeroJoystickController(GameObject hero, Camera camera) {
        super(hero, camera);
        controller = Controllers.getCurrent();
    }

    @Override
    public void control() {
        var mapping = controller.getMapping();

        rotate(controller.getAxis(mapping.axisRightX), controller.getAxis(mapping.axisRightY));
        move(controller.getAxis(mapping.axisLeftX), controller.getAxis(mapping.axisLeftY));
    }
}
