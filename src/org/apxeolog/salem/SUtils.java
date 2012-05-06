package org.apxeolog.salem;

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
	
	public static class HighlightInfo {
		protected Tex resIcon;
		protected boolean highlight;
		protected String optName;
		protected String uniq;
		
		public HighlightInfo(String str) {
			uniq = str;
			String invobj = "gfx/invobjs/herbs/" + uniq;
			Resource base = Resource.load(invobj);
			base.loadwait();
			resIcon = base.layer(Resource.imgc).tex();
			optName = base.layer(Resource.tooltip).t;
			loadCFG();
		}
		
		public Tex getTex() {
			return resIcon;
		}
		
		public String getName() {
			return optName;
		}
		
		public void setBool(boolean val) {
			highlight = val;
			saveCFG();
		}
		
		public boolean getBool() {
			return highlight;
		}
		
		public void loadCFG() {
			Boolean val = HConfig.getValue("cl_hl_res_" + uniq, Boolean.class);
			highlight = val != null ? val : true;
		}
		
		public void saveCFG() {
			HConfig.addValue("cl_hl_res_" + uniq, highlight);
		}
		
		public boolean pass(Gob check) {
			return highlight;
		}
	}
	
	public static HashMap<String, HighlightInfo> mmapHighlightInfoCache;
	
	static {
		mmapHighlightInfoCache = new HashMap<String, SUtils.HighlightInfo>();
		// Precache known goods
		mmapHighlightInfoCache.put("arrowhead", new HighlightInfo("arrowhead"));
		mmapHighlightInfoCache.put("devilwort", new HighlightInfo("devilwort"));
		mmapHighlightInfoCache.put("honeysucklekudzu", new HighlightInfo("honeysucklekudzu"));
		mmapHighlightInfoCache.put("huckleberry", new HighlightInfo("huckleberry"));
		mmapHighlightInfoCache.put("indianfeather", new HighlightInfo("indianfeather"));
		mmapHighlightInfoCache.put("lobstermushroom", new HighlightInfo("lobstermushroom"));
		mmapHighlightInfoCache.put("newworldgourd", new HighlightInfo("newworldgourd"));
		mmapHighlightInfoCache.put("oakworth", new HighlightInfo("oakworth"));
		mmapHighlightInfoCache.put("oldlog", new HighlightInfo("oldlog"));
		mmapHighlightInfoCache.put("seashell", new HighlightInfo("seashell"));
		mmapHighlightInfoCache.put("smoothstone", new HighlightInfo("smoothstone"));
		mmapHighlightInfoCache.put("wildgarlic", new HighlightInfo("wildgarlic"));
		mmapHighlightInfoCache.put("witchshroom", new HighlightInfo("witchshroom"));
		mmapHighlightInfoCache.put("autumngrass", new HighlightInfo("autumngrass"));
		mmapHighlightInfoCache.put("bellpeppersgreen", new HighlightInfo("bellpeppersgreen"));
		mmapHighlightInfoCache.put("bellpeppersred", new HighlightInfo("bellpeppersred"));
		mmapHighlightInfoCache.put("blackberry", new HighlightInfo("blackberry"));
		mmapHighlightInfoCache.put("chestnut", new HighlightInfo("chestnut"));
		mmapHighlightInfoCache.put("coarsesalt", new HighlightInfo("coarsesalt"));
		mmapHighlightInfoCache.put("crowberry", new HighlightInfo("crowberry"));
		mmapHighlightInfoCache.put("driftwood", new HighlightInfo("driftwood"));
		mmapHighlightInfoCache.put("flint", new HighlightInfo("flint"));
		mmapHighlightInfoCache.put("lavenderblewit", new HighlightInfo("lavenderblewit"));
		mmapHighlightInfoCache.put("lilypad", new HighlightInfo("lilypad"));
		mmapHighlightInfoCache.put("lime", new HighlightInfo("lime"));
		mmapHighlightInfoCache.put("milkweed", new HighlightInfo("milkweed"));
		mmapHighlightInfoCache.put("seaweed", new HighlightInfo("seaweed"));
		mmapHighlightInfoCache.put("sugarcap", new HighlightInfo("sugarcap"));
		mmapHighlightInfoCache.put("virginiasnail", new HighlightInfo("virginiasnail"));
		mmapHighlightInfoCache.put("waxingtoadstool", new HighlightInfo("waxingtoadstool"));
	}
	
	public static Coord minimapMarkerRealCoords = null;
	public static Coord lastMinimapClickCoord = null;
	
	private static final ArrayList<Gob> gobSyncCache = new ArrayList<Gob>();
	private static final Coord minimapIconSize = new Coord(24, 24);

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
		synchronized (mv.ui.sess.glob.oc) {
			for (Gob gob : mv.ui.sess.glob.oc) {
				if (gob.resname().lastIndexOf("/") <= 0) continue;
				String lastPart = gob.resname().substring(gob.resname().lastIndexOf("/") + 1);
				HighlightInfo info = mmapHighlightInfoCache.get(lastPart);
				if (info != null) {
					if (info.pass(gob)) {
						gobSyncCache.add(gob);
					}
				}
			}
		}
		// Draw curios
		for (Gob gob : gobSyncCache) {
			try {
				if (gob.getc() != null) {
					Coord ul = mmap.realToLocal(new Coord(gob.getc()));
					if (!ul.isect(minimapIconSize, mmap.sz.sub(minimapIconSize))) continue;
					
					String lastPart = gob.resname().substring(gob.resname().lastIndexOf("/"));
					Resource inventoryRes = Resource.load("gfx/invobjs/herbs" + lastPart);
					Tex tex = inventoryRes.layer(Resource.imgc).tex();
					
					if (tex != null) {
						g.chcolor(Color.BLACK);
						g.fellipse(ul, minimapIconSize.div(2));
						g.chcolor();
						g.image(tex, ul.sub(minimapIconSize.div(2)), minimapIconSize);
					}
				}
			} catch (Exception ex) {
				// WOOPS
			}
		}
		if (lastMinimapClickCoord != null) {
			for (int i = gobSyncCache.size() - 1; i >= 0; i--) {
				Gob gob = gobSyncCache.get(i);
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
}
