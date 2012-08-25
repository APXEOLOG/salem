package org.apxeolog.salem;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import haven.Coord;
import haven.HackThread;
import haven.Loading;
import haven.LocalMiniMap.MapTile;
import haven.MCache;
import haven.MCache.Grid;
import haven.TexI;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;

import org.apxeolog.salem.config.XConfig;

public class SMapper {
	private static SMapper instance = null;

	public static SMapper getInstance() {
		if (instance == null) instance = new SMapper();
		return instance;
	}

	protected File mapRootDirectory;
	protected File currentSessionDirectory;
	protected MCache mapCache;

	protected Coord currentSessionStartGrid = null;
	protected Coord lastPlayerRealCoords = Coord.z;
	protected ArrayList<Coord> dumpedGrids;

	protected Thread mapperThread;

	protected ConcurrentLinkedQueue<Pair<Coord, Coord>> gridsToDump;
	protected HashMap<Coord, MapTile> cache;

	private SMapper() {
		mapRootDirectory = new File("map");
		if (!mapRootDirectory.exists()) mapRootDirectory.mkdir();
		dumpedGrids = new ArrayList<Coord>();
		gridsToDump = new ConcurrentLinkedQueue<Pair<Coord, Coord>>();
		cache = new HashMap<Coord, MapTile>();

		mapperThread = new Thread(HackThread.tg(), new Runnable() {
			@Override
			public void run() {
				while (true) {
					Pair<Coord, Coord> entry;
					while ((entry = gridsToDump.poll()) != null) {
						Grid gridToDump = null;
						BufferedImage img = null;

						while (gridToDump == null || img == null) {
							try {
								Coord div = Coord.z;
								synchronized (lastPlayerRealCoords) {
									div = entry.getFirst().sub(lastPlayerRealCoords.div(tilesz).div(cmaps)).abs();
								}
								if (div.x > 1 || div.y > 1) break;
								synchronized (mapCache) {
									gridToDump = mapCache.getgrid(entry.getFirst());
								}
								img = gridToDump.getGridImage();
							} catch (Loading ex) {
								gridToDump = null;
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
								}
							}
						}

						if (gridToDump == null || img == null) continue;

						if (XConfig.cl_dump_minimaps) {
							try {
								String imgName = String.format("tile_%d_%d.png",
										entry.getFirst().sub(entry.getSecond()).x,
										entry.getFirst().sub(entry.getSecond()).y);
								File outputFile = new File(currentSessionDirectory, imgName);
								ImageIO.write(img, "PNG", outputFile);
							} catch (IOException ex) {

							}
						}
						synchronized (cache) {
							cache.put(entry.getFirst(), new MapTile(new TexI(img), entry.getFirst(), Coord.z));
						}
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// Do nothing
					}
				}
			}
		}, "SMapper Thread"	);
		mapperThread.start();
	}

	public MapTile getCachedTile(Coord gridCoord) {
		synchronized (cache) {
			return cache.get(gridCoord);
		}
	}

	public boolean isGridDumped(Coord grid) {
		return dumpedGrids.contains(grid);
	}

	public void checkMapperSession(Coord playerRealCoords, MCache mCache) {
		if (playerRealCoords.dist(lastPlayerRealCoords) > tilesz.x * 100) startNewSession(playerRealCoords, mCache);
		synchronized (lastPlayerRealCoords) {
			lastPlayerRealCoords = playerRealCoords;
		}
	}

	public void startNewSession(Coord playerRealCoords, MCache mCache) {
		currentSessionStartGrid = playerRealCoords.div(tilesz).div(cmaps);
		synchronized (lastPlayerRealCoords) {
			lastPlayerRealCoords = playerRealCoords;
		}
		dumpedGrids.clear();
		synchronized (cache) {
			cache.clear();
		}
		mapCache = mCache;
		if(XConfig.cl_dump_minimaps) {
			String sessionName = getCurrentDateTimeString(System.currentTimeMillis());
			try {
				currentSessionDirectory = new File(mapRootDirectory, sessionName);
				if (!currentSessionDirectory.exists()) currentSessionDirectory.mkdir();
				FileWriter sessionFile = new FileWriter(new File(mapRootDirectory, "currentsession.js"));
				sessionFile.write("var currentSession = '" + sessionName + "';\n");
				sessionFile.close();
			} catch (Exception ex) {

			}
		}
	}

	public synchronized void dumpMinimap(Coord gridCoord) {
		if (currentSessionStartGrid == null) return;
		if (isGridDumped(gridCoord)) return;
		Coord div = gridCoord.sub(lastPlayerRealCoords.div(tilesz).div(cmaps)).abs();
		if (div.x > 1 || div.y > 1) return;
		dumpedGrids.add(gridCoord);
		gridsToDump.add(new Pair<Coord, Coord>(gridCoord, new Coord(currentSessionStartGrid)));
	}

	public static String getCurrentDateTimeString(long session) {
		return (new SimpleDateFormat("yyyy-MM-dd HH.mm.ss")).format(new Date(session));
	}
}
