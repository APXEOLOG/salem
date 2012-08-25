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

package org.apxeolog.salem.widgets;

import haven.Audio;
import haven.Button;
import haven.CheckBox;
import haven.Config;
import haven.Coord;
import haven.GOut;
import haven.GameUI;
import haven.GameUI.Hidewnd;
import haven.Label;
import haven.Loading;
import haven.Resource;
import haven.RichText;
import haven.Scrollbar;
import haven.Tabs;
import haven.Tabs.Tab;
import haven.TextEntry;
import haven.WItem;
import haven.Widget;

import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.apxeolog.salem.SChatWrapper;
import org.apxeolog.salem.config.MinimapHighlightConfig;
import org.apxeolog.salem.config.MinimapHighlightConfig.HighlightInfo;
import org.apxeolog.salem.config.ToolbarsConfig.TBSlot;
import org.apxeolog.salem.config.ToolbarsConfig;
import org.apxeolog.salem.config.XConfig;
import org.apxeolog.salem.config.XMLConfigProvider;

public class SWidgetOptions extends Hidewnd {
	public static final RichText.Foundry foundry = new RichText.Foundry(
			TextAttribute.FAMILY, "SansSerif", TextAttribute.SIZE, 10);
	private Tabs body;

	@Override
	public void unlink() {
		XMLConfigProvider.save();
		ToolbarsConfig.updateToolbars(ui.root);
		super.unlink();
	}

	@Override
	protected void maximize() {
		super.maximize();
		for (Tab t : body.tabs) t.hide();
		body.showtab(ftab);
	};

	Tab ftab;

	int tb_buf_mode = -1;
	int tb_buf_key = -1;

	public SWidgetOptions(Coord c, Widget parent) {
		super(c, new Coord(345, 300), parent, "Options");

		body = new Tabs(Coord.z, new Coord(345, 290), this);

		Tab tab;

		{ /* Highlight TAB */
			tab = body.new Tab(new Coord(90, 10), 70, "Highlight") {
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

		{ /* Toolbars TAB */
			tab = body.new Tab(new Coord(170, 10), 70, "Toolbars") {
				@Override
				public void draw(GOut g) {
					g.chcolor(255, 255, 255, 255);
					g.rect(Coord.z, sz.add(1, 1));
					super.draw(g);
				}
			};

			new Label(new Coord(20, 40), tab, "Defined toolbars:");

			final TextEntry hotkeyGrabber = new TextEntry(new Coord(180, 60), new Coord(80, 20), tab, "") {
				@Override
				public boolean type(char c, KeyEvent e){
					return true;
				}

				@Override
				public boolean keydown(KeyEvent event) {
					int kcode = event.getKeyCode();
					String hotkey = String.valueOf(KeyEvent.getKeyText(kcode)).toLowerCase();
					if(hotkey.equals("shift") || hotkey.equals("ctrl") || hotkey.equals("alt"))
						return true;
					hotkey = hotkey.toUpperCase();
					String mods = "";
					if(event.isControlDown())
						mods += "Ctrl+";
					if(event.isAltDown())
						mods += "Alt+";
					if(event.isShiftDown())
						mods += "Shift+";
					settext(mods+hotkey);

					tb_buf_mode = event.getModifiersEx();
					tb_buf_key = event.getKeyCode();

					return true;
				}
			};

			final TextEntry toolbarName = new TextEntry(new Coord(20, 190), new Coord(90, 20), tab, "ToolbarName");

			final SlotsList slotSet = new SlotsList(new Coord(180, 90), new Coord(150, 120), tab) {
				@Override
				protected void changed(int index) {
					hotkeyGrabber.settext("");
				}
			};

			final ToolbarsList toolbarsList = new ToolbarsList(new Coord(20, 60),	new Coord(150, 120), tab, ToolbarsConfig.definedToolbars) {
				@Override
				protected void changed(int index) {
					if (index == -1) return;
					toolbarName.settext(getSelectedToolbar().tbName);

					slotSet.setupToolbar(getSelectedToolbar());
				}
			};

			new Button(new Coord(120, 190), 50, tab, "Add") {
				@Override
				public void click() {
					if (!ToolbarsConfig.definedToolbars.containsKey(toolbarName.text)) {
						toolbarsList.addToolbar(toolbarName.text);
					}
				};
			};

			new Button(new Coord(30, 220), 60, tab, "Remove") {
				@Override
				public void click() {
					if (toolbarsList.getSelectedToolbar() != null) {
						toolbarsList.removeCurrent();
						slotSet.setupToolbar(null);
					}
				};
			};
			new Button(new Coord(100, 220), 60, tab, "Toggle") {
				@Override
				public void click() {
					if (toolbarsList.getSelectedToolbar() != null) {
						toolbarsList.toggleCurrent();
					}
				};
			};

			new Label(new Coord(220, 40), tab, "Toolbar settings:");

			new Button(new Coord(270, 60), 60, tab, "Add") {
				@Override
				public void click() {
					if (tb_buf_key > -1 && tb_buf_mode > -1)
						slotSet.addSlot(tb_buf_mode, tb_buf_key);
				};
			};

			new Button(new Coord(190, 220), 60, tab, "Set") {
				@Override
				public void click() {
					if (tb_buf_key > -1 && tb_buf_mode > -1)
						slotSet.setSlot(tb_buf_mode, tb_buf_key);
				};
			};

			new Button(new Coord(260, 220), 60, tab, "Remove") {
				@Override
				public void click() {
					slotSet.removeCurrent();
				};
			};

		}


		{ /* IRC TAB */
			tab = body.new Tab(new Coord(250, 10), 70, "IRC") {
				@Override
				public void draw(GOut g) {
					g.chcolor(255, 255, 255, 255);
					g.rect(Coord.z, sz.add(1, 1));
					super.draw(g);
				}
			};

			new Label(new Coord(20, 40), tab, "Enter IRC settings here. All changes will be saved.");
			new Label(new Coord(20, 60), tab, "IRC Server:");
			new TextEntry(new Coord(20, 80), new Coord(100, 20), tab, XConfig.mp_irc_server) {
				@Override
				public void changed() {
					XConfig.mp_irc_server = this.text;
				}
			};
			new Label(new Coord(20, 100), tab, "Username:");
			if (XConfig.mp_irc_username.isEmpty()) XConfig.mp_irc_username = Config.currentCharName;
			new TextEntry(new Coord(20, 120), new Coord(100, 20), tab, XConfig.mp_irc_username) {
				@Override
				public void changed() {
					XConfig.mp_irc_username = this.text;
				}
			};
			new Label(new Coord(130, 100), tab, "Password:");
			new TextEntry(new Coord(130, 120), new Coord(100, 20), tab, XConfig.mp_irc_password) {
				@Override
				public void changed() {
					XConfig.mp_irc_password = this.text;
				}
			};

			CheckBox checkb = new CheckBox(new Coord(20, 140), tab, "Connect automatically") {
				@Override
				public void changed(boolean val) {
					XConfig.mp_irc_autoconnect = val;
				}
			};
			checkb.set(XConfig.mp_irc_autoconnect);
			new Button(new Coord(20, 180), 50, tab, "Connect") {
				@Override
				public void click() {
					SChatWrapper.startIRCProvider();
				}
			};
		}

		{ /* General TAB */
			GameUI gui = getparent(GameUI.class);
			ftab = tab = body.new Tab(new Coord(10, 10), 70, "General") {
				@Override
				public void draw(GOut g) {
					g.chcolor(255, 255, 255, 255);
					g.rect(Coord.z, sz.add(1, 1));
					super.draw(g);
				}
			};

			new Button(new Coord(200, 40), 120, tab, "Logout") {
				@Override
				public void click() {
					ui.sess.close();
					if(!XConfig.cl_render_on)
						XConfig.cl_render_on = true;
				}
			};

			new Label(new Coord(250, 190), tab, "SFX volume");
			(new Scrollbar(new Coord(285, 85), 100, tab, 0, 100) {
				{
					val = max - XConfig.cl_sfx_volume;
				}
				@Override
				public void changed() {
					XConfig.cl_sfx_volume = max - val;
					XMLConfigProvider.save();
					double vol = (max-val)/100.;
					Audio.setvolume(vol);
				}
				@Override
				public Object tooltip(Coord c, boolean a) {
					return Integer.toString(max - val);
				}
				@Override
				public boolean mousewheel(Coord c, int amount) {
					if(val+amount < min)
						val = min;
					else if(val+amount > max)
						val = max;
					else
						val = val + amount;
					changed();
					return (true);
				}
			}).changed();

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
					XConfig.cl_dump_minimaps = val;
					XMLConfigProvider.save();
				}
			};
			checkb.set(XConfig.cl_dump_minimaps);

			checkb = new CheckBox(new Coord(20, 120), tab, "New tempers") {
				@Override
				public void changed(boolean val) {
					XConfig.cl_use_new_tempers = val;
					GameUI gui = getparent(GameUI.class);
					if (gui != null) gui.updateTempersToConfig();
					XMLConfigProvider.save();
				}
			};
			checkb.set(XConfig.cl_use_new_tempers);

			new Label(new Coord(20, 160), tab, "Windows header align:");
			final CheckBox[] aligns = new CheckBox[3];
			aligns[0] = new CheckBox(new Coord(20, 170), tab, "Left") {
				@Override
				public void changed(boolean val) {
					XConfig.cl_swindow_header_align = SWindow.HEADER_ALIGN_LEFT;
					aligns[1].a = false;
					aligns[2].a = false;
					GameUI ui = getparent(GameUI.class);
					if (ui != null) ui.updateWindowStyle();
					XMLConfigProvider.save();
				}
			};

			aligns[1] = new CheckBox(new Coord(80, 170), tab, "Center") {
				@Override
				public void changed(boolean val) {
					XConfig.cl_swindow_header_align = SWindow.HEADER_ALIGN_CENTER;
					aligns[0].a = false;
					aligns[2].a = false;
					GameUI ui = getparent(GameUI.class);
					if (ui != null) ui.updateWindowStyle();
					XMLConfigProvider.save();
				}
			};

			aligns[2] = new CheckBox(new Coord(140, 170), tab, "Right") {
				@Override
				public void changed(boolean val) {
					XConfig.cl_swindow_header_align = SWindow.HEADER_ALIGN_RIGHT;
					aligns[0].a = false;
					aligns[1].a = false;
					GameUI ui = getparent(GameUI.class);
					if (ui != null) ui.updateWindowStyle();
					XMLConfigProvider.save();
				}
			};
			aligns[XConfig.cl_swindow_header_align].a = true;

			checkb = new CheckBox(new Coord(20, 210), tab, "Use Free Camera") {
				@Override
				public void changed(boolean val) {
					XConfig.cl_use_free_cam = val;
					XMLConfigProvider.save();
					GameUI ui = getparent(GameUI.class);
					if (ui != null && ui.map != null) ui.map.setupCamera();
				}
			};
			checkb.set(XConfig.cl_use_free_cam);

			checkb = new CheckBox(new Coord(20, 250), tab, "Use New Chat") {
				@Override
				public void changed(boolean val) {
					XConfig.cl_use_new_chat = val;
					XMLConfigProvider.save();
					GameUI ui = getparent(GameUI.class);
					if (ui.bdsChatB != null) {
						if (XConfig.cl_use_new_chat) {
							ui.bdsChatB.show();
						} else {
							ui.bdsChatB.hide();
						}
					}
				}
			};
			checkb.set(XConfig.cl_use_new_chat);

			checkb = new CheckBox(new Coord(120, 250), tab, "Use New Toolbars") {
				@Override
				public void changed(boolean val) {
					XConfig.cl_use_new_toolbars = val;
					XMLConfigProvider.save();
					ToolbarsConfig.updateToolbars(parent.ui.root);
				}
			};
			checkb.set(XConfig.cl_use_new_toolbars);
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
					g.atext(current.getTooltip(), new Coord(60, 30), 0, 0);
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
			@Override
			public int compare(HighlightInfo a, HighlightInfo b) {
				return (a.getTooltip().compareTo(b.getTooltip()));
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
			hlList = MinimapHighlightConfig.getHashMap().values().toArray(new HighlightInfo[MinimapHighlightConfig.getHashMap().size()]);
			Arrays.sort(hlList, hlComparator);
			sb.val = 0;
			sb.max = hlList.length - h;
			sel = -1;
		}

		@Override
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
					g.atext(hl.getTooltip(), new Coord(25, i * 20 + 10), 0, 0.5);
				} catch (Loading e) {
					g.image(WItem.missing.layer(Resource.imgc).tex(), new Coord(0, i * 20), new Coord(20, 20));
					g.atext("...", new Coord(25, i * 20 + 10), 0, 0.5);
				}
				g.chcolor();
			}
			super.draw(g);
		}

		@Override
		public boolean mousewheel(Coord c, int amount) {
			sb.ch(amount);
			return (true);
		}

		@Override
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

	public static class TextList extends Widget {
		private int iCannotRememberWhyDoINeedThisVariable;
		private Scrollbar scrollbar;
		private int selectedIndex;
		private ArrayList<String> textList;

		public TextList(Coord c, Coord sz, Widget parent) {
			super(c, sz, parent);

			iCannotRememberWhyDoINeedThisVariable = sz.y / 20;

			scrollbar = new Scrollbar(new Coord(sz.x, 0), sz.y, this, 0, 0);
			scrollbar.val = 0;

			selectedIndex = -1;

			textList = new ArrayList<String>();
		}

		public void addText(String text) {
			textList.add(text);
			scrollbar.max = textList.size() - iCannotRememberWhyDoINeedThisVariable;
		}

		public String getSelectedText() {
			if (selectedIndex >= 0 && selectedIndex < textList.size()) return textList.get(selectedIndex);
			return null;
		}

		public int getSelectedIndex() {
			return selectedIndex;
		}

		public void removeCurrent() {
			textList.remove(selectedIndex);
			scrollbar.max = textList.size() - iCannotRememberWhyDoINeedThisVariable;
			selectedIndex = -1;
		}

		@Override
		public void draw(GOut g) {
			g.chcolor(0, 0, 0, 255);
			g.frect(Coord.z, sz);
			g.chcolor(255, 255, 255, 255);
			g.rect(Coord.z, sz.add(1, 1));
			g.chcolor();

			for (int i = 0; i < iCannotRememberWhyDoINeedThisVariable; i++) {
				if (i + scrollbar.val >= textList.size()) continue;
				String hl = textList.get(i + scrollbar.val);
				if (i + scrollbar.val == selectedIndex) {
					g.chcolor(255, 255, 0, 128);
					g.frect(new Coord(0, i * 20), new Coord(sz.x, 20));
					g.chcolor();
				}
				g.atext(hl, new Coord(25, i * 20 + 10), 0, 0.5);
				g.chcolor();
			}
			super.draw(g);
		}

		@Override
		public boolean mousewheel(Coord c, int amount) {
			scrollbar.ch(amount);
			return (true);
		}

		@Override
		public boolean mousedown(Coord c, int button) {
			if (super.mousedown(c, button))
				return (true);
			if (button == 1) {
				selectedIndex = (c.y / 20) + scrollbar.val;
				if (selectedIndex >= textList.size())
					selectedIndex = -1;
				changed(selectedIndex);
				return (true);
			}
			return (false);
		}

		protected void changed(int index) {

		}

		public void unsel() {
			selectedIndex = -1;
			changed(selectedIndex);
		}
	}


	/* Toolbars */
	public static class ToolbarsList extends Widget {
		private int iCannotRememberWhyDoINeedThisVariable;
		private Scrollbar scrollbar;
		private int selectedIndex;
		private ArrayList<String> textListBuf;
		private HashMap<String, ToolbarsConfig> toolbarsMap;

		public ToolbarsList(Coord c, Coord sz, Widget parent, HashMap<String, ToolbarsConfig> tbMap) {
			super(c, sz, parent);

			iCannotRememberWhyDoINeedThisVariable = sz.y / 20;

			scrollbar = new Scrollbar(new Coord(sz.x, 0), sz.y, this, 0, 0);
			scrollbar.val = 0;
			toolbarsMap = tbMap;
			selectedIndex = -1;

			textListBuf = new ArrayList<String>();
			for (String str : toolbarsMap.keySet()) {
				textListBuf.add(str);
			}
		}

		public void addToolbar(String text) {
			ToolbarsConfig newtb = new ToolbarsConfig(text);
			toolbarsMap.put(text, newtb);
			textListBuf.add(text);
			scrollbar.max = textListBuf.size() - iCannotRememberWhyDoINeedThisVariable;
		}

		public ToolbarsConfig getSelectedToolbar() {
			if (selectedIndex >= 0 && selectedIndex < textListBuf.size()) return toolbarsMap.get(textListBuf.get(selectedIndex));
			return null;
		}

		public int getSelectedIndex() {
			return selectedIndex;
		}

		public void removeCurrent() {
			toolbarsMap.remove(textListBuf.get(selectedIndex));
			textListBuf.remove(selectedIndex);
			scrollbar.max = textListBuf.size() - iCannotRememberWhyDoINeedThisVariable;
			selectedIndex = -1;
		}

		public void toggleCurrent() {
			ToolbarsConfig tbcfg = getSelectedToolbar();
			if (tbcfg != null) {
				tbcfg.toggle();
			}
		}

		@Override
		public void draw(GOut g) {
			g.chcolor(0, 0, 0, 255);
			g.frect(Coord.z, sz);
			g.chcolor(255, 255, 255, 255);
			g.rect(Coord.z, sz.add(1, 1));
			g.chcolor();

			for (int i = 0; i < iCannotRememberWhyDoINeedThisVariable; i++) {
				if (i + scrollbar.val >= textListBuf.size()) continue;
				String hl = textListBuf.get(i + scrollbar.val);
				ToolbarsConfig tbc = toolbarsMap.get(hl);
				if (i + scrollbar.val == selectedIndex) {
					g.chcolor(255, 255, 0, 128);
					g.frect(new Coord(0, i * 20), new Coord(sz.x, 20));
					g.chcolor();
				}
				if (tbc.enabled)
					g.chcolor(0, 255, 0, 192);
				else
					g.chcolor(255, 255, 255, 192);
				g.atext(hl, new Coord(5, i * 20 + 10), 0, 0.5);
				g.chcolor();
			}
			super.draw(g);
		}

		@Override
		public boolean mousewheel(Coord c, int amount) {
			scrollbar.ch(amount);
			return (true);
		}

		@Override
		public boolean mousedown(Coord c, int button) {
			if (super.mousedown(c, button))
				return (true);
			if (button == 1) {
				selectedIndex = (c.y / 20) + scrollbar.val;
				if (selectedIndex >= textListBuf.size())
					selectedIndex = -1;
				changed(selectedIndex);
				return (true);
			}
			return (false);
		}

		protected void changed(int index) {

		}

		public void unsel() {
			selectedIndex = -1;
			changed(selectedIndex);
		}
	}

	public static class SlotsList extends Widget {
		private int iCannotRememberWhyDoINeedThisVariable;
		private Scrollbar scrollbar;
		private int selectedIndex;
		private ToolbarsConfig assocedToolbar;

		public SlotsList(Coord c, Coord sz, Widget parent) {
			super(c, sz, parent);

			iCannotRememberWhyDoINeedThisVariable = sz.y / 20;

			scrollbar = new Scrollbar(new Coord(sz.x, 0), sz.y, this, 0, 0);
			scrollbar.val = 0;

			selectedIndex = -1;
		}

		public void setSlot(int mode, int key) {
			TBSlot slot = getSelectedSlot();
			if (slot != null) {
				slot.sKey = key;
				slot.sMode = mode;
				slot.rebuildString();
			}
		}

		public void setupToolbar(ToolbarsConfig tb) {
			assocedToolbar = tb;
			scrollbar.max = size() - iCannotRememberWhyDoINeedThisVariable;
		}

		public int size() {
			if (assocedToolbar != null) {
				return assocedToolbar.slotList.size();
			} else return 0;
		}

		public void addSlot(int mode, int key) {
			TBSlot slot = new TBSlot(mode, key);
			if (assocedToolbar != null)
				assocedToolbar.slotList.add(slot);
			scrollbar.max = size() - iCannotRememberWhyDoINeedThisVariable;
		}

		public TBSlot getSelectedSlot() {
			if (selectedIndex >= 0 && selectedIndex < size()) return assocedToolbar.slotList.get(selectedIndex);
			return null;
		}

		public int getSelectedIndex() {
			return selectedIndex;
		}

		public void removeCurrent() {
			if (size() <= 0 || selectedIndex < 0) return;
			assocedToolbar.slotList.remove(selectedIndex);
			scrollbar.max = size() - iCannotRememberWhyDoINeedThisVariable;
			selectedIndex = -1;
		}

		@Override
		public void draw(GOut g) {
			g.chcolor(0, 0, 0, 255);
			g.frect(Coord.z, sz);
			g.chcolor(255, 255, 255, 255);
			g.rect(Coord.z, sz.add(1, 1));
			g.chcolor();

			for (int i = 0; i < iCannotRememberWhyDoINeedThisVariable; i++) {
				if (i + scrollbar.val >= size()) continue;
				TBSlot hl = assocedToolbar.slotList.get(i + scrollbar.val);
				if (i + scrollbar.val == selectedIndex) {
					g.chcolor(255, 255, 0, 128);
					g.frect(new Coord(0, i * 20), new Coord(sz.x, 20));
					g.chcolor();
				}
				g.atext(hl.getString(), new Coord(5, i * 20 + 10), 0, 0.5);
				g.chcolor();
			}
			super.draw(g);
		}

		@Override
		public boolean mousewheel(Coord c, int amount) {
			scrollbar.ch(amount);
			return (true);
		}

		@Override
		public boolean mousedown(Coord c, int button) {
			if (super.mousedown(c, button))
				return (true);
			if (button == 1) {
				selectedIndex = (c.y / 20) + scrollbar.val;
				if (selectedIndex >= size())
					selectedIndex = -1;
				changed(selectedIndex);
				return (true);
			}
			return (false);
		}

		protected void changed(int index) {

		}

		public void unsel() {
			selectedIndex = -1;
			changed(selectedIndex);
		}
	}
}
