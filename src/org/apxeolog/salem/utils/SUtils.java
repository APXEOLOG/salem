package org.apxeolog.salem.utils;

import haven.Composite;
import haven.Coord;
import haven.GOut;
import haven.GameUI;
import haven.Gob;
import haven.Loading;
import haven.LocalMiniMap;
import haven.MapView;
import haven.Resource;
import haven.Tex;
import haven.Widget;

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
import java.util.HashMap;

import org.apxeolog.salem.ALS;
import org.apxeolog.salem.Pair;
import org.apxeolog.salem.config.MinimapHighlightConfig;
import org.apxeolog.salem.config.MinimapHighlightConfig.HighlightInfo;

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

	public static class NullTooltip {

	}

	private static final NullTooltip nullTooltip = new NullTooltip();

	private static void disposeFromHighlightCache(long id) {
		MinimapMarker buf =  highlightCache.get(id);
		if (buf != null) {
			buf.unlink();
			highlightCache.remove(id);
		}
	}

	public static class MinimapMarker extends Widget {
		protected HighlightInfo hInfo = null;
		protected long id;

		protected boolean needDispose = false;
		protected boolean passed = false;

		public boolean check() {
			if (!passed || !hInfo.getBool()) {
				needDispose = true;
			} else {
				needDispose = false;
			}
			passed = false;
			return needDispose;
		}

		public MinimapMarker(Coord c, Coord sz, Widget parent, HighlightInfo info, Gob gob) {
			super(c, sz, parent);
			hInfo = info;
			id = gob.id;
		}

		protected long lastRedrawTime = 0;

		@Override
		public void draw(GOut g) {
			if (needDispose) {
				if (System.currentTimeMillis() - lastRedrawTime > 250) {
					disposeFromHighlightCache(id);
				}
			} else {
				lastRedrawTime = System.currentTimeMillis();
			}
			Gob gob = ui.sess.glob.oc.getgob(id);
			if (hInfo != null) {
				hInfo.draw(g, Coord.z, gob);
			}
		}

		@Override
		public Object tooltip(Coord c, Widget prev) {
			Gob gob = ui.sess.glob.oc.getgob(id);
			if (gob != null)
				return hInfo.getTooltip(gob);
			else return nullTooltip;
		}

		public void pass() {
			passed = true;
		}

		public void fail() {
			passed = false;
		}

		public void updateCoord(Coord nc) {
			c = nc.sub(hInfo.getSize().div(2));
		}

		protected boolean pressed = false;

		@Override
		public boolean mousedown(Coord c, int button) {
			if (button != 3) return super.mousedown(c, button);
			pressed = true;
			ui.grabmouse(this);
			return true;
		}

		@Override
		public boolean mouseup(Coord c, int button) {
			if (pressed && button == 3) {
				pressed = false;
				ui.grabmouse(null);
				if (c.isect(new Coord(0, 0), sz))
					click();
				return true;
			}
			return super.mouseup(c, button);
		}

		private void click() {
			if (!hInfo.allowClick()) return;
			GameUI gui = getparent(GameUI.class);
			Gob gob = ui.sess.glob.oc.getgob(id);
			if (gui != null && gob != null) {
				gui.map.parent.wdgmsg(gui.map, "click", gui.map.ui.mc, gob.rc, 3, 0, (int) gob.id, gob.rc, -1);
			}
		}
	}

	public static class CustomMinimapMarker extends Widget {
		protected static final Tex markerCache = Resource.loadtex("apx/gfx/mmap/map-marker-red");
		protected static final Coord markerSize = new Coord(16, 24);
		protected Coord realCoords = null;

		public CustomMinimapMarker(Widget parent, Coord rc) {
			super(Coord.z, markerSize, parent);
			realCoords = new Coord(rc);
		}

		@Override
		public void draw(GOut g) {
			g.image(markerCache, Coord.z, markerSize);
		}

		public Coord getSize() {
			return markerSize;
		}

		public Coord getRC() {
			return realCoords;
		}
	}


	private static final HashMap<Long, MinimapMarker> highlightCache = new HashMap<Long, MinimapMarker>();
	private static CustomMinimapMarker customMarker = null;

	/**
	 * Draw gobs on minimap
	 * @param g graphics out
	 * @param mv {@link MapView} linked to {@link LocalMiniMap}
	 * @param plt Player.RealCoord div tilesize
	 * @param sz Size of minimap widget
	 */
	public static void drawMinimapGob(GOut g, MapView mv, LocalMiniMap mmap) {
		String resname; Composite comp = null; HighlightInfo info = null;
		MinimapMarker bufMarker = null;
		synchronized (mv.ui.sess.glob.oc) {
			for (Gob gob : mv.ui.sess.glob.oc) {
				if (gob.id == mv.plgob) continue;
				try {
					Coord ul = mmap.realToLocal(new Coord(gob.getrc()));
					if (highlightCache.get(gob.id) == null) {
						comp = gob.getattr(Composite.class);
						if (comp != null) {
							resname = comp.resname();
						} else {
							resname = gob.resname();
						}
						info = MinimapHighlightConfig.getHighlightInfo(resname);
						if (info != null && info.getBool()) {
							Coord bufC = ul.sub(info.getSize().div(2));
							if (bufC.isect(info.getSize(), mmap.parent.sz.sub(info.getSize()))) {
								bufMarker = new MinimapMarker(bufC, info.getSize(), mmap, info, gob);
								highlightCache.put(gob.id, bufMarker);
							}
						}
					}
					bufMarker = highlightCache.get(gob.id);
					if (bufMarker != null) {
						bufMarker.updateCoord(ul);
						if (bufMarker.c.isect(bufMarker.sz, mmap.parent.sz.sub(bufMarker.sz))) {
							bufMarker.pass();
						}
					}
				} catch (Loading ex) {
					// skip
				}
			}
		}
		for (MinimapMarker marker : highlightCache.values()) {
			marker.check();
		}
		// Setup minimap marker
		if (customMarker != null) {
			Coord local = mmap.realToLocal(customMarker.getRC());
			Coord xlated = local.sub(customMarker.getSize().x / 2, customMarker.getSize().y);
			Coord markerMinimapMapCoord = strictInRect(xlated, customMarker.getSize(), mmap.sz.sub(customMarker.getSize()));
			customMarker.c = markerMinimapMapCoord;
		}
	}

	public static void addCustomMarker(Widget parent, Coord rc) {
		removeCustomMarkers();
		customMarker = new CustomMinimapMarker(parent, rc);
	}

	public static void removeCustomMarkers() {
		if (customMarker != null) {
			customMarker.unlink();
			customMarker = null;
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
