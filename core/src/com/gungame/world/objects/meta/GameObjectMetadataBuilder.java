package com.gungame.world.objects.meta;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.MassData;
import com.gungame.assets.TextureManager;
import lombok.NonNull;

public class GameObjectMetadataBuilder {
    private static final MassData DEFAULT_MASS_DATA = new MassData();

    private GameObjectType type;
    private String textureRegionName;
    private TextureManager.AtlasType atlasType;
    private String bodyName;
    private Float diameter;
    private Vector2 size;
    private MassData massData;
    private float linearDamping;
    private float angularDamping;
    private float restitution;
    private float friction = .5f;
    private boolean isBullet;

    public GameObjectMetadataBuilder setRestitution(float restitution) {
        this.restitution = restitution;
        return this;
    }

    public GameObjectMetadataBuilder setType(@NonNull GameObjectType type) {
        this.type = type;
        return this;
    }

    public GameObjectMetadataBuilder setBodyName(@NonNull TextureManager.AtlasType atlasType, @NonNull String regionName) {
        this.bodyName = regionName;
        this.atlasType = atlasType;
        this.textureRegionName = regionName;
        return this;
    }

    public GameObjectMetadataBuilder setBodyCircleDiameter(float diameter) {
        this.diameter = diameter;
        this.size = new Vector2(diameter, diameter);
        return this;
    }

    public GameObjectMetadataBuilder setSize(float x, float y) {
        this.size = new Vector2(x, y);
        return this;
    }

    public GameObjectMetadataBuilder setMassData(float mass, float originX, float originY) {
        massData = new MassData();
        massData.mass = mass;
        massData.center.set(originX, originY);
        return this;
    }

    public GameObjectMetadataBuilder setLinearDamping(float linearDamping) {
        this.linearDamping = linearDamping;
        return this;
    }

    public GameObjectMetadataBuilder setAngularDamping(float angularDamping) {
        this.angularDamping = angularDamping;
        return this;
    }

    public GameObjectMetadataBuilder setBullet() {
        isBullet = true;
        return this;
    }

    public GameObjectMetadataBuilder setFriction(float friction) {
        this.friction = friction;
        return this;
    }

    public GameObjectMetadata createGameObjectMetadata() {
        if (massData == null) {
            massData = DEFAULT_MASS_DATA;
        }
        assert diameter == null || bodyName == null;
        return new GameObjectMetadata(type, massData, atlasType, textureRegionName, size, bodyName, diameter, linearDamping, angularDamping, friction, 50, restitution,  isBullet);
    }
}
