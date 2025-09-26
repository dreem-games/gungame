package com.gungame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.gungame.assets.SoundManager;
import com.gungame.assets.TextureManager;
import com.gungame.world.GameWorld;
import com.gungame.world.GameWorldConfig;

public class GunGame extends ApplicationAdapter {

	private BitmapFont font;
	private SpriteBatch batch;
	private OrthographicCamera camera;
	private OrthographicCamera uiCamera;
	private GameWorld gameWorld;
	private int heroScore = 0;
	private int hero2Score = 0;
	
	@Override
	public void create () {
		float width = Gdx.graphics.getWidth();
		float height = Gdx.graphics.getHeight();
		float verticalSize = GameWorldConfig.HORIZONTAL_SIZE / width * height;

		camera = new OrthographicCamera();
		camera.setToOrtho(false, GameWorldConfig.HORIZONTAL_SIZE, verticalSize);

		uiCamera = new OrthographicCamera();
		uiCamera.setToOrtho(false, width, height);

		batch = new SpriteBatch();
		font = new BitmapFont();
		font.getData().setScale(5f);

		TextureManager.initHeroAndProjectiles();
		TextureManager.initLvlOne();
		TextureManager.initUi();
		SoundManager.loadAll();

		gameWorld = new GameWorld(verticalSize, GameWorldConfig.HORIZONTAL_SIZE);
		gameWorld.init(camera);
	}

	public void drawScore() {
		font.draw(batch, String.format((" %d  /  %d " ), heroScore, hero2Score), uiCamera.position.x - uiCamera.viewportWidth * 0.07f, uiCamera.position.y - uiCamera.viewportHeight * 0.44f);
	}

	@Override
	public void render () {
		ScreenUtils.clear(0, 0, 0, 1);
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		gameWorld.renderWorld(batch, camera);
		batch.end();

		uiCamera.update();
		batch.setProjectionMatrix(uiCamera.combined);
		batch.begin();
		gameWorld.renderUi(batch, uiCamera);
		drawScore();
		batch.end();

		if (gameWorld.isWorldToRestart) {
			if (gameWorld.getHero().isToDestroy()) {
				hero2Score++;
			}
			if (gameWorld.getHero2().isToDestroy()) {
				heroScore++;
			}
			restart();
		}
	}

	public void restart() {
		gameWorld.dispose();

		float width = Gdx.graphics.getWidth();
		float height = Gdx.graphics.getHeight();
		float verticalSize = GameWorldConfig.HORIZONTAL_SIZE / width * height;

		camera = new OrthographicCamera();
		camera.setToOrtho(false, GameWorldConfig.HORIZONTAL_SIZE, verticalSize);

		uiCamera = new OrthographicCamera();
		uiCamera.setToOrtho(false, width, height);
		gameWorld = new GameWorld(verticalSize, GameWorldConfig.HORIZONTAL_SIZE);
		gameWorld.init(camera);
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		gameWorld.dispose();
		SoundManager.dispose();
		TextureManager.dispose();
	}
}
