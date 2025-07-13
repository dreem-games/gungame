package com.gungame.world.objects.meta;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.MassData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class GameObjectMetadata {
        @NonNull private final GameObjectType type;
        @NonNull private final String texturePath;
        @NonNull private final String bodyName;
        @NonNull private final Vector2 size;
        @NonNull private final MassData massData;
        private final float linearDamping;
        private final float angularDamping;
        private final float friction;
        private final float density;
        private final boolean isBullet;
}
