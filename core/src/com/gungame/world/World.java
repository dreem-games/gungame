package com.gungame.world;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.world.ground.GroundEngine;
import com.gungame.world.ground.GroundGenerationUtils;
import com.gungame.world.walls.WallsEngine;
import com.gungame.world.walls.WallsGenerationUtils;

import java.util.Optional;

public class World implements Disposable {
    private final WallsEngine wallsEngine = new WallsEngine();
    private final GroundEngine groundEngine = new GroundEngine();

    private static final int HSize = 500;
    private static final int WSize = 1000;

    public void init() {
        WallsGenerationUtils.generateWalls(wallsEngine, 0, 0, WSize, HSize);
        float wallW = wallsEngine.getWallPieceSize().x, wallH = wallsEngine.getWallPieceSize().y;

        GroundGenerationUtils.generateGrass(groundEngine, wallW, wallH, WSize - wallW, HSize - wallH);
        WallsGenerationUtils.generateBoxes(wallsEngine, wallW, wallH, WSize - wallW, HSize - wallH, .4f);
    }

    @Override
    public void dispose() {
        wallsEngine.dispose();
        groundEngine.dispose();
    }

    public void draw(SpriteBatch batch) {
        groundEngine.draw(batch);
        wallsEngine.draw(batch);
    }

    public Optional<float[]> getOverlapsWall(float x, float y) {
        return wallsEngine.getOverlapsWall(new Vector2(x, y))
                .map(Sprite::getVertices);
    }
}
