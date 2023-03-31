package mj.konfigurats.logic.maps;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.utils.Array;

/**
 * Utility class containing all available maps. Do not initialize.
 * These tiled maps - unlike those in the client - don't need to be
 * disposed, as they don't have any actual resources linked to them.
 * @author MJ
 */
public class Maps {
	private Maps() {}
	
	// Array with all playable, non-utility maps.
	private static Array<MapInfo> MAPS = new Array<MapInfo>(MapInfo.values().length-2);
	
	/**
	 * Loads all maps from the TMX files. Should be run once.
	 */
	public static void createMaps() {
		MapLoader mapLoader = new MapLoader();
		for(MapInfo map : MapInfo.values()) {
			// Negative indexes are utility maps:
			if(map.getIndex() >= 0) {
				map.setLinkedMap(mapLoader.load(map.getMapName()));
				MAPS.add(map);
			}
		}
	}
	
	/**
	 * @return a random map.
	 */
	public static MapInfo getRandomMap() {
		return MAPS.random();
	}
	
	public static MapInfo getRandomMap(int currentMapIndex, int playersAmount) {
		MapInfo randomMap = MAPS.random();
		
		while(randomMap.getIndex() == currentMapIndex
			|| randomMap.getLimit() < playersAmount) {
			randomMap = MAPS.get((randomMap.getIndex() + 1)%MAPS.size);
		}
		
		return randomMap;
	}
	
	/**
	 * Contains informations about all available maps.
	 * @author MJ
	 */
	public static enum MapInfo {
		RANDOM(-2,"",0),
		RANDOM_CHANGING(-1,"",0),
		THE_EYE(0,"the_eye",12),
		SEPARATION(1,"separation",8),
		THE_PIT(2,"the_pit",10),
		CLOVER(3,"clover",12),
		DEVILS_TRAP(4,"devils_trap",12),
		CHESSBOARD(5,"chessboard",12),
		DESERT_MAYHEM(6,"desert_mayhem",8),
		CHAOS_DESERT(7,"chaos_desert",12),
		CAVES_OF_DESPAIR(8,"caves_of_despair",10),
		LAKE_OF_FIRE(9,"lake_of_fire",10),
		LEAP_OF_FAITH(10,"leap_of_faith",8);
		
		private final int index,limit;
		private final String mapName;
		private TiledMap linkedMap;
		private MapInfo(int index,String mapName,int limit) {
			this.index = index;
			this.mapName = mapName;
			this.limit = limit;
		}
		
		/**
		 * @return index of the map.
		 */
		public int getIndex() {
			return index;
		}
		
		/**
		 * @return file name of the map.
		 */
		public String getMapName() {
			return mapName;
		}
		
		/**
		 * @return tiled map with objects on the chosen map.
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
		 * @return map's player limit.
		 */
		public int getLimit() {
			return limit;
		}
		
		/**
		 * Finds a map connected with the chosen index and returns its info.
		 * @param index map index.
		 * @return map informations. Null for wrong index.
		 */
		public static MapInfo getMapInfo(int index) {
			for(MapInfo mapInfo : MapInfo.values()) {
				if(mapInfo.getIndex() == index) {
					return mapInfo;
				}
			}
			return null;
		}
	}
}
