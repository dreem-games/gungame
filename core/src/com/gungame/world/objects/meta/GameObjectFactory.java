package com.gungame.world.objects.meta;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public class GameObjectFactory <T extends GameObject> implements Disposable {

    protected final World world;
    private final Texture texture;
    private final BodyEditorLoader bodyLoader;
    private final GameObjectMetadata objectMetadata;

    // такие действия как создание объектов можно выполнять только вне симуляции
    private final Queue<Runnable> updates = new LinkedList<>();

    public GameObjectFactory(World world, BodyEditorLoader bodyLoader, GameObjectMetadata metadata) {
        this.world = world;
        this.texture = new Texture(metadata.texturePath());
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

    public T createImmediately(float x, float y, float rotation) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = objectMetadata.type().getBodyType();
        bodyDef.position.set(x, y);
        bodyDef.linearDamping = 5;
        bodyDef.angularDamping = 1;
        bodyDef.angle = rotation * MathUtils.degreesToRadians;

        var body = world.createBody(bodyDef);
        Sprite sprite = new Sprite(texture);
        T gameObject;
        try {
            gameObject = (T) objectMetadata.type().createInstance(body, sprite);
        } catch (ClassCastException e) {
            throw new IllegalStateException("expected ");
        }

        // инициализируем body
        var fixtureDef = new FixtureDef();
        fixtureDef.friction = 0.99f;
        fixtureDef.density = 50f;
        gameObject.setupCollisionFilter(fixtureDef.filter);
        bodyLoader.attachFixture(body, objectMetadata.bodyName(), fixtureDef, objectMetadata.size(), texture, objectMetadata.massData());
        body.resetMassData();
        body.setUserData(gameObject);

        // инициализируем спрайт
        sprite.setSize(objectMetadata.size().x, objectMetadata.size().y);
        sprite.setPosition(x, y);
        var localCenter = body.getLocalCenter();
        sprite.setOrigin(localCenter.x, localCenter.y);
        sprite.setRotation(rotation);

        gameObject.postConstruct();
        return gameObject;
    }
}
