package com.mygdx.game.model;

/**
 * Created by jeffcailteux on 1/15/15.
 */
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

public class Level {

    private TiledMap map;
    private float tileWidth;
    private float tileHeight;

    public Level(String tilemapName){

        map = new TmxMapLoader().load(tilemapName);
        TiledMapTileLayer layer = (TiledMapTileLayer)map.getLayers().get(0);
        tileWidth = layer.getTileWidth();
        tileHeight = layer.getTileHeight();

    }

    public TiledMap getMap() {
        return map;
    }

    public float getTileHeight() {
        return tileHeight;
    }

    public float getTileWidth() {
        return tileWidth;
    }
}
