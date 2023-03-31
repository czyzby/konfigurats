package mj.konfigurats.logic.maps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;
import com.esotericsoftware.minlog.Log;

/**
 * Basic TMX map loader, based on {@link TmxMapLoader}.
 * Will not load textures - loads only objects, so that the map could
 * be used solely by the server. The map doesn't have to be disposed.
 */
public class MapLoader {
	private final FileHandleResolver fileResolver;
	private XmlReader xmlReader;
	private Element tmxMap;
	
	/**
	 * Creates a map loader with the default file resolver.
	 * The default map folder is set to "assets/maps/".
	 */
	public MapLoader() {
		fileResolver = new FileHandleResolver() {
			@Override
			public FileHandle resolve(String fileName) {
				try {
					// Creating a temporary file:
					File tempFile = File.createTempFile("tempmap", ".tmx");
					// Making sure the file will be deleted:
					tempFile.deleteOnExit();
					// Copying the TMX file (input stream) into the temporary file:
					Files.copy(this.getClass().getClassLoader()
						.getResourceAsStream("maps/"+fileName+".tmx"),
						tempFile.toPath(),StandardCopyOption.REPLACE_EXISTING);
					return new FileHandle(tempFile);
				}
				catch (IOException e) {
					Log.error("SRV: map "+fileName+" could not be loaded: "
						+e.getLocalizedMessage());
					return null;
				}
			}
		};
		
		xmlReader = new XmlReader();
	}
	
	/**
	 * Loads a single map .The map should be in the "assets/maps/" folder.
	 * Based on various {@link TmxMapLoader} methods.
	 * @param fileName map's name without TMX extension.
	 * @return tiled map with only the object layers.
	 */
	public TiledMap load(String fileName) {
		try {
			// Getting TMX file:
			tmxMap = xmlReader.parse(fileResolver.resolve(fileName));
			
			// Initiating an empty tiled map:
			TiledMap tiledMap = new TiledMap();
			
			// Getting basic properties:
			String mapOrientation = tmxMap.getAttribute("orientation", null);
			int mapWidth = tmxMap.getIntAttribute("width", 0);
			int mapHeight = tmxMap.getIntAttribute("height", 0);
			int tileWidth = tmxMap.getIntAttribute("tilewidth", 0);
			int tileHeight = tmxMap.getIntAttribute("tileheight", 0);
			String mapBackgroundColor = tmxMap.getAttribute("backgroundcolor", null);
			
			// Setting basic properties:
			MapProperties mapProperties = tiledMap.getProperties();
			if (mapOrientation != null) {
				mapProperties.put("orientation", mapOrientation);
			}
			mapProperties.put("width", mapWidth);
			mapProperties.put("height", mapHeight);
			mapProperties.put("tilewidth", tileWidth);
			mapProperties.put("tileheight", tileHeight);
			if (mapBackgroundColor != null) {
				mapProperties.put("backgroundcolor", mapBackgroundColor);
			}
			
			// Setting additional properties:
			Element properties = tmxMap.getChildByName("properties");
			if (properties != null) {
				loadProperties(tiledMap.getProperties(), properties);
			}
			
			// Reading object layers:
			for (int i=0, j=tmxMap.getChildCount(); i<j; i++) {
				Element element = tmxMap.getChild(i);
				if (element.getName().equals("objectgroup")) {
					loadObjectGroup(tiledMap, element);
				}
			}
			
			return tiledMap;
		} catch (Exception e) {
			Log.error("SRV: map "+fileName+" could not be loaded:"
				+e.getLocalizedMessage());
			System.exit(2);
			return null;
		}
	}
	
	/**
	 * Reads properties of a single map object.
	 * Works as in {@link TmxMapLoader}.
	 * @param properties map properties of the object.
	 * @param element TMX data.
	 */
	private void loadProperties (MapProperties properties, Element element) {
		if (element.getName().equals("properties")) {
			for (Element property : element.getChildrenByName("property")) {
				String name = property.getAttribute("name", null);
				String value = property.getAttribute("value", null);
				if (value == null) {
					value = property.getText();
				}
				properties.put(name, value);
			}
		}
	}
	
	/**
	 * Loads a single layer of objects.
	 * Works as in {@link TmxMapLoader}.
	 * @param element TMX data of the layer.
	 */
	private void loadObjectGroup(TiledMap map, Element element) {
		String name = element.getAttribute("name", null);
		MapLayer layer = new MapLayer();
		layer.setName(name);
		Element properties = element.getChildByName("properties");
		if (properties != null) {
			loadProperties(layer.getProperties(), properties);
		}

		for (Element objectElement : element.getChildrenByName("object")) {
			loadObject(layer, objectElement);
		}

		map.getLayers().add(layer);
	}

	/**
	 * Loads a single map object.
	 * Works as in {@link TmxMapLoader}.
	 * @param layer object's layer.
	 * @param element TMX data.
	 */
	private void loadObject (MapLayer layer, Element element) {
		if (element.getName().equals("object")) {
			MapObject object = null;

			float scaleX = 1.0f;
			float scaleY = 1.0f;

			float x = element.getIntAttribute("x", 0) * scaleX;
			float y = element.getIntAttribute("y", 0) * scaleY;

			float width = element.getIntAttribute("width", 0) * scaleX;
			float height = element.getIntAttribute("height", 0) * scaleY;

			if (element.getChildCount() > 0) {
				Element child = null;
				if ((child = element.getChildByName("polygon")) != null) {
					String[] points = child.getAttribute("points").split(" ");
					float[] vertices = new float[points.length * 2];
					for (int i = 0; i < points.length; i++) {
						String[] point = points[i].split(",");
						vertices[i * 2] = Integer.parseInt(point[0]) * scaleX;
						vertices[i * 2 + 1] = Integer.parseInt(point[1]) * scaleY;
					}
					Polygon polygon = new Polygon(vertices);
					polygon.setPosition(x, y);
					object = new PolygonMapObject(polygon);
				} else if ((child = element.getChildByName("polyline")) != null) {
					String[] points = child.getAttribute("points").split(" ");
					float[] vertices = new float[points.length * 2];
					for (int i = 0; i < points.length; i++) {
						String[] point = points[i].split(",");
						vertices[i * 2] = Integer.parseInt(point[0]) * scaleX;
						vertices[i * 2 + 1] = Integer.parseInt(point[1]) * scaleY;
					}
					Polyline polyline = new Polyline(vertices);
					polyline.setPosition(x, y);
					object = new PolylineMapObject(polyline);
				} else if ((child = element.getChildByName("ellipse")) != null) {
					object = new EllipseMapObject(x, y, width, height);
				}
			}
			if (object == null) {
				object = new RectangleMapObject(x, y, width, height);
			}
			object.setName(element.getAttribute("name", null));
			String type = element.getAttribute("type", null);
			if (type != null) {
				object.getProperties().put("type", type);
			}
			int gid = element.getIntAttribute("gid", -1);
			if (gid != -1) {
				object.getProperties().put("gid", gid);
			}
			object.getProperties().put("x", x * scaleX);
			object.getProperties().put("y", y * scaleY);
			object.setVisible(element.getIntAttribute("visible", 1) == 1);
			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(object.getProperties(), properties);
			}
			layer.getObjects().add(object);
		}
	}
}
