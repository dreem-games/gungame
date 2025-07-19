package com.gungame.world.objects.meta;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.world.GameWorld;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public class GameObjectFactory <T extends GameObject> implements Disposable {
    protected final GameWorld world;
    private final Texture texture;
    private final BodyEditorLoader bodyLoader;
    private final GameObjectMetadata objectMetadata;

    // такие действия как создание объектов можно выполнять только вне симуляции
    private final Queue<Runnable> updates = new LinkedList<>();

    public GameObjectFactory(GameWorld world, BodyEditorLoader bodyLoader, GameObjectMetadata metadata) {
        this.world = world;
        this.texture = new Texture(metadata.getTexturePath());
        this.bodyLoader = bodyLoader;
        this.objectMetadata = metadata;
    }

    public void executeObjectsUpdates() {
        while (!updates.isEmpty()) {
            updates.poll().run();
        }
    }

    public GameObjectMetadata getObjectMetadata() {
        return objectMetadata;
    }

    @Override
    public void dispose() {
        texture.dispose();
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
        Sprite sprite = new Sprite(texture);
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
        bodyLoader.attachFixture(body, objectMetadata.getBodyName(), fixtureDef,
                objectMetadata.getSize(), texture, objectMetadata.getMassData());
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
