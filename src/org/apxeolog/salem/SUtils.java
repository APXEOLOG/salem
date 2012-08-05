package org.apxeolog.salem;

import haven.Composite;
import haven.Coord;
import haven.GOut;
import haven.Gob;
import haven.LocalMiniMap;
import haven.MapView;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apxeolog.salem.config.MinimapHighlightConfig;
import org.apxeolog.salem.config.MinimapHighlightConfig.CompositeHighlightInfo;
import org.apxeolog.salem.config.MinimapHighlightConfig.HighlightInfo;
import org.apxeolog.salem.config.MinimapHighlightConfig.PlayerHighlightInfo;

public class SUtils {
	public static void writeText(File f, String text) {
		try {
			if (!f.exists()) f.createNewFile();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
			writer.write(text);
			writer.close();
		} catch (UnsupportedEncodingException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public static Coord minimapMarkerRealCoords = null;
	public static Coord lastMinimapClickCoord = null;

	private static final ArrayList<Pair<HighlightInfo, Gob>> gobSyncCache = new ArrayList<Pair<HighlightInfo, Gob>>();
	public static final Coord minimapIconSize = new Coord(24, 24);

	/**
	 * Draw gobs on minimap
	 * @param g graphics out
	 * @param mv {@link MapView} linked to {@link LocalMiniMap}
	 * @param plt Player.RealCoord div tilesize
	 * @param sz Size of minimap widget
	 */
	public static void drawMinimapGob(GOut g, MapView mv, LocalMiniMap mmap) {
		gobSyncCache.clear();
		// Precache gobs and free sync block
		String resname, lastPart = "";
		synchronized (mv.ui.sess.glob.oc) {
			for (Gob gob : mv.ui.sess.glob.oc) {
				resname = gob.resname(); lastPart = "";
				if (resname.lastIndexOf("/") <= 0) {
					// Animals and players
					Composite comp = gob.getattr(Composite.class);
					if (comp != null) {
						resname = comp.resname();
						if (resname.contains("/kritter/")) {
							int index = resname.indexOf("/kritter/") + 9;
							lastPart = resname.substring(index, resname.indexOf('/', index + 1));
						} else if (resname.contains("/borka/")) {
							// Player
							lastPart = "borka";
						} else continue;
					} else continue;
				} else {
					lastPart = resname.substring(resname.lastIndexOf("/") + 1);
				}

				HighlightInfo info = MinimapHighlightConfig.getHighlightInfo(lastPart);
				if (info != null) {
					gobSyncCache.add(new Pair<HighlightInfo, Gob>(info,gob));
				}
			}
		}
		// Draw curios
		for (Pair<HighlightInfo, Gob> pair : gobSyncCache) {
			try {
				Coord ul = mmap.realToLocal(new Coord(pair.getSecond().getrc()));
				if (!ul.isect(minimapIconSize, mmap.sz.sub(minimapIconSize))) continue;

				pair.getFirst().draw(g, ul, pair.getSecond());
			} catch (Exception ex) {
				// WOOPS
			}
		}
		if (lastMinimapClickCoord != null) {
			for (int i = gobSyncCache.size() - 1; i >= 0; i--) {
				if (gobSyncCache.get(i).getFirst() instanceof PlayerHighlightInfo || gobSyncCache.get(i).getFirst() instanceof CompositeHighlightInfo) continue;

				Gob gob = gobSyncCache.get(i).getSecond();
				Coord ul = mmap.realToLocal(new Coord(gob.getc())).sub(minimapIconSize.div(2));
				if (lastMinimapClickCoord.isect(ul, minimapIconSize)) {
					lastMinimapClickCoord = null;
					mv.parent.wdgmsg(mv, "click", mv.ui.mc, gob.rc, 3, 0, (int) gob.id, gob.rc, -1);
					break;
				}
			}
		}
		// Draw minimap marker
		if (minimapMarkerRealCoords != null) {
			Coord markerMinimapMapCoord = strictInRect(mmap.realToLocal(minimapMarkerRealCoords), minimapIconSize, mmap.sz.sub(minimapIconSize));
			g.chcolor(Color.BLACK);
			g.frect(markerMinimapMapCoord.sub(minimapIconSize), minimapIconSize);
			g.chcolor(Color.RED);
			g.fellipse(markerMinimapMapCoord.sub(minimapIconSize.div(2)), minimapIconSize.div(2));
			g.chcolor();
		}
	}

	public static void moveToRealCoords(MapView mv, Coord realCoord) {
		mv.parent.wdgmsg(mv, "click", mv.ui.mc, realCoord, 1, 0);
	}

	public static Coord strictInRect(Coord pointCoord, Coord rectLeftTopCorner, Coord rectSize) {
		Coord returnCoord = new Coord(pointCoord);
		if (pointCoord.x < rectLeftTopCorner.x) returnCoord.x = rectLeftTopCorner.x;
		if (pointCoord.y < rectLeftTopCorner.y) returnCoord.y = rectLeftTopCorner.y;
		if (pointCoord.x > rectLeftTopCorner.x + rectSize.x) returnCoord.x = rectLeftTopCorner.x + rectSize.x;
		if (pointCoord.y > rectLeftTopCorner.y + rectSize.y) returnCoord.y = rectLeftTopCorner.y + rectSize.y;
		return returnCoord;
	}


	/* Logins */
	public static HashMap<String, Pair<String, String>> accounts;

	public static void _sa_add_data(String login, String password) {
		ALS.alDebugPrint("add");
		// Object[] { user.text, pass.text, savepass.a }
		if (!accounts.containsKey(login)) {
			accounts.put(login, new Pair<String, String>(login, password));
			_sa_save_data();
		}
	}

	public static void _sa_save_data() {
		try {
			File file = new File("data.bin");
			if (!file.exists()) {
				file.createNewFile();
			}
			FileOutputStream files = new FileOutputStream(file);
			ObjectOutputStream sstream = new ObjectOutputStream(files);
			sstream.writeObject(accounts);
			sstream.flush();
			sstream.close();
		} catch (FileNotFoundException e) {

		} catch (IOException e) {

		}
	}

	public static void _sa_delete_account(String name) {
		accounts.remove(name);
		_sa_save_data();
	}

	@SuppressWarnings("unchecked")
	public static void _sa_load_data() {
		if (accounts != null) return;
		accounts = new HashMap<String, Pair<String,String>>();
		try {
			FileInputStream file = new FileInputStream("data.bin");
			ObjectInputStream sstream = new ObjectInputStream(file);
			accounts = (HashMap<String, Pair<String, String>>) sstream.readObject();
			sstream.close();
		} catch (Exception e) {

		}
	}
}
