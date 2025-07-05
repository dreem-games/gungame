package com.gungame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.gungame.world.GameWorld;
import com.gungame.world.GameWorldConfig;

public class GunGame extends ApplicationAdapter {

	private BitmapFont font;
	private SpriteBatch batch;
	private OrthographicCamera camera;
	private GameWorld gameWorld;
	private int heroScore = 0;
	private int hero2Score = 0;
	
	@Override
	public void create () {
		// create the camera and the SpriteBatch
		camera = new OrthographicCamera();
		camera.setToOrtho(false, GameWorldConfig.VERTICAL_SIZE, GameWorldConfig.HORIZONTAL_SIZE);
		batch = new SpriteBatch();
		font = new BitmapFont();

		gameWorld = new GameWorld();
		gameWorld.init(camera);
	}

	public void drawScore() {       //Счет игроков
		font.getData().setScale(.35f);
		font.draw(batch, String.format((" %d  /  %d " ), heroScore, hero2Score), camera.position.x - 10, camera.position.y - 21);
	}

	@Override
	public void render () {
		ScreenUtils.clear(1, 1, 1, 1);
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		gameWorld.render(batch, camera);
		drawScore();
		batch.end();
		if (gameWorld.isWorldToRestart) {
			if (gameWorld.getHero().isToDestroy()) {
				hero2Score++;
			} else if (gameWorld.getHero2().isToDestroy()) {
				heroScore++;
			}
			restart();
		}
	}

	public void restart() {
		gameWorld.dispose();
		gameWorld = new GameWorld();
		gameWorld.init(camera);
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		gameWorld.dispose();
	}
}
