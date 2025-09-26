package com.gungame.world.objects.imaginary;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.assets.TextureManager;

import java.util.ArrayList;
import java.util.List;

public class GroundContainer implements Disposable {
    private final Vector2 pieceSize = new Vector2(3, 3);
    private final List<Vector2> positions = new ArrayList<>();

    public Vector2 getGrassSize() {
        return pieceSize;
    }

    public void createGrassPiece(float x, float y) {
        positions.add(new Vector2(x, y));
    }

    public void drawBatch(SpriteBatch batch) {
        positions.forEach(pos -> drawAt(batch, pos));
    }

    private void drawAt(SpriteBatch batch, Vector2 pos) {
        batch.draw(TextureManager.getRegion(TextureManager.AtlasType.LEVEL_1,"grass"), pos.x, pos.y, pieceSize.x, pieceSize.y);
    }

    @Override
    public void dispose() {
    }
}
