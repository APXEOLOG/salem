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

package org.apxeolog.salem;

import haven.CheckBox;
import haven.Coord;
import haven.GOut;
import haven.GameUI;
import haven.Label;
import haven.Loading;
import haven.Resource;
import haven.RichText;
import haven.Scrollbar;
import haven.Tabs;
import haven.Tabs.Tab;
import haven.WItem;
import haven.Widget;

import java.awt.font.TextAttribute;
import java.util.Arrays;
import java.util.Comparator;

import org.apxeolog.salem.SUtils.HighlightInfo;

public class SWidgetOptions extends SWindow {
	public static final RichText.Foundry foundry = new RichText.Foundry(
			TextAttribute.FAMILY, "SansSerif", TextAttribute.SIZE, 10);
	private Tabs body;

	@Override
	public void unlink() {
		HConfig.saveConfig();
		super.unlink();
	}
	
	public SWidgetOptions(Coord c, Widget parent) {
		super(c, new Coord(345, 300), parent, "Options");

		body = new Tabs(Coord.z, new Coord(345, 280), this);

		Tab tab, ftab;
		
		{ /* Highlight TAB */
			ftab = tab = body.new Tab(new Coord(10, 10), 70, "Highlight") {
				@Override
				public void draw(GOut g) {
					g.chcolor(255, 255, 255, 255);
					g.rect(Coord.z, sz.add(1, 1));
					super.draw(g);
				}
			};

			new Label(new Coord(20, 40), tab, "Highlight options:");
			final HideInfo hinfo = new HideInfo(new Coord(180, 60), new Coord(150, 200), tab);
			new HideList(new Coord(20, 60),	new Coord(150, 200), tab) {
				@Override
				protected void changed(HighlightInfo hl) {
					hinfo.setCurrent(hl);
				}
			};
		}
		GameUI gui = getparent(GameUI.class);
		{ /* Other TAB */
			tab = body.new Tab(new Coord(90, 10), 70, "General") {
				@Override
				public void draw(GOut g) {
					g.chcolor(255, 255, 255, 255);
					g.rect(Coord.z, sz.add(1, 1));
					super.draw(g);
				}
			};

			CheckBox checkb = new CheckBox(new Coord(20, 40), tab, "Toogle shadows") {
				@Override
				public void changed(boolean val) {
					GameUI gui = getparent(GameUI.class);
					if (gui != null) gui.setShadows(val);
				}
			};
			if (gui != null) checkb.set(gui.togglesdw);
			
			checkb = new CheckBox(new Coord(20, 80), tab, "Dump minimaps") {
				@Override
				public void changed(boolean val) {
					HConfig.cl_dump_minimaps = val;
					HConfig.saveConfig();
				}
			};
			checkb.set(HConfig.cl_dump_minimaps);
		}
		body.showtab(ftab);
	}
	
	public static class HideInfo extends Widget {
		protected HighlightInfo current;
		protected CheckBox curCheck;
		
		public HideInfo(Coord c, Coord sz, Widget parent) {
			super(c, sz, parent);
			curCheck = new CheckBox(new Coord(10, 90), this, "Visible");
			curCheck.hide();
		}
		
		public void setCurrent(HighlightInfo hl) {
			current = hl;
			curCheck.show();
			curCheck.a = hl.getBool();
		}
		
		@Override
		public void wdgmsg(Widget sender, String msg, Object... args) {
			if (sender == curCheck && msg.equals("ch")) {
				boolean val = (Boolean) args[0];
				current.setBool(val);
			} else super.wdgmsg(sender, msg, args);
		}
		
		@Override
		public void draw(GOut g) {
			g.chcolor(0, 0, 0, 255);
			g.frect(Coord.z, sz);
			g.chcolor(255, 255, 255, 255);
			g.rect(Coord.z, sz.add(1, 1));
			g.chcolor();
			if (current != null) {
				try {
					g.image(current.getTex(), new Coord(10, 10), new Coord(40, 40));
					g.atext(current.getName(), new Coord(60, 30), 0, 0);
				} catch (Loading e) {
					g.image(WItem.missing.layer(Resource.imgc).tex(), new Coord(10, 10), new Coord(40, 40));
					g.atext("...", new Coord(100, 30), 1, 1);
				}
			}
			super.draw(g);
		}
	}
	
	public static class HideList extends Widget {
		private int h;
		private Scrollbar sb;
		private int sel;
		public HighlightInfo[] hlList;
		
		private final Comparator<HighlightInfo> hlComparator = new Comparator<HighlightInfo>() {
			public int compare(HighlightInfo a, HighlightInfo b) {
				return (a.getName().compareTo(b.getName()));
			}
		};

		public HideList(Coord c, Coord sz, Widget parent) {
			super(c, sz, parent);
			h = sz.y / 20;
			sel = -1;
			sb = new Scrollbar(new Coord(sz.x, 0), sz.y, this, 0, 0);
			updateList();
		}
		
		public void updateList() {
			synchronized (SUtils.mmapHighlightInfoCache) {
				hlList = SUtils.mmapHighlightInfoCache.values().toArray(new HighlightInfo[SUtils.mmapHighlightInfoCache.size()]);
				Arrays.sort(hlList, hlComparator);
				sb.val = 0;
				sb.max = hlList.length - h;
				sel = -1;
			}
		}

		public void draw(GOut g) {
			g.chcolor(0, 0, 0, 255);
			g.frect(Coord.z, sz);
			g.chcolor(255, 255, 255, 255);
			g.rect(Coord.z, sz.add(1, 1));
			g.chcolor();
			
			for (int i = 0; i < h; i++) {
				if (i + sb.val >= hlList.length) continue;
				HighlightInfo hl = hlList[i + sb.val];
				if (i + sb.val == sel) {
					g.chcolor(255, 255, 0, 128);
					g.frect(new Coord(0, i * 20), new Coord(sz.x, 20));
					g.chcolor();
				}
				try {
					g.image(hl.getTex(), new Coord(0, i * 20), new Coord(20, 20));
					if (hl.getBool()) {
						g.chcolor(128, 255, 128, 255);
					} else {
						g.chcolor(255, 128, 128, 255);
					}
					g.atext(hl.getName(), new Coord(25, i * 20 + 10), 0, 0.5);
				} catch (Loading e) {
					g.image(WItem.missing.layer(Resource.imgc).tex(), new Coord(0, i * 20), new Coord(20, 20));
					g.atext("...", new Coord(25, i * 20 + 10), 0, 0.5);
				}
				g.chcolor();
			}
			super.draw(g);
		}

		public boolean mousewheel(Coord c, int amount) {
			sb.ch(amount);
			return (true);
		}

		public boolean mousedown(Coord c, int button) {
			if (super.mousedown(c, button))
				return (true);
			if (button == 1) {
				sel = (c.y / 20) + sb.val;
				if (sel >= hlList.length)
					sel = -1;
				changed((sel < 0) ? null : hlList[sel]);
				return (true);
			}
			return (false);
		}

		protected void changed(HighlightInfo hl) {
		}

		public void unsel() {
			sel = -1;
			changed(null);
		}
	}
}
