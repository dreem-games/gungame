package com.gungame.world.ground;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

public class GroundContainer implements Disposable {
    private final Vector2 pieceSize = new Vector2(3, 3);
    private final Texture grassTexture = new Texture("texture/grass.jpg");
    private final List<Vector2> positions = new ArrayList<>();

    public Vector2 getGrassSize() {
        return pieceSize;
    }

    public void createGrassPiece(float x, float y) {
        positions.add(new Vector2(x, y));
    }

    public void draw(SpriteBatch batch) {
        for (var it : positions) {
            batch.draw(grassTexture, it.x, it.y, pieceSize.x, pieceSize.y);
        }
    }

    @Override
    public void dispose() {
        grassTexture.dispose();
    }
}
