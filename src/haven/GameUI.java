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

import static haven.Inventory.invsq;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.apxeolog.salem.ChatWrapper;
import org.apxeolog.salem.HConfig;
import org.apxeolog.salem.SChatWindow;
import org.apxeolog.salem.SGobble;
import org.apxeolog.salem.SInterfaces.IGobble;
import org.apxeolog.salem.SInterfaces.ITempers;
import org.apxeolog.salem.STempers;
import org.apxeolog.salem.SUtils;
import org.apxeolog.salem.SWidgetOptions;
import org.apxeolog.salem.SWindow;

public class GameUI extends ConsoleHost implements DTarget, DropTarget,
		Console.Directory {
	public final String chrid;
	public final long plid;
	public MenuGrid menu;
	//public Tempers tm;
	//public Gobble gobble;
	public MapView map;
	public LocalMiniMap mmap;
	public Fightview fv;
	public static final Text.Foundry errfoundry = new Text.Foundry(
			new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14), new Color(
					192, 0, 0));
	private Text lasterr;
	private long errtime;
	private SWindow invwnd, equwnd, makewnd;
	private Widget mainmenu, menumenu, mapmenu;
	public BuddyWnd buddies;
	public CharWnd chrwdg;
	public Polity polity;
	public HelpWnd help;
	public Collection<GItem> hand = new LinkedList<GItem>();
	private WItem vhand;
	public ChatUI chat;
	public ChatUI.Channel syslog;
	public int prog = -1;
	private boolean afk = false;
	@SuppressWarnings("unchecked")
	public Indir<Resource>[] belt = new Indir[144];
	public Indir<Resource> lblk, dblk, catk;
	public Belt beltwdg;
	public String polowner;

	/* APXEOLOG */
	public SChatWindow bdsChat;
	public SWidgetOptions bdsOptions;
	public ITempers bdsTempers;
	public IGobble bdsGobble;
	
	public void updateWindowStyle() {
		ui.root.resize(ui.root.sz);
	}
	
	public void updateTempersToConfig() {
		if (bdsTempers != null) {
			boolean vs = bdsTempers.visible;
			ui.destroy(bdsTempers);
			if (HConfig.cl_use_new_tempers) {
				bdsTempers = new STempers(Coord.z, this);
			} else {
				bdsTempers = new Tempers(Coord.z, this);
			}
			if (!vs) bdsTempers.hide();
		}
		if (bdsGobble != null) {
			ui.destroy(bdsGobble);
			if (HConfig.cl_use_new_tempers) {
				bdsGobble = new SGobble(Coord.z, bdsTempers.sz, this);
			} else {
				bdsGobble = new Gobble(Coord.z, this);
			}
		}
		if (bdsTempers != null) {
			bdsTempers.c = new Coord((sz.x - bdsTempers.sz.x) / 2, 0);
			if (bdsGobble != null)
				bdsGobble.c = bdsTempers.c;
		}
	}
	
	public abstract class Belt {
		public abstract int draw(GOut g, int by);

		public abstract boolean click(Coord c, int button);

		public abstract boolean key(KeyEvent ev);

		public abstract boolean item(Coord c);

		public abstract boolean thing(Coord c, Object thing);
	}

	static {
		addtype("gameui", new WidgetFactory() {
			public Widget create(Coord c, Widget parent, Object[] args) {
				String chrid = (String) args[0];
				int plid = (Integer) args[1];
				return (new GameUI(parent, chrid, plid));
			}
		});
	}

	public GameUI(Widget parent, String chrid, long plid) {
		super(Coord.z, parent.sz, parent);
		this.chrid = chrid;
		this.plid = plid;
		setcanfocus(true);
		setfocusctl(true);
		menu = new MenuGrid(Coord.z, this);
		new Avaview(new Coord(10, 10), Avaview.dasz, this, plid, "avacam") {
			public boolean mousedown(Coord c, int button) {
				return (true);
			}
		};
		new Bufflist(new Coord(95, 50), this);
		//tm = new Tempers(Coord.z, this);
		chat = new ChatUI(Coord.z, 0, this);

		bdsOptions = new SWidgetOptions(sz.div(2).sub(150, 150), this);
		bdsOptions.hide();
		
		if (HConfig.cl_use_new_tempers) {
			bdsTempers = new STempers(Coord.z, this);
		} else {
			bdsTempers = new Tempers(Coord.z, this);
		}
		
		bdsChat = new SChatWindow(new Coord(100, 100), new Coord(300, 200),	this);

		syslog = new ChatUI.Log(chat, "System");
		ui.cons.out = new java.io.PrintWriter(new java.io.Writer() {
			StringBuilder buf = new StringBuilder();

			public void write(char[] src, int off, int len) {
				buf.append(src, off, len);
				int p;
				while ((p = buf.indexOf("\n")) >= 0) {
					syslog.append(buf.substring(0, p), Color.WHITE);
					buf.delete(0, p + 1);
				}
			}

			public void close() {
			}

			public void flush() {
			}
		});
		makemenu();
		resize(sz);
	}

	static class MenuButton extends IButton {
		private final int gkey;

		MenuButton(Coord c, Widget parent, String base, int gkey, String tooltip) {
			super(c, parent, Resource.loadimg("gfx/hud/" + base + "up"),
					Resource.loadimg("gfx/hud/" + base + "down"));
			this.tooltip = Text.render(tooltip);
			this.gkey = (char) gkey;
		}

		public void click() {
		}

		public boolean globtype(char key, KeyEvent ev) {
			if ((gkey != -1) && (key == gkey)) {
				click();
				return (true);
			}
			return (super.globtype(key, ev));
		}
	}

	public static class Hidewnd extends SWindow {
		protected Hidewnd(Coord c, Coord sz, Widget parent, String cap) {
			super(c, sz, parent, cap);
		}

		public void wdgmsg(Widget sender, String msg, Object... args) {
			if (msg.equals("swindow_close") || msg.equals("close")) {
				this.hide();
				return;
			}
			super.wdgmsg(sender, msg, args);
		}
	}

	private void updhand() {
		if ((hand.isEmpty() && (vhand != null))
				|| ((vhand != null) && !hand.contains(vhand.item))) {
			ui.destroy(vhand);
			vhand = null;
		}
		if (!hand.isEmpty() && (vhand == null)) {
			GItem fi = hand.iterator().next();
			vhand = new ItemDrag(new Coord(15, 15), this, fi);
		}
	}
	
	public void openBackpack() {
		if (equwnd != null) {
			Equipory eq = equwnd.findchild(Equipory.class);
			if (eq != null) {
				for (GItem item : eq.getGItems()) {
					if (item.getResourceName().contains("gfx/invobjs/backpack")) {
						item.wdgmsg("iact", Coord.z);
					}
				}
			}
		}
	}

	public Widget makechild(String type, Object[] pargs, Object[] cargs) {
		String place = ((String) pargs[0]).intern();
		if (place == "mapview") {
			Coord cc = (Coord) cargs[0];
			map = new MapView(Coord.z, sz, this, cc, plid);
			map.lower();
			if (mmap != null) {
				ui.destroy(mmap);
				ui.destroy(mapmenu);
			}

			// Minimap
			SWindow mmapWindow = new SWindow(new Coord(sz.x - 250, 18), new Coord(250, 250), this, "Minimap"){
				public void wdgmsg(Widget sender, String msg, Object... args) {
					if (msg.equals("mmap_reset")) {
						LocalMiniMap mmap = findchild(LocalMiniMap.class);
						if (mmap != null) {
							mmap.resetOffset();
							mmap.resetScale();
						}
					} else if (msg.equals("mmap_grid")) {
						HConfig.cl_minimap_show_grid = !HConfig.cl_minimap_show_grid;
					} else super.wdgmsg(sender, msg, args);
				};
			};
			mmapWindow.setClosable(false);
			mmapWindow.createPictButton(Resource.loadimg("apx/gfx/hud/pict-r"), "mmap_reset");
			mmapWindow.createPictButton(Resource.loadimg("apx/gfx/hud/pict-g"), "mmap_grid");
			
			mmap = new LocalMiniMap(Coord.z, new Coord(250, 250), mmapWindow, map);

			mapmenu = new Widget(new Coord(100, 5), new Coord(250, 18), this);
			new MenuButton(new Coord(0, 0), mapmenu, "cla", -1,
					"Display personal claims") {
				boolean v = false;

				public void click() {
					if (!v) {
						map.enol(0, 1);
						v = true;
					} else {
						map.disol(0, 1);
						v = false;
					}
				}
			};
			new MenuButton(new Coord(18, 0), mapmenu, "tow", -1,
					"Display town claims") {
				boolean v = false;

				public void click() {
					if (!v) {
						map.enol(2, 3);
						v = true;
					} else {
						map.disol(2, 3);
						v = false;
					}
				}
			};
			new MenuButton(new Coord(36, 0), mapmenu, "tow", -1,
					"Add marker to minimap") {
				public void click() {
					mmap.requestedMarkerSet = true;
				}
			};
			new MenuButton(new Coord(54, 0), mapmenu, "tow", -1,
					"Remove marker from minimap") {
				public void click() {
					SUtils.minimapMarkerRealCoords = null;
				}
			};
			new MenuButton(new Coord(72, 0), mapmenu, "chat", 3,
					"Chat (Ctrl+C)") {
				public void click() {
					chat.toggle();
				}
			};
			return (map);
		} else if (place == "fight") {
			fv = (Fightview) gettype(type).create(
					new Coord(sz.x - Fightview.width, 0), this, cargs);
			return (fv);
		} else if (place == "inv") {
			invwnd = new Hidewnd(new Coord(100, 100), Coord.z, this, "Inventory") {
				@Override
				public void wdgmsg(Widget wdg, String msg, Object... args) {
					if (msg.equals("inv_openbp")) {
						openBackpack();
					} else super.wdgmsg(wdg, msg, args);
				}
			};
			invwnd.createPictButton(Resource.loadimg("apx/gfx/hud/pict-b"), "inv_openbp");
			
			Widget inv = gettype(type).create(Coord.z, invwnd, cargs);
			invwnd.pack();
			invwnd.hide();
			return (inv);
		} else if (place == "equ") {
			equwnd = new Hidewnd(new Coord(400, 10), Coord.z, this, "Equipment");
			Widget equ = gettype(type).create(Coord.z, equwnd, cargs);
			equwnd.pack();
			equwnd.hide();
			return (equ);
		} else if (place == "hand") {
			GItem g = (GItem) gettype(type).create((Coord) pargs[1], this,
					cargs);
			hand.add(g);
			updhand();
			return (g);
		} else if (place == "craft") {
			final Widget[] mk = { null };
			makewnd = new SWindow(new Coord(200, 100), Coord.z, this,
					"Crafting") {
				public void wdgmsg(Widget sender, String msg, Object... args) {
					if (msg.equals("swindow_close")) {
						mk[0].wdgmsg("close");
						return;
					}
					super.wdgmsg(sender, msg, args);
				}

				public void cdestroy(Widget w) {
					if (w == mk[0]) {
						ui.destroy(this);
						makewnd = null;
					}
				}
			};
			mk[0] = gettype(type).create(Coord.z, makewnd, cargs);
			makewnd.pack();
			return (mk[0]);
		} else if (place == "buddy") {
			buddies = (BuddyWnd) gettype(type).create(new Coord(187, 50), this,
					cargs);
			buddies.hide();
			return (buddies);
		} else if (place == "pol") {
			polity = (Polity) gettype(type).create(new Coord(500, 50), this,
					cargs);
			polity.hide();
			return (polity);
		} else if (place == "chr") {
			chrwdg = (CharWnd) gettype(type).create(new Coord(100, 50), this,
					cargs);
			chrwdg.hide();
			return (chrwdg);
		} else if (place == "chat") {
			return chat.makechild(type, new Object[] {}, cargs);
		} else if (place == "party") {
			return (gettype(type).create(new Coord(10, 95), this, cargs));
		} else if (place == "misc") {
			return (gettype(type).create((Coord) pargs[1], this, cargs));
		} else {
			throw (new UI.UIException("Illegal gameui child", type, pargs));
		}
	}

	public void cdestroy(Widget w) {
		if ((w instanceof GItem) && hand.contains(w)) {
			hand.remove(w);
			updhand();
		} else if (w == polity) {
			polity = null;
		} else if (w == chrwdg) {
			chrwdg = null;
		}
	}

	private boolean showbeltp() {
		return (!chat.expanded);
	}

	static Text.Foundry progf = new Text.Foundry(new java.awt.Font("serif",
			java.awt.Font.BOLD, 24));
	static {
		progf.aa = true;
	}
	Text progt = null;

	public void draw(GOut g) {
		mainmenu.show(showbeltp());
		super.draw(g);
		togglesdw(g.gc);
		if (prog >= 0) {
			String progs = String.format("%d%%", prog);
			if ((progt == null) || !progs.equals(progt.text))
				progt = progf.render(progs);
			g.aimage(progt.tex(), new Coord(sz.x / 2, (sz.y * 4) / 10), 0.5,
					0.5);
		}
		int by = sz.y;
		if (mainmenu.visible)
			by -= mainmenu.sz.y + 5;
		if (chat.expanded)
			by -= chat.sz.y;
		if (showbeltp()) {
			by -= beltwdg.draw(g, by);
		}
		if (cmdline != null) {
			drawcmd(g, new Coord(135, by -= 20));
		} else if (lasterr != null) {
			if ((System.currentTimeMillis() - errtime) > 3000) {
				lasterr = null;
			} else {
				g.chcolor(0, 0, 0, 192);
				g.frect(new Coord(133, by - 22), lasterr.sz().add(4, 4));
				g.chcolor();
				g.image(lasterr.tex(), new Coord(135, by -= 20));
			}
		}
		if (!chat.expanded) {
			chat.drawsmall(g, new Coord(135, by), 50);
		}
	}

	public void tick(double dt) {
		super.tick(dt);
		if (!afk && (System.currentTimeMillis() - ui.lastevent > 300000)) {
			afk = true;
			wdgmsg("afk");
		} else if (afk && (System.currentTimeMillis() - ui.lastevent < 300000)) {
			afk = false;
		}
		dwalkupd();
	}

	public void uimsg(String msg, Object... args) {
		if (msg == "err") {
			String err = (String) args[0];
			error(err);
		} else if (msg == "prog") {
			if (args.length > 0)
				prog = (Integer) args[0];
			else
				prog = -1;
		} else if (msg == "setbelt") {
			int slot = (Integer) args[0];
			if (args.length < 2) {
				belt[slot] = null;
			} else {
				belt[slot] = ui.sess.getres((Integer) args[1]);
			}
		} else if (msg == "stm") {
			int[] n = new int[4];
			for (int i = 0; i < 4; i++)
				n[i] = (Integer) args[i];
			bdsTempers.upds(n);
		} else if (msg == "htm") {
			int[] n = new int[4];
			for (int i = 0; i < 4; i++)
				n[i] = (Integer) args[i];
			bdsTempers.updh(n);
		} else if (msg == "gobble") {
			boolean g = (Integer) args[0] != 0;
			if (g && (bdsGobble == null)) {
				bdsTempers.hide();
				if (HConfig.cl_use_new_tempers) {
					bdsGobble = new SGobble(Coord.z, bdsTempers.sz, this);
				} else {
					bdsGobble = new Gobble(Coord.z, this);
				}
				resize(sz);
			} else if (!g && (bdsGobble != null)) {
				ui.destroy(bdsGobble);
				bdsGobble = null;
				bdsTempers.show();
			}
		} else if (msg == "gtm") {
			int[] n = new int[4];
			for (int i = 0; i < 4; i++)
				n[i] = (Integer) args[i];
			bdsGobble.updt(n);
		} else if (msg == "gvar") {
			bdsGobble.updv((Integer) args[0]);
		} else if (msg == "gtrig") {
			bdsGobble.trig((Integer) args[0]);
		} else if (msg == "polowner") {
			String o = (String) args[0];
			if (o.length() == 0)
				o = null;
			else
				o = o.intern();
			if (o != polowner) {
				if (map != null) {
					if (o == null) {
						if (polowner != null)
							map.setpoltext("Leaving " + polowner);
					} else {
						map.setpoltext("Entering " + o);
					}
				}
				polowner = o;
			}
		} else if (msg == "dblk") {
			int id = (Integer) args[0];
			dblk = (id < 0) ? null : (ui.sess.getres(id));
		} else if (msg == "lblk") {
			int id = (Integer) args[0];
			lblk = (id < 0) ? null : (ui.sess.getres(id));
		} else if (msg == "catk") {
			int id = (Integer) args[0];
			catk = (id < 0) ? null : (ui.sess.getres(id));
		} else if (msg == "showhelp") {
			Indir<Resource> res = ui.sess.getres((Integer) args[0]);
			if (help == null)
				help = new HelpWnd(sz.div(2).sub(150, 200), this, res);
			else
				help.res = res;
		} else {
			super.uimsg(msg, args);
		}
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (sender == menu) {
			wdgmsg(msg, args);
			return;
		} else if ((sender == buddies) && (msg == "close")) {
			buddies.hide();
		} else if ((sender == polity) && (msg == "close")) {
			polity.hide();
		} else if ((sender == chrwdg) && (msg == "close")) {
			chrwdg.hide();
		} else if ((sender == help) && (msg == "close")) {
			ui.destroy(help);
			help = null;
			return;
		}
		super.wdgmsg(sender, msg, args);
	}

	private void fitwdg(Widget wdg) {
		if (wdg.c.x < 0)
			wdg.c.x = 0;
		if (wdg.c.y < 0)
			wdg.c.y = 0;
		if (wdg.c.x + wdg.sz.x > sz.x)
			wdg.c.x = sz.x - wdg.sz.x;
		if (wdg.c.y + wdg.sz.y > sz.y)
			wdg.c.y = sz.y - wdg.sz.y;
	}

	/*
	 * Directional walking. Apparently AWT send repeated keyup/keydown events on
	 * key autorepeat (:-/), so hysteresis elimination of some kind is
	 * necessary. This variant waits 100 ms before accepting a keyup event.
	 */
	private boolean dwalking = false;
	private Coord dwalkang = new Coord();
	private long dwalkhys;
	private float dwalkbase;
	private boolean[] dkeys = { false, false, false, false };

	private void dwalkupd() {
		Coord a = new Coord();
		if (dkeys[0])
			a = a.add(1, 0);
		if (dkeys[1])
			a = a.add(0, 1);
		if (dkeys[2])
			a = a.add(-1, 0);
		if (dkeys[3])
			a = a.add(0, -1);
		long now = System.currentTimeMillis();
		if (!a.equals(dwalkang) && (now > dwalkhys)) {
			if ((a.x == 0) && (a.y == 0)) {
				wdgmsg("dwalk");
			} else {
				float da = dwalkbase + (float) a.angle(Coord.z);
				wdgmsg("dwalk", (int) ((da / (Math.PI * 2)) * 1000));
			}
			dwalkang = a;
		}
	}

	private int dwalkkey(char key) {
		if (key == 'W')
			return (0);
		else if (key == 'D')
			return (1);
		else if (key == 'S')
			return (2);
		else if (key == 'A')
			return (3);
		throw (new Error());
	}

	private void dwalkdown(char key, KeyEvent ev) {
		if (!dwalking) {
			dwalking = true;
			dwalkbase = -map.camera.angle();
			ui.grabkeys(this);
		}
		int k = dwalkkey(key);
		dkeys[k] = true;
		dwalkhys = ev.getWhen();
	}

	private void dwalkup(char key, KeyEvent ev) {
		int k = dwalkkey(key);
		dkeys[k] = false;
		dwalkhys = ev.getWhen() + 100;
		if (!dkeys[0] && !dkeys[1] && !dkeys[2] && !dkeys[3]) {
			dwalking = false;
			ui.grabkeys(null);
		}
	}

	public boolean togglesdw = false;

	public void setShadows(boolean shad) {
		togglesdw = shad;
	}
	
	public void toggleOptions() {
		if(bdsOptions == null) return;
		if (bdsOptions.visible) bdsOptions.hide();
		else bdsOptions.show();
	}
	
	private void makemenu() {
		mainmenu = new Widget(new Coord(135, sz.y - 26), new Coord(386, 26),
				this);
		int x = 0;
		new MenuButton(new Coord(x, 0), mainmenu, "inv", 9, "Inventory (Tab)") {
			public void click() {
				if ((invwnd != null) && invwnd.show(!invwnd.visible)) {
					invwnd.raise();
					fitwdg(invwnd);
				}
			}
		};
		x += 62;
		new MenuButton(new Coord(x, 0), mainmenu, "equ", 5,
				"Equipment (Ctrl+E)") {
			public void click() {
				if ((equwnd != null) && equwnd.show(!equwnd.visible)) {
					equwnd.raise();
					fitwdg(equwnd);
				}
			}
		};
		x += 62;
		new MenuButton(new Coord(x, 0), mainmenu, "chr", 20,
				"Studying (Ctrl+T)") {
			public void click() {
				if ((chrwdg != null) && chrwdg.show(!chrwdg.visible)) {
					chrwdg.raise();
					fitwdg(chrwdg);
					setfocus(chrwdg);
				}
			}
		};
		x += 62;
		new MenuButton(new Coord(x, 0), mainmenu, "bud", 2,
				"Buddy List (Ctrl+B)") {
			public void click() {
				if ((buddies != null) && buddies.show(!buddies.visible)) {
					buddies.raise();
					fitwdg(buddies);
					setfocus(buddies);
				}
			}
		};
		x += 62;
		new MenuButton(new Coord(x, 0), mainmenu, "pol", 16, "Town (Ctrl+P)") {
			public void click() {
				if ((polity != null) && polity.show(!polity.visible)) {
					polity.raise();
					fitwdg(polity);
					setfocus(polity);
				}
			}
		};
		x += 62;
		new MenuButton(new Coord(x, 0), mainmenu, "opt", -1,
				"Options (BDSaleM)") {
			public void click() {
				toggleOptions();
			}
		};
		menumenu = new Widget(Coord.z, new Coord(132, 33), this) {
			public void draw(GOut g) {
				super.draw(g);
				try {
					if (catk != null)
						g.image(catk.get().layer(Resource.imgc).tex(),
								new Coord(33, 0));
				} catch (Loading e) {
				}
				try {
					if (lblk != null) {
						Tex t = lblk.get().layer(Resource.imgc).tex();
						g.image(t, new Coord(99, 0));
						g.chcolor(0, 255, 0, 128);
						g.frect(new Coord(99, 0), t.sz());
						g.chcolor();
					} else if (dblk != null) {
						g.image(dblk.get().layer(Resource.imgc).tex(),
								new Coord(99, 0));
					}
				} catch (Loading e) {
				}
			}
		};
		new MenuButton(new Coord(0, 0), menumenu, "atk", 1,
				"Attack mode (Ctrl+A)") {
			public void click() {
				GameUI.this.wdgmsg("atkm");
			}
		};
		new MenuButton(new Coord(66, 0), menumenu, "blk", 19,
				"Toggle maneuver (Ctrl+S)") {
			public void click() {
				act("blk");
			}
		};
	}

	private void togglesdw(GLConfig gc) {
		if (togglesdw) {
			togglesdw = false;
			if (gc.deflight == Light.pslights) {
				gc.deflight = Light.vlights;
			} else {
				if (gc.shuse) {
					gc.deflight = Light.pslights;
				} else {
					error("Shadow rendering requires a shader compatible video card.");
				}
			}
		}
	}

	public boolean globtype(char key, KeyEvent ev) {
		if(ev.getKeyCode() == 79 && ev.isControlDown()) {
			//CTRL+O
			toggleOptions();
			return true;
		}
		if(ev.getKeyCode() == 81 && ev.isControlDown()) {
			//CTRL+Q
			openBackpack();
			return true;
		}
		char ukey = Character.toUpperCase(key);
		if (key == ':') {
			entercmd();
			return (true);
		} else if ((ukey == 'W') || (ukey == 'A') || (ukey == 'S')
				|| (ukey == 'D')) {
			dwalkdown(ukey, ev);
			return (true);
		}
		if ((key == 0) && beltwdg.key(ev))
			return (true);
		return (super.globtype(key, ev));
	}

	public boolean keydown(KeyEvent ev) {
		char ukey = Character.toUpperCase(ev.getKeyChar());
		if (dwalking
				&& ((ukey == 'W') || (ukey == 'A') || (ukey == 'S') || (ukey == 'D'))) {
			dwalkdown(ukey, ev);
			return (true);
		}
		return (super.keydown(ev));
	}

	public boolean keyup(KeyEvent ev) {
		char ukey = Character.toUpperCase(ev.getKeyChar());
		if (dwalking
				&& ((ukey == 'W') || (ukey == 'A') || (ukey == 'S') || (ukey == 'D'))) {
			dwalkup(ukey, ev);
			return (true);
		}
		return (super.keyup(ev));
	}

	public boolean mousedown(Coord c, int button) {
		if (showbeltp() && beltwdg.click(c, button))
			return (true);
		return (super.mousedown(c, button));
	}

	public boolean drop(Coord cc, Coord ul) {
		return (showbeltp() && beltwdg.item(cc));
	}

	public boolean iteminteract(Coord cc, Coord ul) {
		return (false);
	}

	public boolean dropthing(Coord c, Object thing) {
		return (showbeltp() && beltwdg.thing(c, thing));
	}

	public void resize(Coord sz) {
		super.resize(sz);
		menu.c = sz.sub(menu.sz);
		menumenu.c = menu.c.add(menu.sz.x, 0).sub(menumenu.sz);
		bdsTempers.c = new Coord((sz.x - bdsTempers.sz.x) / 2, 0);
		chat.resize(sz.x - 125 - menu.sz.x);
		chat.move(new Coord(125, sz.y));
		if (bdsGobble != null)
			bdsGobble.c = bdsTempers.c;
		if (map != null)
			map.resize(sz);

//		if (mmap != null)
//			mmap.parent.c = new Coord(sz.x - 250, 18);
//
//		if (mapmenu != null)
//			mapmenu.c = mmap.c.add(0, -18);
		if (fv != null)
			fv.c = new Coord(sz.x - Fightview.width, 0);
		mainmenu.c = new Coord(135, sz.y - 26);
	}

	public void presize() {
		resize(parent.sz);
	}

	private static final Resource errsfx = Resource.load("sfx/error");

	public void error(String msg) {
		errtime = System.currentTimeMillis();
		lasterr = errfoundry.render(msg);
		syslog.append(msg, Color.RED);
		Audio.play(errsfx);
	}

	public void act(String... args) {
		wdgmsg("act", (Object[]) args);
	}

	public class FKeyBelt extends Belt {
		public final int beltkeys[] = { KeyEvent.VK_F1, KeyEvent.VK_F2,
				KeyEvent.VK_F3, KeyEvent.VK_F4, KeyEvent.VK_F5, KeyEvent.VK_F6,
				KeyEvent.VK_F7, KeyEvent.VK_F8, KeyEvent.VK_F9,
				KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12 };
		public int curbelt = 0;

		private Coord beltc(int i) {
			return (new Coord(/* ((sz.x - (invsq.sz().x * 12) - (2 * 11)) / 2) */
			135 + ((invsq.sz().x + 2) * i) + (10 * (i / 4)), sz.y - 26
					- invsq.sz().y - 2));
		}

		private int beltslot(Coord c) {
			for (int i = 0; i < 12; i++) {
				if (c.isect(beltc(i), invsq.sz()))
					return (i + (curbelt * 12));
			}
			return (-1);
		}

		public int draw(GOut g, int by) {
			for (int i = 0; i < 12; i++) {
				int slot = i + (curbelt * 12);
				Coord c = beltc(i);
				g.image(invsq, beltc(i));
				try {
					if (belt[slot] != null)
						g.image(belt[slot].get().layer(Resource.imgc).tex(),
								c.add(1, 1));
				} catch (Loading e) {
				}
				g.chcolor(156, 180, 158, 255);
				FastText.aprintf(g, c.add(invsq.sz()), 1, 1, "F%d", i + 1);
				g.chcolor();
			}
			return (invsq.sz().y);
		}

		public boolean click(Coord c, int button) {
			int slot = beltslot(c);
			if (slot != -1) {
				if (button == 1)
					wdgmsg("belt", slot, 1, ui.modflags());
				if (button == 3)
					wdgmsg("setbelt", slot, 1);
				return (true);
			}
			return (false);
		}

		public boolean key(KeyEvent ev) {
			boolean M = (ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
			for (int i = 0; i < beltkeys.length; i++) {
				if (ev.getKeyCode() == beltkeys[i]) {
					if (M) {
						curbelt = i;
						return (true);
					} else {
						wdgmsg("belt", i + (curbelt * 12), 1, ui.modflags());
						return (true);
					}
				}
			}
			return (false);
		}

		public boolean item(Coord c) {
			int slot = beltslot(c);
			if (slot != -1) {
				wdgmsg("setbelt", slot, 0);
				return (true);
			}
			return (false);
		}

		public boolean thing(Coord c, Object thing) {
			int slot = beltslot(c);
			if (slot != -1) {
				if (thing instanceof Resource) {
					Resource res = (Resource) thing;
					if (res.layer(Resource.action) != null) {
						wdgmsg("setbelt", slot, res.name);
						return (true);
					}
				}
			}
			return (false);
		}
	}

	public class NKeyBelt extends Belt {
		public int curbelt = 0;

		private Coord beltc(int i) {
			return (new Coord(/* ((sz.x - (invsq.sz().x * 12) - (2 * 11)) / 2) */
			135 + ((invsq.sz().x + 2) * i) + (10 * (i / 5)), sz.y - 26
					- invsq.sz().y - 2));
		}

		private int beltslot(Coord c) {
			for (int i = 0; i < 10; i++) {
				if (c.isect(beltc(i), invsq.sz()))
					return (i + (curbelt * 12));
			}
			return (-1);
		}

		public int draw(GOut g, int by) {
			for (int i = 0; i < 10; i++) {
				int slot = i + (curbelt * 12);
				Coord c = beltc(i);
				g.image(invsq, beltc(i));
				try {
					if (belt[slot] != null)
						g.image(belt[slot].get().layer(Resource.imgc).tex(),
								c.add(1, 1));
				} catch (Loading e) {
				}
				g.chcolor(156, 180, 158, 255);
				FastText.aprintf(g, c.add(invsq.sz()), 1, 1, "%d", (i + 1) % 10);
				g.chcolor();
			}
			return (invsq.sz().y);
		}

		public boolean click(Coord c, int button) {
			int slot = beltslot(c);
			if (slot != -1) {
				if (button == 1)
					wdgmsg("belt", slot, 1, ui.modflags());
				if (button == 3)
					wdgmsg("setbelt", slot, 1);
				return (true);
			}
			return (false);
		}

		public boolean key(KeyEvent ev) {
			int c = ev.getKeyChar();
			if ((c < KeyEvent.VK_0) || (c > KeyEvent.VK_9))
				return (false);
			int i = Utils.floormod(c - KeyEvent.VK_0 - 1, 10);
			boolean M = (ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
			if (M)
				curbelt = i;
			else
				wdgmsg("belt", i + (curbelt * 12), 1, ui.modflags());
			return (true);
		}

		public boolean item(Coord c) {
			int slot = beltslot(c);
			if (slot != -1) {
				wdgmsg("setbelt", slot, 0);
				return (true);
			}
			return (false);
		}

		public boolean thing(Coord c, Object thing) {
			int slot = beltslot(c);
			if (slot != -1) {
				if (thing instanceof Resource) {
					Resource res = (Resource) thing;
					if (res.layer(Resource.action) != null) {
						wdgmsg("setbelt", slot, res.name);
						return (true);
					}
				}
			}
			return (false);
		}
	}

	{
		String val = Utils.getpref("belttype", "n");
		if (val.equals("n")) {
			beltwdg = new NKeyBelt();
		} else if (val.equals("f")) {
			beltwdg = new FKeyBelt();
		} else {
			beltwdg = new NKeyBelt();
		}
	}

	private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
	{
		cmdmap.put("afk", new Console.Command() {
			public void run(Console cons, String[] args) {
				afk = true;
				wdgmsg("afk");
			}
		});
		cmdmap.put("act", new Console.Command() {
			public void run(Console cons, String[] args) {
				Object[] ad = new Object[args.length - 1];
				System.arraycopy(args, 1, ad, 0, ad.length);
				wdgmsg("act", ad);
			}
		});
		cmdmap.put("belt", new Console.Command() {
			public void run(Console cons, String[] args) {
				if (args[1].equals("f")) {
					beltwdg = new FKeyBelt();
					Utils.setpref("belttype", "f");
				} else if (args[1].equals("n")) {
					beltwdg = new NKeyBelt();
					Utils.setpref("belttype", "n");
				}
			}
		});
	}

	public Map<String, Console.Command> findcmds() {
		return (cmdmap);
	}
}
