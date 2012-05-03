package org.apxeolog.salem;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import haven.Coord;
import haven.HackThread;
import haven.Loading;
import haven.MCache;
import haven.MCache.Grid;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;

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
	protected ConcurrentLinkedQueue<Pair<Coord, Coord>> gridsToDump;
	
	protected Thread mapperThread;
	
	private SMapper() {
		mapRootDirectory = new File("map");
		if (!mapRootDirectory.exists()) mapRootDirectory.mkdir();
		dumpedGrids = new ArrayList<Coord>();
		gridsToDump = new ConcurrentLinkedQueue<Pair<Coord, Coord>>();
		mapperThread = new Thread(HackThread.tg(), new Runnable() {
			@Override
			public void run() {
				while (true) {
					Pair<Coord, Coord> entry;
					while ((entry = gridsToDump.poll()) != null) {
						Grid gridToDump = null;
						BufferedImage img = null;
						synchronized (mapCache) {
							while (gridToDump == null) {
								try {
									gridToDump = mapCache.getgrid(entry.getKey());
									img = gridToDump.getGridImage();
								} catch (Loading ex) {
									gridToDump = null;
									try {
										Thread.sleep(100);
									} catch (InterruptedException e) {
										
									}
								}
							}
						}
						try {
							String imgName;
								imgName = String.format("tile_%d_%d.png", 
										entry.getKey().sub(entry.getValue()).x,
										entry.getKey().sub(entry.getValue()).y);
							
							File outputFile = new File(currentSessionDirectory, imgName);
							ImageIO.write(img, "PNG", outputFile);
						} catch (IOException ex) {

						}
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// Do nothing
					}
				}
			}
		});
		mapperThread.start();
	}
	
	public boolean isGridDumped(Coord grid) {
		return dumpedGrids.contains(grid);
	}
	
	public void checkMapperSession(Coord playerRealCoords, MCache mCache) {
		if (playerRealCoords.dist(lastPlayerRealCoords) > tilesz.x * 100) startNewSession(playerRealCoords, mCache);
		lastPlayerRealCoords = playerRealCoords;
	}
	
	public void startNewSession(Coord playerRealCoords, MCache mCache) {
		currentSessionStartGrid = playerRealCoords.div(tilesz).div(cmaps);
		mapCache = mCache;
		String sessionName = getCurrentDateTimeString(System.currentTimeMillis());
		try {
			currentSessionDirectory = new File(mapRootDirectory, sessionName);
			if (!currentSessionDirectory.exists()) currentSessionDirectory.mkdir();
			dumpedGrids.clear();
			FileWriter sessionFile = new FileWriter(new File(mapRootDirectory, "currentsession.js"));
			sessionFile.write("var currentSession = '" + sessionName + "';\n");
			sessionFile.close();
		} catch (Exception ex) {
			
		}
	}
	
	public synchronized void dumpMinimap(Coord gridCoord) {
		if (currentSessionStartGrid == null) return;
		if (isGridDumped(gridCoord)) return;

		dumpedGrids.add(gridCoord);
		gridsToDump.add(new Pair<Coord, Coord>(gridCoord, currentSessionStartGrid));
	}
	
	public static String getCurrentDateTimeString(long session) {
		return (new SimpleDateFormat("yyyy-MM-dd HH.mm.ss")).format(new Date(session));
	}
}
