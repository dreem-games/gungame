package com.gungame.world.walls;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class WallsEngine implements Disposable {

    private final Vector2 wallSize = new Vector2(30, 30);
    private final Vector2 boxSize = new Vector2(40, 40);
    private final Texture wallTexture = new Texture("texture/wall.png");
    private final Texture boxTexture = new Texture("texture/box.jpg");
    private final List<Sprite> walls = new ArrayList<>();
    private final List<Sprite> boxes = new ArrayList<>();

    public Vector2 getWallPieceSize() {
        return wallSize;
    }

    public Vector2 getBoxSize() {
        return boxSize;
    }

    public void createWallPiece(float x, float y) {
        walls.add(createSprite(wallTexture, x, y, 0));
    }

    public boolean createBox(float x, float y, float rotation) {
        Sprite sprite = createSprite(boxTexture, x, y, rotation);
        sprite.setCenter(x, y);
        if (getOverlapsWall(sprite.getBoundingRectangle()).isPresent()) {
            return false;
        }
        boxes.add(sprite);
        return true;
    }

    private Sprite createSprite(Texture texture, float x, float y, float rotation) {
        Sprite sprite = new Sprite(texture);
        sprite.setPosition(x, y);

        if (texture == boxTexture) {
            sprite.setSize(boxSize.x, boxSize.y);
        } else {
            sprite.setSize(wallSize.x, wallSize.y);
        }

        sprite.setOriginCenter();
        sprite.setRotation(rotation);
        return sprite;
    }

    public Stream<Sprite> allObjects() {
        return Stream.concat(walls.stream(), boxes.stream());
    }

    public void draw(SpriteBatch batch) {
        allObjects().forEach(it -> it.draw(batch));
    }

    public void dispose() {
        wallTexture.dispose();
        boxTexture.dispose();
    }

    // TODO: починить коллизии

    public Optional<Sprite> getOverlapsWall(Vector2 point) {
        return allObjects()
                .filter(it -> isCollision(it, point))
                .findAny();
    }

    public Optional<Sprite> getOverlapsWall(Rectangle rectangle) {
        return allObjects()
                .filter(it -> isCollision(it, rectangle))
                .findAny();
    }

    public Optional<Sprite> getOverlapsWall(Polygon polygon) {
        return allObjects()
                .filter(it -> isCollision(it, polygon))
                .findAny();
    }

    private boolean isCollision(Sprite sprite, Rectangle rectangle) {
        if (!sprite.getBoundingRectangle().overlaps(rectangle)) {
            return false;
        }
        Polygon rPoly = new Polygon(new float[] { 0, 0, rectangle.width, 0, rectangle.width,
                rectangle.height, 0, rectangle.height });
        rPoly.setPosition(rectangle.x, rectangle.y);
        return Intersector.overlapConvexPolygons(rPoly, new Polygon(sprite.getVertices()));
    }

    private boolean isCollision(Sprite sprite, Polygon polygon) {
        return Intersector.overlapConvexPolygons(polygon, new Polygon(sprite.getVertices()));
    }

    private boolean isCollision(Sprite sprite, Vector2 point) {
        if (sprite.getRotation() == 0) {
            return sprite.getBoundingRectangle().contains(point);
        }
        return new Polygon(Arrays.copyOf(sprite.getVertices(), 8)).contains(point);
    }
}
