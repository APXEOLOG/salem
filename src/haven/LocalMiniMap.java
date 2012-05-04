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

import haven.MCache.Grid;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apxeolog.salem.ALS;
import org.apxeolog.salem.HConfig;
import org.apxeolog.salem.SMapper;
import org.apxeolog.salem.SUtils;

import com.sun.corba.se.impl.ior.ByteBuffer;

public class LocalMiniMap extends Widget {
	public final MapView mv;
	
	protected boolean isPressed = false;
	public boolean requestedMarkerSet = false;
	protected boolean dragMode = false;
	
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

	public void draw(GOut og) {
		Gob pl = ui.sess.glob.oc.getgob(mv.plgob);
		if (pl == null) return;
		
		SMapper.getInstance().checkMapperSession(pl.rc, ui.sess.glob.map);
		
		Coord hsz = sz.div(scale);
		Coord translatedTile = pl.rc.div(tilesz).sub(mapCenterTranslation); // Center
		final Coord centralGrid = translatedTile.div(cmaps);
		Coord numGrids = hsz.div(cmaps).add(1, 1);
		Coord upperLeftGrid = centralGrid.sub(numGrids.div(2));
		
		GOut g = og.reclip(og.ul.mul((1 - scale) / scale), hsz);
		g.gl.glPushMatrix();
		g.scale(scale);
		
		for (int i = 0; i < numGrids.x; i++) {
			for (int j = 0; j < numGrids.y; j++) {
				Coord curGrid = upperLeftGrid.add(i, j);
				MapTile tile = null;
				if ((tile = SMapper.getInstance().getCachedTile(curGrid)) != null) {
					Coord c = tile.ul.mul(cmaps).sub(translatedTile).add(hsz.div(2));
					g.image(tile.img, c);
				} else {
					SMapper.getInstance().dumpMinimap(curGrid);
				}
			}
		}
		g.gl.glPopMatrix();
		try {
			SUtils.drawMinimapGob(og, mv, translatedTile, hsz);
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
					ptc = ptc.div(tilesz).sub(translatedTile).add(hsz.div(2));
					g.chcolor(m.col.getRed(), m.col.getGreen(),
							m.col.getBlue(), 255);
					g.image(MiniMap.plx.layer(Resource.imgc).tex(),
							ptc.add(MiniMap.plx.layer(Resource.negc).cc.inv()));
					g.chcolor();
				}
			}
		} catch (Loading l) {
		}
		super.draw(og);
	}
	

	
	public boolean mousedown(Coord c, int button) {
		parent.setfocus(this);
		raise();

		if (super.mousedown(c, button))
			return true;

		isPressed = true;
		if (button == 1) {
			ui.grabmouse(this);
			dragMode = true;
			dragOffset = c;
			sizeBuffer = sz;
		}
		return true;
	}

	public boolean mouseup(Coord c, int button) {
		if (isPressed) {
			if (c.isect(Coord.z, sz))
				click(c, button);
			isPressed = false;
		}
		if (dragMode) {
			ui.grabmouse(null);
			dragMode = false;
			transBuffer = mapCenterTranslation;
		} else {
			super.mouseup(c, button);
		}
		return true;
	}
	
	public void mousemove(Coord c) {
		if (dragMode) {
			mapCenterTranslation = transBuffer.add(c.add(dragOffset.inv()));
			//sz = szbuf.add(c.add(doff.inv()));
			//parent.resize(sz);
		} else {
			super.mousemove(c);
		}
	}
	
	@Override
	public boolean mousewheel(Coord c, int amount) {
		scale += -((double)amount / 10);
		return true;
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
