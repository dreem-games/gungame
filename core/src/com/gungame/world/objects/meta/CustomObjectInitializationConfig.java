package com.gungame.world.objects.meta;

import com.badlogic.gdx.physics.box2d.Filter;
import lombok.Getter;
import lombok.Setter;

/**
 * Позволяет более точно настроить объект перед созданием,
 * заменяя значения по-умолчанию на динамические.
 */
@Getter
@Setter
public class CustomObjectInitializationConfig {
    private Short groupIndex;

    public void postprocessCollisionFilter(Filter filter) {
        if (groupIndex != null) {
            filter.groupIndex = groupIndex;
        }
    }
}
