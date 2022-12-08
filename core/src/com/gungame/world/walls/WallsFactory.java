package com.gungame.world.walls;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.GameObjectType;
import com.gungame.world.GameObjectUtils;

import java.util.LinkedList;
import java.util.Queue;

public class WallsFactory implements Disposable {

    private final Vector2 wallSize = new Vector2(30, 30);
    private final Vector2 boxSize = new Vector2(40, 40);
    private final Texture wallTexture = new Texture("texture/wall.png");
    private final Texture boxTexture = new Texture("texture/box.jpg");
    private final World world;
    private final BodyEditorLoader bodyLoader;
    private final Queue<Runnable> updates = new LinkedList<>();  // такие действия как создание объектов можно выполнять не всегда

    public WallsFactory(World world, BodyEditorLoader bodyLoader) {
        this.world = world;
        this.bodyLoader = bodyLoader;
    }

    public Vector2 getWallPieceSize() {
        return wallSize;
    }

    public Vector2 getBoxSize() {
        return boxSize;
    }

    public void createWallPiece(float x, float y) {
        // они создаются только перед симуляцией, соответственно, проблем не будет
        GameObjectUtils.createObject(world, bodyLoader, "wall", wallTexture, wallSize,
                GameObjectType.WALL, x, y, 0, this);
    }

    public void createBox(float x, float y, float rotation) {
        updates.add(() -> GameObjectUtils.createObject(
                world, bodyLoader, "box", boxTexture, boxSize, GameObjectType.BOX, x, y, rotation, this));
    }

    public void executeObjectsUpdates() {
        while (!updates.isEmpty()) {
            updates.poll().run();
        }
    }

    public void dispose() {
        wallTexture.dispose();
        boxTexture.dispose();
    }
}
