package com.gungame.world.objects.meta;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.MassData;

public record GameObjectMetadata (
        GameObjectType type,
        String texturePath,
        String bodyName,
        Vector2 size,
        MassData massData
) {}
