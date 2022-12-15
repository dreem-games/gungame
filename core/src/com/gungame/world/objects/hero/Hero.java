package com.gungame.world.objects.hero;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.gungame.world.objects.bullet.Bullet;
import com.gungame.world.objects.collision.CollisionCategory;
import com.gungame.world.objects.meta.DynamicVisibleGameObject;
import com.gungame.world.objects.meta.GameObjectFactoryManager;
import com.gungame.world.objects.meta.GameObjectType;

public class Hero extends DynamicVisibleGameObject {
    private static final float BOX_COLLISION_BODY_CIRCLE_RADIUS = .15f;

    public Hero(GameObjectType type, Body body, Sprite sprite) {
        super(type, body, sprite);
    }

    public Bullet fire() {
        // TODO: мб через фабрику лучше как-то?
        var result = new Bullet(GameObjectType.BULLET, null, null);
        return result;
    }

    @Override
    public void setupCollisionFilter(Filter filter) {
        filter.categoryBits = CollisionCategory.HEIGHT_OBJECTS.getBitMask();
        filter.maskBits = CollisionCategory.HEIGHT_OBJECTS.getBitMask();
    }

    @Override
    public void postConstruct() {
        super.postConstruct();

        var fixtureDef = new FixtureDef();
        fixtureDef.density = 100f;
        fixtureDef.friction = 1f;
        fixtureDef.filter.categoryBits = CollisionCategory.ALL.getBitMask();
        fixtureDef.filter.maskBits = CollisionCategory.ALL.getBitMask();

        var circleShape = new CircleShape();
        circleShape.setPosition(body.getLocalCenter());
        circleShape.setRadius(calculateCorrectBoxCollisionBodyCircleRadius());
        fixtureDef.shape = circleShape;
        body.createFixture(fixtureDef);
    }

    private float calculateCorrectBoxCollisionBodyCircleRadius() {
        var defaultMassData = GameObjectFactoryManager.getInstance(getWorld())
                .getHeroFactory()
                .getObjectMetadata()
                .massData();
        var massCenter = body.getMassData().center;
        float xScale = massCenter.x / defaultMassData.center.x;
        float yScale = massCenter.y / defaultMassData.center.y;
        return Math.min(xScale, yScale) * BOX_COLLISION_BODY_CIRCLE_RADIUS;
    }

    @Override
    public int getDrawLevel() {
        return 1;
    }
}
