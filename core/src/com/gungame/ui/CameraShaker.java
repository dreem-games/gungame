package com.gungame.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/** Добавляет камере дрожание, позволяя нескольким всплескам накладываться. */
public class CameraShaker {

    /** Один отдельный «толчок». */
    private static class Shake {
        final float duration;
        float timeLeft;
        final float intensity;

        Shake(float duration, float intensity) {
            this.duration = duration;
            this.timeLeft = duration;
            this.intensity = intensity;
        }

        /** затухание */
        float currentIntensity() {
            float alpha = timeLeft / duration;
            alpha = Interpolation.pow2Out.apply(alpha);
            return intensity * alpha;
        }
    }

    private final Array<Shake> active = new Array<>();
    private float lastOffsetX, lastOffsetY;

    /** Добавляет новый толчок (можно вызывать сколько угодно раз подряд). */
    public void shake(float duration, float intensity) {
        active.add(new Shake(duration, intensity));
    }

    /** Вызывать каждый кадр. */
    public void update(Camera cam, float delta) {
        boolean needUpdate = false;

        // Сначала убираем смещение, добавленное на прошлом кадре
        if (lastOffsetX != 0 || lastOffsetY != 0) {
            cam.position.add(-lastOffsetX, -lastOffsetY, 0);
            needUpdate = true;
            lastOffsetX = lastOffsetY = 0;
        }

        // Суммарное смещение от всех активных толчков
        for (int i = active.size - 1; i >= 0; i--) {
            Shake s = active.get(i);
            s.timeLeft -= delta;

            if (s.timeLeft <= 0) {                // закончился — удалить
                active.removeIndex(i);
                continue;
            }

            float cur = s.currentIntensity();
            lastOffsetX += (MathUtils.random() - 0.5f) * cur;
            lastOffsetY += (MathUtils.random() - 0.5f) * cur;
        }

        // Применяем итоговое смещение
        if (lastOffsetX != 0 || lastOffsetY != 0) {
            cam.position.add(lastOffsetX, lastOffsetY, 0);
            needUpdate = true;
        }

        if (needUpdate) {
            cam.update();
        }
    }
}
