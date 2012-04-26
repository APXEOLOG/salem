/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apxeolog.salem.SUtils;

public class LocalMiniMap extends Widget {
	public final MapView mv;
	private MapTile cur = null;
	private final BufferedImage[] texes = new BufferedImage[256];
	private final Map<Coord, Defer.Future<MapTile>> cache = new LinkedHashMap<Coord, Defer.Future<MapTile>>(
			5, 0.75f, true) {
		protected boolean removeEldestEntry(
				Map.Entry<Coord, Defer.Future<MapTile>> eldest) {
			if (size() > 5) {
				try {
					MapTile t = eldest.getValue().get();
					t.img.dispose();
				} catch (RuntimeException e) {
				}
				return (true);
			}
			return (false);
		}
	};

	public static class MapTile {
		public final Tex img;
		public final Coord ul, c;

		public MapTile(Tex img, Coord ul, Coord c) {
			this.img = img;
			this.ul = ul;
			this.c = c;
		}
	}

	private BufferedImage tileimg(int t) {
		BufferedImage img = texes[t];
		if (img == null) {
			Resource r = ui.sess.glob.map.sets[t];
			if (r == null)
				return (null);
			Resource.Image ir = r.layer(Resource.imgc);
			if (ir == null)
				return (null);
			img = ir.img;
			texes[t] = img;
		}
		return (img);
	}

	public BufferedImage drawmap(Coord ul, Coord sz) {
		MCache m = ui.sess.glob.map;
		BufferedImage buf = TexI.mkbuf(sz);
		Coord c = new Coord();
		for (c.y = 0; c.y < sz.y; c.y++) {
			for (c.x = 0; c.x < sz.x; c.x++) {
				int t = m.gettile(ul.add(c));
				BufferedImage tex = tileimg(t);
				if (tex != null)
					buf.setRGB(c.x, c.y, tex.getRGB(
							Utils.floormod(c.x + ul.x, tex.getWidth()),
							Utils.floormod(c.y + ul.y, tex.getHeight())));
			}
		}
		for (c.y = 0; c.y < sz.y; c.y++) {
			for (c.x = 0; c.x < sz.x; c.x++) {
				int t = m.gettile(ul.add(c));
				if ((m.gettile(ul.add(c).add(-1, 0)) > t)
						|| (m.gettile(ul.add(c).add(1, 0)) > t)
						|| (m.gettile(ul.add(c).add(0, -1)) > t)
						|| (m.gettile(ul.add(c).add(0, 1)) > t))
					buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
			}
		}
		return (buf);
	}

	public LocalMiniMap(Coord c, Coord sz, Widget parent, MapView mv) {
		super(c, sz, parent);
		this.mv = mv;
	}

	public void draw(GOut g) {
		Gob pl = ui.sess.glob.oc.getgob(mv.plgob);
		if (pl == null)
			return;
		final Coord plt = pl.rc.div(tilesz);
		final Coord plg = plt.div(cmaps);
		if ((cur == null) || !plg.equals(cur.c)) {
			Defer.Future<MapTile> f;
			synchronized (cache) {
				f = cache.get(plg);
				if (f == null) {
					final Coord ul = plg.mul(cmaps).sub(cmaps).add(1, 1);
					f = Defer.later(new Defer.Callable<MapTile>() {
						public MapTile call() {
							return (new MapTile(new TexI(drawmap(ul,
									cmaps.mul(3).sub(2, 2))), ul, plg));
						}
					});
					cache.put(plg, f);
				}
			}
			if (f.done())
				cur = f.get();
		}
		if (cur != null) {
			GOut g2 = g.reclip(Window.swbox.tloff(),
					sz.sub(Window.swbox.bisz()));
			g2.image(cur.img, cur.ul.sub(plt).add(sz.div(2)));
			Window.swbox.draw(g, Coord.z, sz);
			try {
				
				SUtils.drawMinimapGob(g2, mv, plt, sz);
				
				synchronized (ui.sess.glob.party.memb) {
					for (Party.Member m : ui.sess.glob.party.memb.values()) {
						Coord ptc;
						try {
							ptc = m.getc();
						} catch (MCache.LoadingMap e) {
							ptc = null;
						}
						if (ptc == null)
							continue;
						ptc = ptc.div(tilesz).sub(plt).add(sz.div(2));
						g2.chcolor(m.col.getRed(), m.col.getGreen(),
								m.col.getBlue(), 255);
						g2.image(MiniMap.plx.layer(Resource.imgc).tex(), ptc
								.add(MiniMap.plx.layer(Resource.negc).cc.inv()));
						g2.chcolor();
					}
				}
				
			} catch (Loading l) {
			}
		}
	}
	
	protected boolean isPressed = false;
	public boolean requestedMarkerSet = false;
	
	public boolean mousedown(Coord c, int button) {
		isPressed = true;
		return true;
	}

	public boolean mouseup(Coord c, int button) {
		if (isPressed) {
			isPressed = false;
			if (c.isect(Coord.z, sz))
				click(c, button);
			return true;
		}
		return false;
	}
	
	public Coord localToReal(Coord local) {
		Gob pl = ui.sess.glob.oc.getgob(mv.plgob);
		if (pl == null) return Coord.z;
		return local.sub(sz.div(2)).add(pl.rc.div(tilesz)).mul(tilesz);
	}

	public void click(Coord c, int button) {
		if (button == 3) {
			// Activate curio click
			SUtils.lastMinimapClickCoord = c;
		} else if (button == 1) {
			if (requestedMarkerSet) {
				// Add marker
				requestedMarkerSet = false;
				SUtils.minimapMarkerRealCoords = localToReal(c);
			} else {
				// Minimap movement
				SUtils.moveToRealCoords(mv, localToReal(c));
			}
		}
	}
}
