package com.gungame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.gungame.world.GameWorld;
import com.gungame.world.GameWorldConfig;

public class GunGame extends ApplicationAdapter {

	private SpriteBatch batch;
	private OrthographicCamera camera;
	private GameWorld gameWorld;
	
	@Override
	public void create () {
		// create the camera and the SpriteBatch
		camera = new OrthographicCamera();
		camera.setToOrtho(false, GameWorldConfig.VERTICAL_SIZE, GameWorldConfig.HORIZONTAL_SIZE);
		batch = new SpriteBatch();

		gameWorld = new GameWorld();
		gameWorld.init(camera);
	}

	@Override
	public void render () {
		ScreenUtils.clear(1, 1, 1, 1);
		camera.update();
		batch.setProjectionMatrix(camera.combined);

		batch.begin();
		gameWorld.render(batch, camera);
		batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		gameWorld.dispose();
	}
}
