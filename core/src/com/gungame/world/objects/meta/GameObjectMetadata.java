package com.gungame.world.objects.meta;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.MassData;
import com.gungame.assets.TextureManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class GameObjectMetadata {
        private final @NonNull GameObjectType type;
        private final @NonNull MassData massData;
        private final @NonNull TextureManager.AtlasType atlasType;
        private final @NonNull String textureRegionName;
        private final @NonNull Vector2 size;
        private final String bodyName;
        private final Float diameter;
        private final float linearDamping;
        private final float angularDamping;
        private final float friction;
        private final float density;
        private final float restitution;
        private final boolean isBullet;
}
