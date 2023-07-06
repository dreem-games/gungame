package com.gungame.world.objects.phisical;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Array;
import com.gungame.world.collision.CollisionCategory;
import com.gungame.world.objects.meta.CustomObjectInitializationConfig;
import com.gungame.world.objects.meta.DynamicVisibleGameObject;
import com.gungame.world.objects.meta.GameObjectFactoryManager;
import com.gungame.world.objects.meta.GameObjectType;
import lombok.Getter;

public class Hero extends DynamicVisibleGameObject {
    private static final float BOX_COLLISION_BODY_CIRCLE_RADIUS = .15f;
    private static final float MAX_STAMINA = 100f;

    private float xScale;
    private float yScale;
    private @Getter float stamina = MAX_STAMINA;
    private long lastStaminaUpdate = System.nanoTime();
    private boolean staminaRegenBlocked = false;

    public Hero(GameObjectType type, Body body, Sprite sprite) {
        super(type, body, sprite);
    }

    public void fire() {
        var bulletFactory = GameObjectFactoryManager.getInstance(getWorld()).getBulletFactory();
        var position = getPosition();
        float angle = getAngle();
        float virtualAngle = angle - .3f;
        float x = position.x + MathUtils.cos(virtualAngle) * xScale / 1.7f;
        float y = position.y + MathUtils.sin(virtualAngle) * yScale / 1.7f;

        var hidesBox = hidesBox(x, y);
        CustomObjectInitializationConfig customInitConfig = null;
        if (hidesBox != null) {
            customInitConfig = new CustomObjectInitializationConfig();
            customInitConfig.setGroupIndex(hidesBox.getGroupIndex());
        }

        bulletFactory.create(x, y, angle * MathUtils.radiansToDegrees, customInitConfig,
                bullet -> bullet.setVelocity(MathUtils.cos(angle) * 70, MathUtils.sin(angle) * 70));
    }

    private Box hidesBox(float x, float y) {
        var arr = new Array<Body>();
        getWorld().getBodies(arr);

        Box nearestBox = null;
        float nearestDistance = Float.MAX_VALUE;
        final float minDistance = Math.min(xScale, yScale) * BOX_COLLISION_BODY_CIRCLE_RADIUS * 5;
        // поворот не учитывается, так что пока для небольшого количества объектов норм

        for (var body : arr) {
            var userData = body.getUserData();
            if (userData instanceof Box box) {
                var distance = box.getPosition().dst(x, y);
                if (distance < minDistance && distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestBox = box;
                }
            }
        }

        return nearestBox;
    }

    @Override
    public void applyImpulse(float x, float y) {
        super.applyImpulse(x, y);

        // по хардкору либо передвигаемся, либо регенерируем стамину
        staminaRegenBlocked = true;
    }

    public boolean tryUseStamina(float stamina) {
        if (this.stamina >= stamina) {
            this.stamina -= stamina;
            return true;
        }
        return false;
    }

    @Override
    public void update() {
        super.update();

        long now = System.nanoTime();
        if (!staminaRegenBlocked && stamina < MAX_STAMINA) {
            long delta = now - lastStaminaUpdate;
            stamina = Math.min(MAX_STAMINA, stamina + delta / 100_000_000f);
        }
        lastStaminaUpdate = now;
        staminaRegenBlocked = false;
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
                .getMassData();
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

    public float getMaxStamina() {
        return MAX_STAMINA;
    }
}
