package com.gungame.world.objects.meta;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.gungame.assets.TextureManager;
import com.gungame.world.GameWorld;
import lombok.Getter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public class GameObjectFactory <T extends GameObject> {
    protected final GameWorld world;
    private final BodyEditorLoader bodyLoader;
    private final @Getter GameObjectMetadata objectMetadata;

    // такие действия как создание объектов можно выполнять только вне симуляции
    private final Queue<Runnable> updates = new LinkedList<>();

    public GameObjectFactory(GameWorld world, BodyEditorLoader bodyLoader, GameObjectMetadata metadata) {
        this.world = world;
        this.bodyLoader = bodyLoader;
        this.objectMetadata = metadata;
    }

    public void executeObjectsUpdates() {
        while (!updates.isEmpty()) {
            updates.poll().run();
        }
    }

    public void create(float x, float y, float rotation) {
        updates.add(() -> createImmediately(x, y, rotation));
    }

    public void create(float x, float y, float rotation, Consumer<T> initializer) {
        updates.add(() -> initializer.accept(createImmediately(x, y, rotation)));
    }

    public void create(float x, float y, float rotation,
                       CustomObjectInitializationConfig customObjectInitializationConfig,
                       Consumer<T> initializer) {
        updates.add(() -> initializer.accept(createImmediately(x, y, rotation, customObjectInitializationConfig)));
    }

    public void create(Vector2 pos, float rotation,
                       CustomObjectInitializationConfig customObjectInitializationConfig,
                       Consumer<T> initializer) {
        create(pos.x, pos.y, rotation, customObjectInitializationConfig, initializer);
    }

    public T createImmediately(float x, float y, float rotation,
                               CustomObjectInitializationConfig customObjectInitializationConfig) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = objectMetadata.getType().getBodyType();
        bodyDef.position.set(x, y);
        if (!objectMetadata.isBullet()) {
            bodyDef.linearDamping = objectMetadata.getLinearDamping();
            bodyDef.angularDamping = objectMetadata.getAngularDamping();
        }
        bodyDef.angle = rotation * MathUtils.degreesToRadians;
        bodyDef.bullet = objectMetadata.isBullet();

        var body = world.getPhisicsWorld().createBody(bodyDef);
        Sprite sprite = new Sprite(TextureManager.getRegion(objectMetadata.getAtlasType(), objectMetadata.getTextureRegionName()));
        T gameObject;
        try {
            gameObject = (T) objectMetadata.getType().createInstance(world, body, sprite);
        } catch (ClassCastException e) {
            throw new IllegalStateException("expected ");
        }

        // инициализируем body
        var fixtureDef = new FixtureDef();
        fixtureDef.friction = objectMetadata.getFriction();
        fixtureDef.density = objectMetadata.getDensity();
        fixtureDef.restitution = objectMetadata.getRestitution();
        gameObject.setupCollisionFilter(fixtureDef.filter);
        if (customObjectInitializationConfig != null) {
            customObjectInitializationConfig.postprocessCollisionFilter(fixtureDef.filter);
        }
        if (objectMetadata.getBodyName() != null) {
            bodyLoader.attachFixture(body, objectMetadata.getBodyName(), fixtureDef,
                    objectMetadata.getSize(), sprite, objectMetadata.getMassData());
        } else {
            // сейчас только круглые объекты как альтернатива
            var circleShape = new CircleShape();
            float radius = objectMetadata.getDiameter() / 2f;
            circleShape.setRadius(radius);
            circleShape.setPosition(new Vector2(radius, radius));
            fixtureDef.shape = circleShape;
            body.createFixture(fixtureDef); 
            circleShape.dispose();
        }
        body.resetMassData();
        body.setUserData(gameObject);

        // инициализируем спрайт
        sprite.setSize(objectMetadata.getSize().x, objectMetadata.getSize().y);
        sprite.setPosition(x, y);
        var localCenter = body.getLocalCenter();
        sprite.setOrigin(localCenter.x, localCenter.y);
        sprite.setRotation(rotation);

        gameObject.postConstruct();
        return gameObject;
    }

    public T createImmediately(float x, float y, float rotation) {
        return createImmediately(x, y, rotation, null);
    }
}
