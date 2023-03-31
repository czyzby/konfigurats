package mj.konfigurats.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.maps.tiled.TiledMap;
import mj.konfigurats.Core;

public class Maps {
	private Maps() {}

	/**
	 * Schedules loading of all maps.
	 * Should be called once, along with the loading of the other assets.
	 */
	public static void loadMaps(AssetManager assetManager) {
		// Loading maps:
		for(MapInfo map : MapInfo.values()) {
			// Negative indexes are utility maps.
			if(map.getIndex() >= 0) {
				assetManager.load("game/maps/"+map.getFileName()+".tmx",TiledMap.class);
			}
		}
	}

	/**
	 * Assigns loaded map to the static references.
	 * Should be called once, after all assets are loaded.
	 */
	public static void createMaps() {
		// Assigning maps:
		for(MapInfo map : MapInfo.values()) {
			// Maps with negative indexes are utility ones:
			if(map.getIndex() >= 0) {
				map.setLinkedMap(((Core)Gdx.app.getApplicationListener())
					.getInterfaceManager().getAsset("game/maps/"
					+map.getFileName()+".tmx", TiledMap.class));
			}
		}
	}

	/**
	 * Disposes of the maps.
	 */
	public static void dispose() {
		for(MapInfo map : MapInfo.values()) {
			if(map.getLinkedMap() != null) {
				map.getLinkedMap().dispose();
			}
		}
	}

	/**
	 * Contains informations about all maps.
	 * Helps to select the correct map from the index given by the server.
	 * @author MJ
	 */
	public static enum MapInfo {
		RANDOM(-2,"Random (constant)",""),
		RANDOM_CHANGING(-1,"Random (changing)",""),
		THE_EYE(0,"The Eye (12)","the_eye"),
		SEPARATION(1,"Separation (8)","separation"),
		THE_PIT(2,"The Pit (10)","the_pit"),
		CLOVER(3,"Clover (12)","clover"),
		DEVILS_TRAP(4,"Devil's Trap (12)","devils_trap"),
		CHESSBOARD(5,"Chessboard (12)","chessboard"),
		DESERT_MAYHEM(6,"Desert Mayhem (8)","desert_mayhem"),
		CHAOS_DESERT(7,"Chaos Desert (12)","chaos_desert"),
		CAVES_OF_DESPAIR(8,"Caves of Despair (10)","caves_of_despair"),
		LAKE_OF_FIRE(9,"Lake of Fire (10)","lake_of_fire"),
		LEAP_OF_FAITH(10,"Leap of Faith (8)","leap_of_faith");

		private final int index;
		private final String name,fileName;
		private TiledMap linkedMap;
		private MapInfo(int index,String name,String fileName) {
			this.index = index;
			this.name = name;
			this.fileName = fileName;
		}

		/**
		 * @return map's index.
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * @return tiled map linked with the selected map data.
		 */
		public TiledMap getLinkedMap() {
			return linkedMap;
		}

		/**
		 * Sets the linked map, provided it doesn't exist yet.
		 * @param map new linked map.
		 */
		private void setLinkedMap(TiledMap map) {
			if(linkedMap == null) {
				linkedMap = map;
			}
		}

		/**
		 * @return file name of the map.
		 */
		public String getFileName() {
			return fileName;
		}

		@Override
		public String toString() {
			return name;
		}

		/**
		 * Finds a map with the given index.
		 * @param index map's index.
		 * @return map with the selected index. Null for invalid index.
		 */
		public static TiledMap getTiledMap(int index) {
			for(MapInfo mapInfo : MapInfo.values()) {
				if(mapInfo.getIndex() == index) {
					return mapInfo.getLinkedMap();
				}
			}
			return null;
		}
	}
}
