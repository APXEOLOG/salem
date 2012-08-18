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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.apxeolog.salem.SMapper;
import org.apxeolog.salem.config.XConfig;
import org.apxeolog.salem.utils.SUtils;

public class LocalMiniMap extends Widget {
	public final MapView mv;

	public static Tex gridImage = null;

	static {
		BufferedImage img = TexI.mkbuf(cmaps);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(0, 0, 0, 0));
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, img.getWidth(), img.getHeight());
		gridImage = new TexI(img);
	}

	protected boolean isPressed = false;
	public boolean requestedMarkerSet = false;
	protected boolean dragMode = false;
	protected boolean waitingForDrag = false;

	protected Coord mapCenterTranslation = Coord.z;
	protected Coord dragOffset = c;
	protected Coord sizeBuffer = sz;
	protected Coord transBuffer = Coord.z;

	protected double scale = 1.0;

	public static class MapTile {
		public final Tex img;
		public final Coord ul, c;

		public MapTile(Tex img, Coord ul, Coord c) {
			this.img = img;
			this.ul = ul;
			this.c = c;
		}
	}

	public LocalMiniMap(Coord c, Coord sz, Widget parent, MapView mv) {
		super(c, sz, parent);
		this.mv = mv;
	}

	public void resetScale() {
		scale = 1.0D;
	}

	public void resetOffset() {
		mapCenterTranslation = Coord.z;
		transBuffer = mapCenterTranslation;
		//Kerri
		//fixes dragging bug, when u starts drag and
		//minimap focus moved to last 'mouseup' position
	}

	@Override
	public void draw(GOut og) {
		Gob pl = ui.sess.glob.oc.getgob(mv.plgob);
		if (pl == null) return;

		SMapper.getInstance().checkMapperSession(pl.rc, ui.sess.glob.map);

		Coord scaledSize = sz.div(scale);

		Coord translatedTile = pl.rc.div(tilesz).sub(mapCenterTranslation); // Center
		final Coord centralGrid = translatedTile.div(cmaps);

		int startGridX = - (int) Math.ceil((double)(scaledSize.div(2).x) / cmaps.x);
		int startGridY = - (int) Math.ceil((double)(scaledSize.div(2).y) / cmaps.y);
		int numGridsX = (int) Math.ceil((double)(scaledSize.x) / cmaps.x) + 1;
		int numGridsY = (int) Math.ceil((double)(scaledSize.y) / cmaps.y) + 1;

		GOut g = og.reclipl(og.ul.mul((1 - scale) / scale), scaledSize);
		g.gl.glPushMatrix();
		g.scale(scale);
		for (int i = startGridX; i <= startGridX + numGridsX; i++) {
			for (int j = startGridY; j <= numGridsY + numGridsY; j++) {
				Coord curGrid = centralGrid.add(i, j);
				MapTile tile = null;
				if ((tile = SMapper.getInstance().getCachedTile(curGrid)) != null) {
					Coord c = tile.ul.mul(cmaps).sub(translatedTile).add(scaledSize.div(2));
					g.image(tile.img, c);
					if (XConfig.cl_minimap_show_grid) {
						g.image(gridImage, c);
					}
				} else {
					SMapper.getInstance().dumpMinimap(curGrid);
				}
			}
		}
		g.gl.glPopMatrix();

		SUtils.drawMinimapGob(og, mv, this);
		super.draw(og);

		try {

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
					ptc = realToLocal(ptc);
					og.chcolor(m.col.getRed(), m.col.getGreen(),
							m.col.getBlue(), 255);
					og.image(MiniMap.plx.layer(Resource.imgc).tex(),
							ptc.add(MiniMap.plx.layer(Resource.negc).cc.inv()));
					og.chcolor();
				}
			}
		} catch (Loading l) {
		}
	}



	@Override
	public boolean mousedown(Coord c, int button) {
		parent.setfocus(this);
		raise();

		if (super.mousedown(c, button))
			return true;

		isPressed = true;

		if (button == 1) {
			ui.grabmouse(this);
			waitingForDrag = true;
			dragOffset = c;
			sizeBuffer = sz;
		}
		return true;
	}

	@Override
	public boolean mouseup(Coord c, int button) {
		waitingForDrag = false;
		ui.grabmouse(null);
		if (dragMode) {
			dragMode = false;
			transBuffer = mapCenterTranslation;
		} else if (isPressed) {
			if (c.isect(Coord.z, sz))
				click(c, button);
			isPressed = false;
		} else {
			super.mouseup(c, button);
		}
		return true;
	}

	@Override
	public void mousemove(Coord c) {
		if (c.dist(dragOffset) > 10 && waitingForDrag) {
			waitingForDrag = false;
			dragOffset = c;
			dragMode = true;
		}
		if (dragMode) {
			mapCenterTranslation = transBuffer.add(c.add(dragOffset.inv()));
		} else {
			super.mousemove(c);
		}
	}

	@Override
	public boolean mousewheel(Coord c, int amount) {
		scale = Math.max(Math.min(scale - ((double)amount / 3), 2.0), 1.0);
		return true;
	}

	public Coord localToReal(Coord local) {
		Gob pl = ui.sess.glob.oc.getgob(mv.plgob);
		if (pl == null) return Coord.z;
		// MAGIC!~
		return local.sub(sz.div(2)).div(scale).add(pl.rc.div(tilesz).sub(mapCenterTranslation)).mul(tilesz);
	}

	public Coord realToLocal(Coord real) {
		Gob pl = ui.sess.glob.oc.getgob(mv.plgob);
		if (pl == null) return Coord.z;
		// MAGIC!~
		return sz.div(2).add(real.div(tilesz).sub(pl.rc.div(tilesz).sub(mapCenterTranslation)).mul(scale));
	}

	public void click(Coord c, int button) {
		if (button == 1) {
			if (requestedMarkerSet) {
				// Add marker
				requestedMarkerSet = false;
				SUtils.addCustomMarker(this, localToReal(c));
			} else {
				// Minimap movement
				SUtils.moveToRealCoords(mv, localToReal(c));
			}
		}
	}
}
