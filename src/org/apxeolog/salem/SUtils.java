package org.apxeolog.salem;

import static haven.MCache.tilesz;
import haven.Coord;
import haven.GOut;
import haven.Gob;
import haven.LocalMiniMap;
import haven.MapView;
import haven.Resource;
import haven.Tex;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

public class SUtils {
	public static Coord minimapMarkerRealCoords = null;
	public static Coord lastMinimapClickCoord = null;
	
	private static final ArrayList<Gob> gobSyncCache = new ArrayList<Gob>();
	private static final Coord minimapIconSize = new Coord(24, 24);
	public static HashMap<String, Boolean> herbResourceNames = new HashMap<String, Boolean>();
	static {
		herbResourceNames.put("autumngrass", true);
		herbResourceNames.put("bellpeppersgreen", true);
		herbResourceNames.put("bellpeppersred", true);
		herbResourceNames.put("blackberry", true);
		herbResourceNames.put("chestnut", true);
		herbResourceNames.put("coarsesalt", true);
		herbResourceNames.put("crowberry", true);
		herbResourceNames.put("driftwood", true);
		herbResourceNames.put("flint", true);
		herbResourceNames.put("lavenderblewit", true);
		herbResourceNames.put("lilypad", true);
		herbResourceNames.put("lime", true);
		herbResourceNames.put("milkweed", true);
		herbResourceNames.put("oldlog", true);
		herbResourceNames.put("seaweed", true);
		herbResourceNames.put("sugarcap", true);
		herbResourceNames.put("waxingtoadstool", true);
	}
	/**
	 * Draw gobs on minimap
	 * @param g graphics out
	 * @param mv {@link MapView} linked to {@link LocalMiniMap}
	 * @param plt Player.RealCoord div tilesize
	 * @param sz Size of minimap widget
	 */
	public static void drawMinimapGob(GOut g, MapView mv, Coord plt, Coord sz) {
		gobSyncCache.clear();
		// Precache gobs and free sync block
		synchronized (mv.ui.sess.glob.oc) {
			for (Gob gob : mv.ui.sess.glob.oc) {
				if (gob.resname().lastIndexOf("/") <= 0) continue;
				String lastPart = gob.resname().substring(gob.resname().lastIndexOf("/") + 1);
				if (gob.resname().contains("gfx/terobjs/herbs/")) {
					Boolean use = herbResourceNames.get(lastPart);
					if (use != null && use == false) {
						continue;
					} else if (!lastPart.equals("herbs")){
						gobSyncCache.add(gob);
					}
				}
			}
		}
		// Draw curios
		for (Gob gob : gobSyncCache) {
			try {
				if (gob.getc() != null) {
					String lastPart = gob.resname().substring(gob.resname().lastIndexOf("/"));
					Resource inventoryRes = Resource.load("gfx/invobjs/herbs" + lastPart);
					Tex tex = inventoryRes.layer(Resource.imgc).tex();
					Coord ptc = new Coord(gob.getc()).div(tilesz).sub(plt).add(sz.div(2));
					if (tex != null) {
						g.chcolor(Color.BLACK);
						g.fellipse(ptc, minimapIconSize.div(2));
						g.chcolor();
						g.image(tex, ptc.sub(minimapIconSize.div(2)),
								minimapIconSize);
					}
					if (lastMinimapClickCoord != null) {
						if (lastMinimapClickCoord.isect(ptc.sub(minimapIconSize.div(2)), minimapIconSize)) {
							lastMinimapClickCoord = null;
							mv.parent.wdgmsg(mv, "click", mv.ui.mc, gob.rc, 3, 0, (int) gob.id, gob.rc, -1);
						}
					}
				}
			} catch (Exception ex) {
				// WOOPS
			}
		}
		// Draw minimap marker
		if (minimapMarkerRealCoords != null) {
			Coord markerMinimapMapCoord = strictInRect(minimapMarkerRealCoords.div(tilesz).sub(plt).add(sz.div(2)), minimapIconSize, sz.sub(minimapIconSize));
			g.chcolor(Color.BLACK);
			g.frect(markerMinimapMapCoord.sub(minimapIconSize), minimapIconSize);
			g.chcolor(Color.RED);
			g.fellipse(markerMinimapMapCoord.sub(minimapIconSize.div(2)), minimapIconSize.div(2));
			g.chcolor();
		}
	}
	
	public static void moveToRealCoords(MapView mv, Coord realCoord) {
		// wdgmsg("click", pc, mc, clickb, ui.modflags());
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
}
