/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Bj√∂rn Johannessen <johannessen.bjorn@gmail.com>
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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apxeolog.salem.HConfig;
import org.apxeolog.salem.SWindow;

public class CharWnd extends SWindow {
	public static final Map<String, String> attrnm;
	public static final List<String> attrorder;
	private String watchingAttr = "LOFTAR_TI_MENYA_ZAEBAL_SVOEI_HYINEI";
	public final Map<String, Attr> attrs = new HashMap<String, Attr>();
	public final SkillList csk, nsk;
	private final SkillInfo ski;

	@Override
	public void savePosition() {
		try {
			HConfig.addValue("swnd_pos_Character", c);
			HConfig.saveConfig();
		} catch (NullPointerException ex) {
		}
	}
	
	static class CharWndWindowHeader extends SWindowHeader {
		protected int meterPercent = -1;
		protected int futurePercent = 0;
		
		protected int currentLp = 0;
		protected int attLvl = 0;
		protected int willLp = 0;
		protected String name = "";
		protected String shortName = "";

		protected Color meterColor = Color.BLACK;
		protected Color futureColor = Color.BLACK;
		protected Object tooltipCache = null;
		
		public CharWndWindowHeader(Coord c, Coord sz, Widget parent,
				String caption, boolean min, boolean clo) {
			super(c, sz, parent, caption, min, clo);
		}
		
		private void buy() {
			parent.wdgmsg("sattr", shortName);
		}
		
		public boolean mousedown(Coord c, int btn) {
			if(btn == 1 && ui.modctrl) {
				buy();
				return true;
			}
			
			return super.mousedown(c, btn);
		}
		
		public void freeTipCache() {
			tooltipCache = null;
		}
		
		public void setShortName(String n) {
			shortName = n;
		}
		
		public void setTipValues(int cl, int al, int wl, String name) {
			currentLp = cl;
			attLvl = al;
			willLp = wl;
			this.name = name;
			((SWindow)parent).resize();
		}
		
		public Object tooltip(Coord c, boolean again) {
			if(meterPercent < 0) return null;
			
			if (tooltipCache == null) {
				String tiptext = String.format("Watching %s (lvl %d)\nCurrent: %d\nWill got: %d\nNeed: %d\nCompleted: %d%%", 
						name, attLvl, currentLp, willLp, attLvl*100, meterPercent);
				tooltipCache = RichText.render(tiptext, 0).tex();
			}
			return tooltipCache;
		}
		
		public void setMeterColor(Color clr) {
			meterColor = clr;
		}
		
		public void setFutureColor(Color col) {
			futureColor = col;
		}
		
		public void setMeterValue(int percent) {
			meterPercent = percent;
		}
		
		public void setFutureValue(int perc) {
			futurePercent = perc;
		}
		
		@Override
		public void draw(GOut initialGL) {
			initialGL.chcolor(0, 0, 0, 255);
			initialGL.frect(headerBox.getBorderPosition(), textSize());
			super.draw(initialGL);
			if (headerBox.borderWidth != 0) {
				initialGL.chcolor(255, 255, 255, 255);
				initialGL.rect(headerBox.getBorderPosition(), textSize().add(1, 1));
			}
			if (meterPercent > 0) {
				initialGL.chcolor(meterColor);
				double width = textSize().x / 100.;
				width *= meterPercent;
				//Û ÏÂÌˇ ÓÚ ˝ÚËı ÚËÔÓ‚ ‰‡ÌÌ˚ı ƒ≈À≈Õ»≈
				Coord meterSize = new Coord(Math.max((int)width, 3), textSize().y);
				initialGL.frect(headerBox.getBorderPosition().add(1, 1), meterSize.sub(3, 3));
				initialGL.chcolor();
			}
			if(futurePercent > 0) {
				if(futurePercent > 100) futurePercent = 100;
				initialGL.chcolor(futureColor);
				double width = textSize().x/100.;
				width *= futurePercent;
				Coord meterSize = new Coord(Math.max((int)width, 3), textSize().y/2);
				initialGL.frect(headerBox.getBorderPosition().add(1, 1), meterSize.sub(3, 3));
				initialGL.chcolor();
			}
			if (headerText != null) {
				initialGL.image(headerText.img, headerBox.getContentPosition().add(4, -1));
			}
		}
	}//charwnd header
	
	static {
		Widget.addtype("chr", new WidgetFactory() {
			public Widget create(Coord c, Widget parent, Object[] args) {
				return (new CharWnd(c, parent));
			}
		});
		{
			final List<String> ao = new ArrayList<String>();
			Map<String, String> an = new HashMap<String, String>() {
				public String put(String k, String v) {
					ao.add(k);
					return (super.put(k, v));
				}
			};
			an.put("arts", "Arts & Crafts");
			an.put("cloak", "Cloak & Dagger");
			an.put("faith", "Faith & Wisdom");
			an.put("wild", "Frontier & Wilderness");
			an.put("nail", "Hammer & Nail");
			an.put("hung", "Hunting & Gathering");
			an.put("law", "Law & Lore");
			an.put("mine", "Mines & Mountains");
			an.put("pots", "Pots & Pans");
			an.put("fire", "Sparks & Embers");
			an.put("stock", "Stocks & Cultivars");
			an.put("spice", "Sugar & Spice");
			an.put("thread", "Thread & Needle");
			an.put("natp", "Natural Philosophy");
			an.put("perp", "Perennial Philosophy");
			attrnm = Collections.unmodifiableMap(an);
			attrorder = Collections.unmodifiableList(ao);
		}
	}

	public class Skill {
		public final String nm;
		public final Indir<Resource> res;
		public final String[] costa;
		public final int[] costv;

		private Skill(String nm, Indir<Resource> res, String[] costa,
				int[] costv) {
			this.nm = nm;
			this.res = res;
			this.costa = costa;
			this.costv = costv;
		}

		private Skill(String nm, Indir<Resource> res) {
			this(nm, res, new String[0], new int[0]);
		}

		public int afforded() {
			int ret = 0;
			for (int i = 0; i < costa.length; i++) {
				if (attrs.get(costa[i]).attr.comp * 100 < costv[i])
					return (3);
				if (attrs.get(costa[i]).sexp < costv[i])
					ret = Math.max(ret, 2);
				else if (attrs.get(costa[i]).hexp < costv[i])
					ret = Math.max(ret, 1);
			}
			return (ret);
		}
	}

	private static class SkillInfo extends RichTextBox {
		final static RichText.Foundry skbodfnd;
		Skill cur = null;
		boolean d = false;

		static {
			skbodfnd = new RichText.Foundry(java.awt.font.TextAttribute.FAMILY,
					"SansSerif", java.awt.font.TextAttribute.SIZE, 9);
			skbodfnd.aa = true;
		}

		public SkillInfo(Coord c, Coord sz, Widget parent) {
			super(c, sz, parent, "", skbodfnd);
		}

		public void tick(double dt) {
			if (d) {
				try {
					StringBuilder text = new StringBuilder();
					text.append("$img[" + cur.res.get().name + "]\n\n");
					text.append("$font[serif,16]{"
							+ cur.res.get().layer(Resource.action).name
							+ "}\n\n");
					int[] o = sortattrs(cur.costa);
					if (cur.costa.length > 0) {
						for (int i = 0; i < o.length; i++) {
							int u = o[i];
							text.append(attrnm.get(cur.costa[u]) + ": "
									+ cur.costv[u] + "\n");
						}
						text.append("\n");
					}
					text.append(cur.res.get().layer(Resource.pagina).text);
					settext(text.toString());
					d = false;
				} catch (Loading e) {
				}
			}
		}

		public void setsk(Skill sk) {
			d = (sk != null);
			cur = sk;
			settext("");
		}
	}

	public static int[] sortattrs(final String[] attrs) {
		Integer[] o = new Integer[attrs.length];
		for (int i = 0; i < o.length; i++)
			o[i] = new Integer(i);
		Arrays.sort(o, new Comparator<Integer>() {
			public int compare(Integer a, Integer b) {
				return (attrorder.indexOf(attrs[a.intValue()]) - attrorder
						.indexOf(attrs[b.intValue()]));
			}
		});
		int[] r = new int[o.length];
		for (int i = 0; i < o.length; i++)
			r[i] = o[i];
		return (r);
	}

	public static class SkillList extends Widget {
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

		public SkillList(Coord c, Coord sz, Widget parent) {
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

	public class Attr extends Widget {
		private final Coord imgc = new Coord(0, 1), nmc = new Coord(17, 1),
				vc = new Coord(137, 1), expc = new Coord(162, 0),
				expsz = new Coord(sz.x - expc.x, sz.y);
		public final String nm; // name
		public final Resource res; // res
		public final Glob.CAttr attr;
		public int sexp, hexp; // max / current
		public boolean av = false; // got it
		private Text rnm, rv, rexp;
		private int cv;
		private String longname;

		public boolean finished() {
			return av;
		}
		
		private Attr(String attr, Coord c, Widget parent) {
			super(c, new Coord(237, 15), parent);
			this.nm = attr;
			this.res = Resource.load("gfx/hud/skills/" + nm);
			this.res.loadwait();
			Resource.Pagina pag = this.res.layer(Resource.pagina);
			if (pag != null)
				this.tooltip = RichText.render(pag.text, 300);
			this.attr = ui.sess.glob.cattr.get(nm);
			this.longname = attrnm.get(attr);
			this.rnm = Text.render(longname);
		}
		
		public void drawAsWatching(boolean b) {
			if(b)
				rnm = Text.render(longname, Color.red);
			else
				rnm = Text.render(longname, Color.white);
		}

		public void draw(GOut g) {
			g.image(res.layer(Resource.imgc).tex(), imgc);
			g.image(rnm.tex(), nmc);
			if (attr.comp != cv)
				rv = null;
			if (rv == null)
				rv = Text.render(String.format("%d", cv = attr.comp));
			g.image(rv.tex(), vc);
			g.chcolor(0, 0, 0, 255);
			g.frect(expc, expsz);
			g.chcolor(64, 64, 64, 255);
			g.frect(expc.add(1, 1), new Coord(((expsz.x - 2) * sexp)
					/ (attr.comp * 100), expsz.y - 2));
			if (av)
				g.chcolor(0, (a == 1) ? 255 : 128, 0, 255);
			else
				g.chcolor(0, 0, 128, 255);
			g.frect(expc.add(1, 1), new Coord(((expsz.x - 2) * hexp)
					/ (attr.comp * 100), expsz.y - 2));
			if (ui.lasttip instanceof WItem.ItemTip) {
				GItem item = ((WItem.ItemTip) ui.lasttip).item();
				Inspiration insp = GItem.find(Inspiration.class, item.info());
				if (insp != null) {
					for (int i = 0; i < insp.attrs.length; i++) {
						if (insp.attrs[i].equals(nm)) {
							int w = ((expsz.x - 2) * insp.exp[i])
									/ (attr.comp * 100);
							if (w > expsz.x - 2) {
								w = expsz.x - 2;
								g.chcolor(255, 255, 0, 255);
							} else {
								g.chcolor(255, 192, 0, 255);
							}
							g.frect(expc.add(1, 1), new Coord(w, (expsz.y / 2)));
							break;
						}
					}
				}
			}
			if (nsk.sel >= 0) {
				Skill sk = nsk.skills[nsk.sel];
				for (int i = 0; i < sk.costa.length; i++) {
					if (sk.costa[i].equals(nm)) {
						int w = ((expsz.x - 2) * sk.costv[i])
								/ (attr.comp * 100);
						if (w > expsz.x - 2) {
							w = expsz.x - 2;
							g.chcolor(255, 0, 0, 255);
						} else {
							g.chcolor(128, 0, 0, 255);
						}
						g.frect(expc.add(1, expsz.y / 2), new Coord(w,
								(expsz.y / 2)));
						break;
					}
				}
			}
			g.chcolor();
			if (rexp == null)
				rexp = Text.render(String
						.format("%d/%d", sexp, attr.comp * 100));
			g.aimage(rexp.tex(), expc.add(expsz.x / 2, 1), 0.5, 0);
		}

		private int a = 0;

		public boolean mousedown(Coord c, int btn) {
			if ((btn == 1) && c.isect(expc, expsz)) {
				if(ui.modctrl) {
					if(!watchingAttr.equals(nm))
						watchingAttr = nm;
					else
						watchingAttr = "";
					return true;
				}
				if (av) {
					a = 1;
					ui.grabmouse(this);
				}
				return (true);
			}
			return (false);
		}

		public boolean mouseup(Coord c, int btn) {
			if ((btn == 1) && (a == 1)) {
				a = 0;
				ui.grabmouse(null);
				if (c.isect(expc, expsz))
					buy();
				return (true);
			}
			return (false);
		}

		public void buy() {
			CharWnd.this.wdgmsg("sattr", nm);
		}
	}

	public CharWnd(Coord c, Widget parent) {
		super(c, new Coord(620, 340), parent, "Character");
		windowHeader.unlink();
		windowHeader = new CharWndWindowHeader(Coord.z, Coord.z, this, "Character", true, true);
		resize();
		
		new Label(new Coord(0, 0), this, "Proficiencies:");
		int y = 30;
		for (String nm : attrorder) {
			this.attrs.put(nm, new Attr(nm, new Coord(0, y), this));
			y += 20;
		}
		new Label(new Coord(250, 0), this, "Skills:");
		new Label(new Coord(250, 30), this, "Current:");
		this.csk = new SkillList(new Coord(250, 45), new Coord(170, 120), this) {
			protected void changed(Skill sk) {
				if (sk != null)
					nsk.unsel();
				ski.setsk(sk);
			}
		};
		new Label(new Coord(250, 170), this, "Available:");
		this.nsk = new SkillList(new Coord(250, 185), new Coord(170, 120), this) {
			protected void changed(Skill sk) {
				if (sk != null)
					csk.unsel();
				ski.setsk(sk);
			}
		};
		new Button(new Coord(250, 310), 50, this, "Buy") {
			public void click() {
				if (nsk.sel >= 0) {
					CharWnd.this.wdgmsg("buy", nsk.skills[nsk.sel].nm);
				}
			}
		};
		this.ski = new SkillInfo(new Coord(430, 30), new Coord(190, 275), this);
	}
	
	@Override
	public void draw(GOut initialGL) {
		boolean haveWatching = false;
		for (Attr atr : attrs.values()) {
			if (atr.nm.equals(watchingAttr)) {
				((CharWndWindowHeader) windowHeader).setShortName(atr.nm);
				atr.drawAsWatching(true);
				int maxlp = atr.attr.comp * 100;
				int currentlp = atr.hexp;
				double op = maxlp/100.;
				double meterperc = currentlp/op;
				((CharWndWindowHeader) windowHeader).setMeterValue((int)meterperc);
				if(meterperc != 100)
					((CharWndWindowHeader) windowHeader).setMeterColor(new Color(0, 0, 255, 225));
				else
					((CharWndWindowHeader) windowHeader).setMeterColor(new Color(0, 255, 0, 225));
				if (ui.lasttip instanceof WItem.ItemTip) {
					GItem item = ((WItem.ItemTip) ui.lasttip).item();
					Inspiration insp = GItem.find(Inspiration.class, item.info());
					if (insp != null) {
						for (int i = 0; i < insp.attrs.length; i++) {
							if (insp.attrs[i].equals(atr.nm)) {
								int itemExp = insp.exp[i];
								itemExp += atr.sexp;
								double futureperc = (itemExp*1.)/op;
								((CharWndWindowHeader) windowHeader).setFutureValue((int)futureperc);
								if((int)futureperc > 100)
									((CharWndWindowHeader) windowHeader).setFutureColor(new Color(255, 255, 0));
								else
									((CharWndWindowHeader) windowHeader).setFutureColor(new Color(200, 200, 0));
								break;
							}
						}
					}
				} else {
					((CharWndWindowHeader) windowHeader).setFutureValue(0);
				}
				((CharWndWindowHeader) windowHeader).setTipValues(currentlp, atr.attr.comp, atr.sexp, attrnm.get(atr.nm));
				windowHeader.setText("Character ("+attrnm.get(atr.nm)+": "+Integer.toString((int)meterperc)+"%)");
				haveWatching = true;
			} else {
				atr.drawAsWatching(false);
			}
		}
		if(!haveWatching) {
			windowHeader.setText("Character");
			((CharWndWindowHeader) windowHeader).setMeterValue(-1);
		}
		super.draw(initialGL);
	}
	
	public void uimsg(String msg, Object... args) {
		if (msg == "exp") {
			((CharWndWindowHeader) windowHeader).freeTipCache();
			for (int i = 0; i < args.length; i += 4) {
				String nm = (String) args[i];
				int s = (Integer) args[i + 1];
				int h = (Integer) args[i + 2];
				boolean av = ((Integer) args[i + 3]) != 0;
				Attr a = attrs.get(nm);
				a.sexp = s;
				a.hexp = h;
				a.rexp = null;
				a.av = av;
			}
		} else if (msg == "csk") {
			Collection<Skill> sk = new LinkedList<Skill>();
			for (int i = 0; i < args.length; i += 2) {
				String nm = (String) args[i];
				Indir<Resource> res = ui.sess.getres((Integer) args[i + 1]);
				sk.add(new Skill(nm, res));
			}
			csk.pop(sk);
		} else if (msg == "nsk") {
			Collection<Skill> sk = new LinkedList<Skill>();
			int i = 0;
			while (i < args.length) {
				String nm = (String) args[i++];
				Indir<Resource> res = ui.sess.getres((Integer) args[i++]);
				List<String> costa = new LinkedList<String>();
				List<Integer> costv = new LinkedList<Integer>();
				while (true) {
					String anm = (String) args[i++];
					if (anm.equals(""))
						break;
					Integer val = (Integer) args[i++];
					costa.add(anm);
					costv.add(val);
				}
				String[] costa2 = costa.toArray(new String[0]);
				int[] costv2 = new int[costa2.length];
				int o = 0;
				for (Integer v : costv)
					costv2[o++] = v;
				sk.add(new Skill(nm, res, costa2, costv2));
			}
			nsk.pop(sk);
		}
	}
}
