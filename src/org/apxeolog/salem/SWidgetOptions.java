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

import haven.*;
import haven.CharWnd.Skill;

import java.awt.font.TextAttribute;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map.Entry;

public class SWidgetOptions extends SWindow {
	public static final RichText.Foundry foundry = new RichText.Foundry(
			TextAttribute.FAMILY, "SansSerif", TextAttribute.SIZE, 10);
	private Tabs body;

	public SWidgetOptions(Coord c, Widget parent) {
		super(c, new Coord(400, 340), parent, "Options");

		body = new Tabs(Coord.z, new Coord(400, 300), this);

		Widget tab;

		{ /* Highlight TAB */
			tab = body.new Tab(new Coord(0, 0), 60, "Highlight");

			int xc = 15, yc = 25;
			
			for (Entry<String, Boolean> entry : SUtils.herbResourceNames
					.entrySet()) {
				Resource resBuf = Resource.load("gfx/invobjs/herbs/"
						+ entry.getKey());
				if (resBuf == null)
					continue;

				CheckBox buf = new CheckBox(new Coord(xc, yc), tab,
						entry.getKey(), entry.getKey()) {
					public void changed(boolean val) {
						SUtils.herbResourceNames.put(additionalInfo, val);
						HConfig.addValue("mmap_show_" + additionalInfo, val);
						HConfig.saveConfig();
					}
				};
				Boolean bool = HConfig.getValue("mmap_show_" + entry.getKey(),
						Boolean.class);
				if (bool != null)
					buf.set(bool);

				yc += buf.sz.y;
				
				if (yc >= tab.sz.y) {
					xc += 100;
					yc = 25;
				}
			}
		}
	}
	
	public static class HideList extends Widget {
		private int h;
		private Scrollbar sb;
		private int sel;
		public Skill[] skills = new Skill[0];
		private boolean loading = false;
		private final Comparator<Skill> skcomp = new Comparator<Skill>() {
			public int compare(Skill a, Skill b) {
				String an, bn;
				try {
					an = a.res.get().layer(Resource.action).name;
				} catch (Loading e) {
					loading = true;
					an = "\uffff";
				}
				try {
					bn = b.res.get().layer(Resource.action).name;
				} catch (Loading e) {
					loading = true;
					bn = "\uffff";
				}
				return (an.compareTo(bn));
			}
		};

		public HideList(Coord c, Coord sz, Widget parent) {
			super(c, sz, parent);
			h = sz.y / 20;
			sel = -1;
			sb = new Scrollbar(new Coord(sz.x, 0), sz.y, this, 0, 0);
		}

		public void draw(GOut g) {
			if (loading) {
				loading = false;
				Arrays.sort(skills, skcomp);
			}
			g.chcolor(0, 0, 0, 255);
			g.frect(Coord.z, sz);
			g.chcolor();
			for (int i = 0; i < h; i++) {
				if (i + sb.val >= skills.length)
					continue;
				Skill sk = skills[i + sb.val];
				if (i + sb.val == sel) {
					g.chcolor(255, 255, 0, 128);
					g.frect(new Coord(0, i * 20), new Coord(sz.x, 20));
					g.chcolor();
				}
				int astate = sk.afforded();
				if (astate == 3)
					g.chcolor(255, 128, 128, 255);
				else if (astate == 2)
					g.chcolor(255, 192, 128, 255);
				else if (astate == 1)
					g.chcolor(255, 255, 128, 255);
				try {
					g.image(sk.res.get().layer(Resource.imgc).tex(), new Coord(
							0, i * 20), new Coord(20, 20));
					g.atext(sk.res.get().layer(Resource.action).name,
							new Coord(25, i * 20 + 10), 0, 0.5);
				} catch (Loading e) {
					WItem.missing.loadwait();
					g.image(WItem.missing.layer(Resource.imgc).tex(),
							new Coord(0, i * 20), new Coord(20, 20));
					g.atext("...", new Coord(25, i * 20 + 10), 0, 0.5);
				}
				g.chcolor();
			}
			super.draw(g);
		}

		public void pop(Collection<Skill> nsk) {
			Skill[] skills = nsk.toArray(new Skill[0]);
			sb.val = 0;
			sb.max = skills.length - h;
			sel = -1;
			this.skills = skills;
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
				if (sel >= skills.length)
					sel = -1;
				changed((sel < 0) ? null : skills[sel]);
				return (true);
			}
			return (false);
		}

		protected void changed(Skill sk) {
		}

		public void unsel() {
			sel = -1;
			changed(null);
		}
	}
}
