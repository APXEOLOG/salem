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

import static haven.ItemInfo.find;

import java.awt.Color;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;

import org.apxeolog.salem.ALS;

public class WItem extends Widget implements DTarget {
	public static final Resource missing = Resource.load("gfx/invobjs/missing");
	public final static RichText.Foundry ttfnd = new RichText.Foundry(
			TextAttribute.FAMILY, "SansSerif", TextAttribute.SIZE, 10);
	public final GItem item;
	private Tex mask = null;
	private Resource cmask = null;

	public static class ItemQualityComparator implements Comparator<WItem> {
		int desc = -100;

		@Override
		public int compare(WItem itema, WItem itemb) {
			return (int) (desc * (itema.getPurity() - itemb.getPurity()));
		}

		public ItemQualityComparator() {

		}

		public ItemQualityComparator(Boolean bdesc) {
			desc = (bdesc) ? 1 : -1;
		}
	}
	
	public WItem(Coord c, Widget parent, GItem item) {
		super(c, new Coord(30, 30), parent);
		this.item = item;
	}

	public void drawmain(GOut g, Tex tex) {
		g.image(tex, Coord.z);
	}

	public static BufferedImage rendershort(List<ItemInfo> info) {
		ItemInfo.Name nm = find(ItemInfo.Name.class, info);
		if (nm == null)
			return (null);
		BufferedImage img = nm.str.img;
		Alchemy ch = find(Alchemy.class, info);
		if (ch != null) {
			img = ItemInfo.catimgsh(5, img, ch.smallmeter());
			img = ItemInfo.catimgs(5, img, ttfnd.render(String.format(
					"Purity: $col[255,255,0]{%2.1f}  Multiplier: $col[255,255,0]{%2.1f}",
					ch.getPurity(), ch.getMultiplier())).img);
		}
		return (img);
	}

	public static BufferedImage shorttip(List<ItemInfo> info) {
		BufferedImage img = rendershort(info);
		ItemInfo.Contents cont = find(ItemInfo.Contents.class, info);
		if (cont != null) {
			BufferedImage rc = rendershort(cont.sub);
			if ((img != null) && (rc != null))
				img = ItemInfo.catimgs(0, img, rc);
			else if ((img == null) && (rc != null))
				img = rc;
		}
		if (img == null)
			return (null);
		return (img);
	}
	
	protected double getPurity() {
		List<ItemInfo> info = item.info();
		Alchemy ch = find(Alchemy.class, info);
		if (ch != null) {
			return ch.getPurity() * 100;
		}
		return -1;
	}
	
	protected int getDominantElement() {
		List<ItemInfo> info = item.info();
		Alchemy ch = find(Alchemy.class, info);
		if (ch != null) {
			return (int)(ch.getDominantElement());
		}
		return 0;
	}

	public static BufferedImage longtip(GItem item, List<ItemInfo> info) {
		BufferedImage img = ItemInfo.longtip(info);
		Alchemy ch = find(Alchemy.class, info);
		if (ch != null) {
			img = ItemInfo.catimgs(5, img, ttfnd.render(String.format(
					"Purity: $col[255,255,0]{%2.1f}  Multiplier: $col[255,255,0]{%2.1f}",
					ch.getPurity(), ch.getMultiplier())).img);
		}
		Resource.Pagina pg = item.res.get().layer(Resource.pagina);
		if (pg != null)
			img = ItemInfo.catimgs(0, img,
					RichText.render("\n" + pg.text, 200).img);
		return (img);
	}

	public BufferedImage longtip(List<ItemInfo> info) {
		return (longtip(item, info));
	}

	public class ItemTip implements Indir<Tex> {
		private final TexI tex;

		public ItemTip(BufferedImage img) {
			if (img == null)
				throw (new Loading());
			tex = new TexI(img);
		}

		public GItem item() {
			return (item);
		}

		public Tex get() {
			return (tex);
		}

		public void set(Tex val) {
			throw (new UnsupportedOperationException());
		}
	}

	public class ShortTip extends ItemTip {
		public ShortTip(List<ItemInfo> info) {
			super(shorttip(info));
		}
	}

	public class LongTip extends ItemTip {
		public LongTip(List<ItemInfo> info) {
			super(longtip(info));
		}
	}

	private long hoverstart;
	private ItemTip shorttip = null, longtip = null;
	private List<ItemInfo> ttinfo = null;

	public Object tooltip(Coord c, boolean again) {
		long now = System.currentTimeMillis();
		if (!again)
			hoverstart = now;
		try {
			List<ItemInfo> info = item.info();
			if (info != ttinfo) {
				shorttip = longtip = null;
				ttinfo = info;
			}
			if (now - hoverstart < 1000) {
				if (shorttip == null)
					shorttip = new ShortTip(info);
				return (shorttip);
			} else {
				if (longtip == null)
					longtip = new LongTip(info);
				return (longtip);
			}
		} catch (Loading e) {
			return ("...");
		}
	}

	private List<ItemInfo> olinfo = null;
	private Color olcol = null;

	private Color olcol() {
		try {
			List<ItemInfo> info = item.info();
			if (info != olinfo) {
				olcol = null;
				GItem.ColorInfo cinf = find(GItem.ColorInfo.class, info);
				if (cinf != null)
					olcol = cinf.olcol();
				olinfo = info;
			}
		} catch (Loading e) {
			return (null);
		}
		return (olcol);
	}

	private List<ItemInfo> numinfo = null;
	private Tex itemnum = null;

	private Tex itemnum() {
		try {
			List<ItemInfo> info = item.info();
			if (info != numinfo) {
				itemnum = null;
				GItem.NumberInfo ninf = find(GItem.NumberInfo.class, info);
				if (ninf != null)
					itemnum = new TexI(Utils.outline2(Text.render(
							Integer.toString(ninf.itemnum()), Color.WHITE).img,
							Utils.contrast(Color.WHITE)));
				numinfo = info;
			}
		} catch (Loading e) {
			return (null);
		}
		return (itemnum);
	}

	protected Text purityOutlinedText = null;
	
	public void draw(GOut g) {
		try {
			Resource res = item.res.get();
			Tex tex = res.layer(Resource.imgc).tex();
			drawmain(g, tex);
			if (item.num >= 0) {
				g.atext(Integer.toString(item.num), Coord.z, 0, 0);
			} else if (itemnum() != null) {
				g.aimage(itemnum(), Coord.z, 0, 0);
			}
			if (item.meter > 0) {
				double a = ((double) item.meter) / 100.0;
				int r = (int) ((1 - a) * 255);
				int gr = (int) (a * 255);
				int b = 0;
				g.chcolor(r, gr, b, 255);
				g.frect(new Coord(sz.x - 5, (int) ((1 - a) * sz.y)), new Coord(
						5, (int) (a * sz.y)));
				g.chcolor();
			}
			int purity = (int) Math.round(getPurity());
			//ALS.alDebugPrint(getPurity(), Math.round(getPurity()), purity);
			if (purity >= 0) {
				if (purityOutlinedText == null) {
					purityOutlinedText = Text.renderOutlined(Integer.toString(purity), Alchemy.colors[getDominantElement()].brighter().brighter(), Color.BLACK, 1);
				}
				g.aimage(purityOutlinedText.tex(), sz, 1, 1);
			}
			if (olcol() != null) {
				if (cmask != res) {
					mask = null;
					if (tex instanceof TexI)
						mask = ((TexI) tex).mkmask();
					cmask = res;
				}
				if (mask != null) {
					g.chcolor(olcol());
					g.image(mask, Coord.z);
					g.chcolor();
				}
			}
		} catch (Loading e) {
			missing.loadwait();
			g.image(missing.layer(Resource.imgc).tex(), Coord.z, sz);
		}
	}
	
	public boolean mousedown(Coord c, int btn) {
		if (btn == 1) {
			if (ui.modshift)
				item.wdgmsg("transfer", c);
			else if (ui.modctrl)
				item.wdgmsg("drop", c);
			else
				item.wdgmsg("take", c);
			return (true);
		} else if (btn == 3) {
			if (parent instanceof Inventory) {
				if (ui.modctrl && ui.modshift) {
					wdgmsg("transfer_such_all_ql", item.getResourceName());
				} else if (ui.modshift && ui.modmeta) {
					wdgmsg("transfer_such_all_qldesc", item.getResourceName());
				} else if (ui.modctrl) {
					wdgmsg("drop_such_all", item.getResourceName());
				} else if (ui.modshift) {
					wdgmsg("transfer_such_all", item.getResourceName());
				} else item.wdgmsg("iact", c);
			} else item.wdgmsg("iact", c);
			return (true);
		}
		return (false);
	}

	public boolean drop(Coord cc, Coord ul) {
		return (false);
	}

	public boolean iteminteract(Coord cc, Coord ul) {
		item.wdgmsg("itemact", ui.modflags());
		return (true);
	}
}
