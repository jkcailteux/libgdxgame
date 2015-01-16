package com.mygdx.game.controller;

/**
 * Created by jeffcailteux on 1/15/15.
 */

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.mygdx.game.model.Level;
import com.mygdx.game.model.Player;

public class WorldRenderer implements InputProcessor {

    private static final float UNIT_SCALE = 1/16f;
    private static final float RUNNING_FRAME_DURATION = 0.09f;

    private OrthogonalTiledMapRenderer renderer;
    private OrthographicCamera camera;

    private Level level;
    private Player player;

    private Batch spriteBatch;

    /* Textures for Player */
    private TextureRegion playerIdleLeft;
    private TextureRegion playerIdleRight;
    private TextureRegion playerJumpLeft;
    private TextureRegion playerJumpRight;

    private TextureRegion playerFrame;

    /* Animations for Player */
    private Animation walkLeftAnimation;
    private Animation walkRightAnimation;

    private boolean jumpingPressed;
    private long jumpPressedTime;
    private float TILEWIDTH;

    /* for debug rendering */
    ShapeRenderer debugRenderer = new ShapeRenderer();

    private Array<Rectangle> tiles;

    private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
        @Override
        protected Rectangle newObject () {
            return new Rectangle();
        }
    };

    public WorldRenderer() {

        level = new Level("level1.tmx");
        player = new Player();

        loadPlayerTextures();
        tiles = new Array<Rectangle>();
        Gdx.input.setInputProcessor(this);

        player.setPosition(new Vector2(16, 2));
        player.setWidth( UNIT_SCALE * playerIdleRight.getRegionWidth());
        player.setHeight( UNIT_SCALE * playerIdleRight.getRegionHeight());

        renderer = new OrthogonalTiledMapRenderer(level.getMap(), UNIT_SCALE);

        TILEWIDTH = level.getTileWidth() * UNIT_SCALE;

        spriteBatch = renderer.getBatch();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 30, 20);
        camera.update();

    }

    public void render (float delta) {

        debugRenderer.setProjectionMatrix(camera.combined);

        renderer.setView(camera);
        camera.position.x = player.getPosition().x;
        camera.update();

        renderer.render();

        drawPlayer();
        drawDebug();
        updatePlayer(delta);

        player.update(delta);

    }

    public void updatePlayer(float delta){
        if (delta == 0) return;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {

            player.getVelocity().x = -Player.MAX_VELOCITY;
            if (player.isGrounded()) {
                player.setState(Player.State.Walking);
            }
            player.setFacingRight(false);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {

            player.getVelocity().x = Player.MAX_VELOCITY;
            if (player.isGrounded()) {
                player.setState(Player.State.Walking);
            }
            player.setFacingRight(true);
        }


        if(player.getState() != Player.State.Falling){
            if(player.getVelocity().y < 0){
                player.setState(Player.State.Falling);
                player.setGrounded(false);

            }
        }

        player.getAcceleration().y = Player.GRAVITY;
        player.getAcceleration().scl(delta);
        player.getVelocity().add(player.getAcceleration().x, player.getAcceleration().y);

        // clamp the velocity to the maximum, x-axis only
        if (Math.abs(player.getVelocity().x) > Player.MAX_VELOCITY) {
            player.getVelocity().x = Math.signum(player.getVelocity().x) * Player.MAX_VELOCITY;
        }

        // clamp the velocity to 0 if it's < 1, and set the state to standing
        if (Math.abs(player.getVelocity().x) < 1) {
            player.getVelocity().x = 0;
            if (player.isGrounded()) {
                player.setState(Player.State.Standing);
            }
        }

        player.getVelocity().scl(delta);

        // perform collision detection & response, on each axis, separately
        // if the koala is moving right, check the tiles to the right of it's
        // right bounding box edge, otherwise check the ones to the left
        Rectangle playerRect = rectPool.obtain();
        playerRect.set(player.getPosition().x, player.getPosition().y + player.getHeight()*0.1f, player.getWidth(), player.getHeight());

        int startX, startY, endX, endY;
        if (player.getVelocity().x > 0) {
            startX = endX = (int)(player.getPosition().x + player.getWidth() + player.getVelocity().x);
        } else {
            startX = endX = (int)(player.getPosition().x + player.getVelocity().x);
        }

        startY = (int)(player.getPosition().y);
        endY = (int)(player.getPosition().y + player.getHeight());
        getTiles(startX, startY, endX, endY, tiles);

        playerRect.x += player.getVelocity().x;

        for (Rectangle tile : tiles) {

            if (playerRect.overlaps(tile)) {

                if(player.getVelocity().x > 0){
                    player.getPosition().x = tile.x - TILEWIDTH - TILEWIDTH * 0.40f;
                }else if(player.getVelocity().x < 0){
                    player.getPosition().x = tile.x + TILEWIDTH + TILEWIDTH * 0.05f;
                }

                player.getVelocity().x = 0;

                break;
            }
        }

        //playerRect.x = player.getPosition().x;
        playerRect.set(player.getPosition().x, player.getPosition().y, player.getWidth(), player.getHeight());

        // if the koala is moving upwards, check the tiles to the top of it's
        // top bounding box edge, otherwise check the ones to the bottom
        if (player.getVelocity().y > 0) {
            startY = endY = (int)(player.getPosition().y + player.getHeight() + player.getVelocity().y);
        } else {
            startY = endY = (int)(player.getPosition().y + player.getVelocity().y);
        }

        startX = (int)(player.getPosition().x);
        endX = (int)(player.getPosition().x + player.getWidth());
        getTiles(startX, startY, endX, endY, tiles);
        playerRect.y += player.getVelocity().y;
        for (Rectangle tile : tiles) {
            if (playerRect.overlaps(tile)) {
                // we actually reset the koala y-position here
                // so it is just below/above the tile we collided with
                // this removes bouncing :)
                if (player.getVelocity().y > 0) {
                    player.getVelocity().y = tile.y - player.getHeight();
                    // we hit a block jumping upwards, let's destroy it!
                    //					TiledMapTileLayer layer = (TiledMapTileLayer)level.getMap().getLayers().get(1);
                    //					layer.setCell((int)tile.x, (int)tile.y, null);
                } else {
                    player.getPosition().y = tile.y + tile.height;
                    // if we hit the ground, mark us as grounded so we can jump
                    player.setGrounded(true);
                }
                player.getVelocity().y = 0;
                break;
            }
        }
        rectPool.free(playerRect);

        // unscale the velocity by the inverse delta time and set
        // the latest position

        player.getPosition().add(player.getVelocity());

        player.getVelocity().scl(1 / delta);

        player.getVelocity().x *= Player.DAMPING;

    }

    public void loadPlayerTextures(){

        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("solbrain.pack"));

		/* Standing */
        playerIdleLeft = atlas.findRegion("1");

        playerIdleRight = new TextureRegion(playerIdleLeft);
        playerIdleRight.flip(true, false);

        TextureRegion[] walkLeftFrames = new TextureRegion[6];
        for (int i = 0; i < 6; i++) {
            walkLeftFrames[i] =  atlas.findRegion(((i+6)+""));
        }

        walkLeftAnimation = new Animation(RUNNING_FRAME_DURATION, walkLeftFrames);

        TextureRegion[] walkRightFrames = new TextureRegion[6];
        for (int i = 0; i < 6; i++) {
            walkRightFrames[i] = new TextureRegion(walkLeftFrames[i]);
            walkRightFrames[i].flip(true, false);
        }

        walkRightAnimation = new Animation(RUNNING_FRAME_DURATION, walkRightFrames);

        playerJumpLeft = atlas.findRegion("3");
        playerJumpRight = new TextureRegion(playerJumpLeft);
        playerJumpRight.flip(true, false);

    }

    private void getTiles (int startX, int startY, int endX, int endY, Array<Rectangle> tiles) {
        TiledMapTileLayer layer = (TiledMapTileLayer)level.getMap().getLayers().get(1);
        rectPool.freeAll(tiles);
        tiles.clear();
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell != null) {
                    Rectangle rect = rectPool.obtain();
                    rect.set(x, y, 1, 1);
                    tiles.add(rect);
                }
            }
        }
    }

    public void drawPlayer(){

        playerFrame = player.isFacingRight() ? playerIdleRight : playerIdleLeft;

        if(player.getState() == Player.State.Walking) {

            playerFrame = player.isFacingRight() ? walkRightAnimation.getKeyFrame(player.getStateTime(), true) : walkLeftAnimation.getKeyFrame(player.getStateTime(), true);

        } else if (player.getState() == Player.State.Jumping) {

            playerFrame = player.isFacingRight() ? playerJumpRight : playerJumpLeft;

        }else if (player.getState() == Player.State.Falling){

            playerFrame = player.isFacingRight() ? playerJumpRight : playerJumpLeft;

        }

        spriteBatch.begin();

        spriteBatch.draw(playerFrame, player.getPosition().x, player.getPosition().y, player.getWidth(), player.getHeight());

        spriteBatch.end();

    }

    public void drawDebug(){

        debugRenderer.begin(ShapeRenderer.ShapeType.Line);

        debugRenderer.setColor(new Color(0, 1, 0, 1));
        debugRenderer.rect(player.getPosition().x, player.getPosition().y + player.getHeight()*0.1f, player.getWidth(), player.getHeight());

        debugRenderer.end();

    }

    @Override
    public boolean keyDown(int keycode) {

        // check input and apply to velocity & state
        if (keycode == Input.Keys.SPACE && player.isGrounded() && player.getState() != Player.State.Falling) {
            if (!player.getState().equals(Player.State.Jumping)) {
                jumpingPressed = true;
                player.setGrounded(false);
                jumpPressedTime = System.currentTimeMillis();
                player.setState(Player.State.Jumping);
                player.getVelocity().y = Player.MAX_JUMP_SPEED;
            } else {

                if ((jumpingPressed && ((System.currentTimeMillis() - jumpPressedTime) >= Player.LONG_JUMP_PRESS))) {
                    jumpingPressed = false;
                } else {
                    if (jumpingPressed) {
                        player.getVelocity().y = Player.MAX_JUMP_SPEED;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {

        if(keycode == Input.Keys.SPACE && player.getState() == Player.State.Jumping){
            player.getVelocity().y -= Player.MAX_JUMP_SPEED/1.5f;
            player.setState(Player.State.Falling);
            jumpingPressed = false;
        }
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}

