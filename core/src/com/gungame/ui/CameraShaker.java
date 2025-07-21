package com.gungame.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;

public class CameraShaker {
    private float time = -1f;
    private float duration;
    private float intensity;

    private float lastOffsetX;
    private float lastOffsetY;

    public void shake(float duration, float intensity) {
        if (intensity < getcurrentIntensity()) {
            return;
        }

        this.intensity = intensity;
        this.duration = duration;
        this.time = duration;
    }

    private float getcurrentIntensity() {
        return intensity * (time / duration); // затухание со временем
    }

    public void update(Camera camera, float delta) {
        if (lastOffsetX != 0 || lastOffsetY != 0) {
            camera.position.add(-lastOffsetX, -lastOffsetY, 0);
            lastOffsetX = lastOffsetY = 0;
        }

        if (time <= 0) {
            return;
        }

        time -= delta;

        float currentIntensity = getcurrentIntensity();
        float offsetX = (MathUtils.random() - 0.5f) * currentIntensity;
        float offsetY = (MathUtils.random() - 0.5f) * currentIntensity;

        camera.position.add(lastOffsetX = offsetX, lastOffsetY = offsetY, 0);
        camera.update();
    }
}
