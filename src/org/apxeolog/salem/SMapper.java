package org.apxeolog.salem;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import haven.Coord;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

public class SMapper {
	private static SMapper instance = null;
	
	public static SMapper getInstance() {
		if (instance == null) instance = new SMapper();
		return instance;
	}
	
	protected File mapRootDirectory;
	protected File currentSessionDirectory;
	protected Coord currentSessionStartGrid = null;
	protected Coord lastPlayerRealCoords = Coord.z;
	
	private SMapper() {
		mapRootDirectory = new File("map");
		if (!mapRootDirectory.exists()) mapRootDirectory.mkdir();
	}
	
	public void checkMapperSession(Coord playerRealCoords) {
		if (playerRealCoords.dist(lastPlayerRealCoords) > tilesz.x * 10) startNewSession(playerRealCoords);
	}
	
	public void startNewSession(Coord playerRealCoords) {
		currentSessionStartGrid = playerRealCoords.div(tilesz).div(cmaps);
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
	
	public synchronized void dumpMinimap(BufferedImage img, Coord gridCoord) {
		if (currentSessionStartGrid == null) return;
		try {
			File outputFile = new File(currentSessionDirectory, 
					String.format("tile_%d_%d.png", 
					gridCoord.sub(currentSessionStartGrid).x,
					gridCoord.sub(currentSessionStartGrid).y));
			ImageIO.write(img, "PNG", outputFile);
		} catch (IOException ex) {

		}
	}
	
	public static String getCurrentDateTimeString(long session) {
		return (new SimpleDateFormat("yyyy-MM-dd HH.mm.ss")).format(new Date(session));
	}
}
