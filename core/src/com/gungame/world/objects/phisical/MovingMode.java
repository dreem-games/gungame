package com.gungame.world.objects.phisical;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MovingMode {
    STANDING(0, 0, 0, 1),
    NORMAL(0, 0, 0, 0.5f),
    RUNNING(0, 0, 0.025f, 0),
    JUMPING(100, 100, 0.7f, 0);  // как рывок в сторону

    private final long minDuration;
    private final long maxDuration;
    private final float staminaCost;
    private final float staminaRegenSpeed;
}