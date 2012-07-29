package org.apxeolog.salem;

import haven.BuddyWnd;
import haven.Composite;
import haven.Coord;
import haven.GOut;
import haven.Gob;
import haven.KinInfo;
import haven.LocalMiniMap;
import haven.MapView;
import haven.Resource;
import haven.Tex;

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
	
	public static class HighlightInfo {
		protected Tex resIcon = null;
		protected boolean highlight = true;
		protected String optName;
		protected String uniq;
		
		public HighlightInfo() {
			
		}
		
		public HighlightInfo(String str) {
			uniq = str;
			String invobj = "gfx/invobjs/herbs/" + uniq;
			Resource base = Resource.load(invobj);
			base.loadwait();
			resIcon = base.layer(Resource.imgc).tex();
			optName = base.layer(Resource.tooltip).t;
			loadCFG();
		}
		
		public void draw(GOut g, Coord ul, Gob gob) {
			g.chcolor(Color.BLACK);
			g.fellipse(ul, minimapIconSize.div(2));
			g.chcolor();
			g.image(getTex(), ul.sub(minimapIconSize.div(2)), minimapIconSize);
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
	
	public static class AnimalHighlightInfo extends HighlightInfo {
		public AnimalHighlightInfo(String str, String tooltip) {
			uniq = str;
			String tex = "apx/gfx/mmap/icon-" + uniq;
			resIcon = Resource.loadtex(tex);
			optName = tooltip;
			loadCFG();
		}
		
		@Override
		public void draw(GOut g, Coord ul, Gob gob) {
//			// TODO Auto-generated method stub
//			super.draw(g, ul, gob);
			
			g.chcolor(Color.BLACK);
			g.fellipse(ul, minimapIconSize.div(2));
			g.chcolor();
			g.image(getTex(), ul.sub(minimapIconSize.div(2)), minimapIconSize);
		}
	}
	
	public static class DotHighlightInfo extends HighlightInfo {
		protected Color hlColor = new Color(150, 75, 0);
		
		public DotHighlightInfo(String str, String tooltip) {
			uniq = str;
			String tex = "apx/gfx/mmap/icon-bear";
			resIcon = Resource.loadtex(tex);
			optName = tooltip;
			loadCFG();
		}
		
		public DotHighlightInfo(String str, String tooltip, Color clr) {
			uniq = str;
			String tex = "apx/gfx/mmap/icon-bear";
			resIcon = Resource.loadtex(tex);
			optName = tooltip;
			hlColor = clr;
			loadCFG();
		}
		
		@Override
		public void draw(GOut g, Coord ul, Gob gob) {
//			// TODO Auto-generated method stub
//			super.draw(g, ul, gob);
			
			g.chcolor(Color.BLACK);
			g.fellipse(ul, new Coord(5, 5));
			g.chcolor(hlColor);
			g.fellipse(ul, new Coord(4 ,4));
		}
	}
	
	public static class PlayerHighlightInfo extends HighlightInfo {
		public PlayerHighlightInfo(String tooltip) {
			uniq = "player";
			String tex = "apx/gfx/mmap/icon-bear";
			resIcon = Resource.loadtex(tex);
			optName = tooltip;
			loadCFG();
		}
		
		@Override
		public void draw(GOut g, Coord ul, Gob gob) {
			int state = 0; // 2 - enemy | 0 - neutral | 1 - friend
			KinInfo kin = gob.getattr(KinInfo.class);
			if (kin != null) {
				state = kin.getGroup();
				if (kin.inYourVillage() && state == 0) state = 1;
			}
			
			if (!gob.glob.party.haveMember(gob.id)) {
				g.chcolor(Color.BLACK);
				g.fellipse(ul, new Coord(5, 5));
				g.chcolor(BuddyWnd.gc[state]);
				g.fellipse(ul, new Coord(4 ,4));
			}
		}
	}
	
	public static HashMap<String, HighlightInfo> mmapHighlightInfoCache;
	
	static {
		mmapHighlightInfoCache = new HashMap<String, SUtils.HighlightInfo>();
		// Add players
		mmapHighlightInfoCache.put("borka", new PlayerHighlightInfo("Player"));
		// Add animals
		mmapHighlightInfoCache.put("bear", new AnimalHighlightInfo("bear", "Bear"));
		mmapHighlightInfoCache.put("beaver", new AnimalHighlightInfo("beaver", "Beaver"));
		mmapHighlightInfoCache.put("cricket", new DotHighlightInfo("cricket", "Cricket"));
		mmapHighlightInfoCache.put("deer", new AnimalHighlightInfo("deer", "Deer"));
		
		mmapHighlightInfoCache.put("rabbit", new DotHighlightInfo("rabbit", "Rabbit", Color.DARK_GRAY));
		mmapHighlightInfoCache.put("rattler", new DotHighlightInfo("rattler", "Snake", Color.CYAN));
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
		//mmapHighlightInfoCache.put("bellpeppersgreen", new HighlightInfo("bellpeppersgreen"));
		//mmapHighlightInfoCache.put("bellpeppersred", new HighlightInfo("bellpeppersred"));
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
	
	private static final ArrayList<Pair<HighlightInfo, Gob>> gobSyncCache = new ArrayList<Pair<HighlightInfo, Gob>>();
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
				
				HighlightInfo info = mmapHighlightInfoCache.get(lastPart);
				if (info != null) {
					if (info.pass(gob)) {
						gobSyncCache.add(new Pair<SUtils.HighlightInfo, Gob>(info,gob));
					}
				}
			}
		}
		// Draw curios
		for (Pair<SUtils.HighlightInfo, Gob> pair : gobSyncCache) {
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
				if (gobSyncCache.get(i).getFirst() instanceof PlayerHighlightInfo || gobSyncCache.get(i).getFirst() instanceof AnimalHighlightInfo) continue;
				
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
		} catch (FileNotFoundException e) {
			// Just no save file
		} catch (IOException e) {
			// Some file error
		} catch (ClassNotFoundException e) {
			
		}
	}
}
