package com.gungame.world.objects.phisical;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.gungame.world.collision.CollisionCategory;
import com.gungame.world.objects.meta.DynamicVisibleGameObject;
import com.gungame.world.objects.meta.GameObjectFactoryManager;
import com.gungame.world.objects.meta.GameObjectType;

public class Hero extends DynamicVisibleGameObject {
    private static final float BOX_COLLISION_BODY_CIRCLE_RADIUS = .15f;

    private float xScale;
    private float yScale;

    public Hero(GameObjectType type, Body body, Sprite sprite) {
        super(type, body, sprite);
    }

    public void fire() {
        var bulletFactory = GameObjectFactoryManager.getInstance(getWorld()).getBulletFactory();
        var position = getPosition();
        float angle = getAngle();
        float virtualAngle = angle - .3f;
        float x = MathUtils.cos(virtualAngle) * xScale / 2, y = MathUtils.sin(virtualAngle) * yScale / 2;
        // TODO: найти точку получше
        bulletFactory.create(position.x + x, position.y + y, angle,
                bullet -> bullet.setVelocity(MathUtils.cos(angle) * 70, MathUtils.sin(angle) * 70));
        // TODO: applyForce
    }

    @Override
    public void setupCollisionFilter(Filter filter) {
        filter.categoryBits = CollisionCategory.HEIGHT_OBJECTS.getBitMask();
        filter.maskBits = CollisionCategory.HEIGHT_OBJECTS.getBitMask();
    }

    @Override
    public void postConstruct() {
        super.postConstruct();

        var defaultMassData = GameObjectFactoryManager.getInstance(getWorld())
                .getHeroFactory()
                .getObjectMetadata()
                .massData();
        var massCenter = body.getMassData().center;
        xScale = massCenter.x / defaultMassData.center.x;
        yScale = massCenter.y / defaultMassData.center.y;

        var fixtureDef = new FixtureDef();
        fixtureDef.density = 100f;
        fixtureDef.friction = 1f;
        fixtureDef.filter.categoryBits = CollisionCategory.ALL.getBitMask();
        fixtureDef.filter.maskBits = CollisionCategory.ALL.getBitMask();

        var circleShape = new CircleShape();
        circleShape.setPosition(body.getLocalCenter());
        circleShape.setRadius(Math.min(xScale, yScale) * BOX_COLLISION_BODY_CIRCLE_RADIUS);
        fixtureDef.shape = circleShape;
        body.createFixture(fixtureDef);
    }

    @Override
    public int getDrawLevel() {
        return 1;
    }
}
