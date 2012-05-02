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

import java.util.*;

import org.apxeolog.salem.ALS;

public class Inventory extends Widget implements DTarget {
	public static final Tex invsq = Resource.loadtex("gfx/hud/invsq");
	public static final Tex refl = Resource.loadtex("gfx/hud/invref");
	public static final Coord sqsz = new Coord(33, 33);
	Coord isz;
	Map<GItem, WItem> wmap = new HashMap<GItem, WItem>();

	static {
		Widget.addtype("inv", new WidgetFactory() {
			public Widget create(Coord c, Widget parent, Object[] args) {
				return (new Inventory(c, (Coord) args[0], parent));
			}
		});
	}

	public void draw(GOut g) {
		Coord c = new Coord();
		for (c.y = 0; c.y < isz.y; c.y++) {
			for (c.x = 0; c.x < isz.x; c.x++) {
				invsq(g, c.mul(sqsz));
			}
		}
		super.draw(g);
	}

	public Inventory(Coord c, Coord sz, Widget parent) {
		super(c,
				invsq.sz().add(new Coord(-1, -1)).mul(sz).add(new Coord(1, 1)),
				parent);
		isz = sz;
	}

	public static void invsq(GOut g, Coord c) {
		g.image(invsq, c);
		Coord ul = g.ul.sub(g.ul.div(2)).mod(refl.sz()).inv();
		Coord rc = new Coord();
		for (rc.y = ul.y; rc.y < c.y + invsq.sz().y; rc.y += refl.sz().y) {
			for (rc.x = ul.x; rc.x < c.x + invsq.sz().x; rc.x += refl.sz().x) {
				g.image(refl, rc, c.add(1, 1), invsq.sz().sub(2, 2));
			}
		}
	}

	public boolean mousewheel(Coord c, int amount) {
		if (amount < 0)
			wdgmsg("xfer", -1, ui.modflags());
		if (amount > 0)
			wdgmsg("xfer", 1, ui.modflags());
		return (true);
	}

	public Widget makechild(String type, Object[] pargs, Object[] cargs) {
		Coord c = (Coord) pargs[0];
		Widget ret = gettype(type).create(c, this, cargs);
		if (ret instanceof GItem) {
			GItem i = (GItem) ret;
			wmap.put(i, new WItem(c.mul(sqsz).add(1, 1), this, i));
		}
		return (ret);
	}

	public void cdestroy(Widget w) {
		super.cdestroy(w);
		if (w instanceof GItem) {
			GItem i = (GItem) w;
			ui.destroy(wmap.remove(i));
		}
	}

	public boolean drop(Coord cc, Coord ul) {
		wdgmsg("drop", ul.add(sqsz.div(2)).div(invsq.sz()));
		return (true);
	}

	public boolean iteminteract(Coord cc, Coord ul) {
		return (false);
	}

	public void uimsg(String msg, Object... args) {
		if (msg == "sz") {
			isz = (Coord) args[0];
			sz = invsq.sz().add(new Coord(-1, -1)).mul(isz)
					.add(new Coord(1, 1));
		}
	}
	
	public void wdgmsg(Widget sender, String msg, Object... args) {
		for (Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
			if (wdg.visible && wdg instanceof WItem) {
				ALS.alDebugPrint(((WItem)wdg).item.getResourceName());
			}
		}
		if (msg.equals("drop_such_all")) {
			for (Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
				if (wdg.visible && wdg instanceof WItem) {
					WItem buf = (WItem) wdg;
					if ((buf.item.getResourceName().equals((String) args[0])))
							buf.item.wdgmsg(buf.item, "drop", Coord.z);
				}
			}
		} else if (msg.equals("transfer_such_all")) {
			for (Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
				if (wdg.visible && wdg instanceof WItem) {
					WItem buf = (WItem) wdg;
					if ((buf.item.getResourceName().equals((String) args[0])))
							buf.item.wdgmsg(buf.item, "transfer", Coord.z);
				}
			}
		} else if (msg.equals("transfer_such_all_ql")) {
			List<WItem> il = new ArrayList<WItem>();
			WItem.ItemQualityComparator comp = new WItem.ItemQualityComparator();
			for (Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
				if (wdg.visible && wdg instanceof WItem) {
					if (((WItem) wdg).item.getResourceName().equals((String) args[0]))
						il.add((WItem) wdg);
				}
			}
			Collections.sort(il, comp);
			for (int i = 0; i < il.size(); i++) {
				WItem buf = il.get(i);
				buf.item.wdgmsg(buf.item, "transfer", Coord.z);
			}
		} else if (msg.equals("transfer_such_all_qldesc")) {
			List<WItem> il = new ArrayList<WItem>();
			WItem.ItemQualityComparator comp = new WItem.ItemQualityComparator(
					true);
			for (Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
				if (wdg.visible && wdg instanceof WItem) {
					if (((WItem) wdg).item.getResourceName().equals((String) args[0]))
						il.add((WItem) wdg);
				}
			}
			Collections.sort(il, comp);
			for (int i = 0; i < il.size(); i++) {
				WItem buf = il.get(i);
				buf.item.wdgmsg(buf.item, "transfer", Coord.z);
			}
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}
}
