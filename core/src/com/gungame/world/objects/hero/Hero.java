package com.gungame.world.objects.hero;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.MassData;
import com.gungame.world.objects.collision.CollisionCategory;
import com.gungame.world.objects.meta.DynamicVisibleGameObject;
import com.gungame.world.objects.meta.GameObjectType;
import com.gungame.world.objects.meta.GameObjectUtils;

import static com.gungame.world.objects.meta.GameObjectUtils.createMassData;

public class Hero extends DynamicVisibleGameObject {
    public static final MassData DEFAULT_MASS_DATA = createMassData(100, .3f, .4f);
    private static final float BOX_COLLISION_BODY_CIRCLE_RADIUS = .15f;

    public Hero(GameObjectType type, Body body, Sprite sprite, Object parent) {
        super(type, body, sprite, parent);
    }

    public MassData getDefaultMassData() {
        return DEFAULT_MASS_DATA;
    }

    @Override
    public void setupCollisionFilter(Filter filter) {
        filter.categoryBits = CollisionCategory.HEIGHT_OBJECTS.getBitMask();
        filter.maskBits = CollisionCategory.HEIGHT_OBJECTS.getBitMask();
    }

    @Override
    public void postConstruct() {
        super.postConstruct();

        var fixtureDef = GameObjectUtils.createFixture(this);
        fixtureDef.filter.categoryBits = CollisionCategory.ALL.getBitMask();
        fixtureDef.filter.maskBits = CollisionCategory.ALL.getBitMask();

        var circleShape = new CircleShape();
        circleShape.setPosition(body.getLocalCenter());
        circleShape.setRadius(calculateCorrectBoxCollisionBodyCircleRadius());
        fixtureDef.shape = circleShape;
        body.createFixture(fixtureDef);
    }

    private float calculateCorrectBoxCollisionBodyCircleRadius() {
        Vector2 massCenter = body.getMassData().center;
        float xScale = massCenter.x / DEFAULT_MASS_DATA.center.x;
        float yScale = massCenter.y / DEFAULT_MASS_DATA.center.y;
        return Math.min(xScale, yScale) * BOX_COLLISION_BODY_CIRCLE_RADIUS;
    }

    @Override
    public int getDrawLevel() {
        return 1;
    }
}
