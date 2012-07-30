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

import java.awt.event.KeyEvent;

import org.apxeolog.salem.HConfig;

public class RootWidget extends ConsoleHost {
	public static Resource defcurs = Resource.load("gfx/hud/curs/arw");
	Logout logout = null;
	Profile gprof;
	boolean afk = false;

	public RootWidget(UI ui, Coord sz) {
		super(ui, new Coord(0, 0), sz);
		setfocusctl(true);
		cursor = defcurs;
	}

	public boolean globtype(char key, KeyEvent ev) {
		if (!super.globtype(key, ev)) {
			if (ev.getKeyCode() == 89 && ev.isControlDown()) {
				HConfig.cl_render_on = !HConfig.cl_render_on;
			} else if(ev.getKeyCode() == 90 && ev.isControlDown()) {
				GameUI gui = ui.root.findchild(GameUI.class);
				if (gui != null) {
					gui.toggleTilify();
				}
				HConfig.saveConfig();
			} else if(ev.getKeyCode() == 71 && ev.isControlDown()) { // Ctrl + G 
				GameUI gui = ui.root.findchild(GameUI.class);
				if (gui != null) {
					gui.toggleGrid();
				}
				HConfig.saveConfig();
			} else if(ev.getKeyCode() == 76 && ev.isAltDown()) {
				if (ui.sess != null) ui.sess.close();
				if(!HConfig.cl_render_on) {
					HConfig.cl_render_on = true;
					HConfig.saveConfig();
				}
			} /*else if (key == '`') {
				//Config.dbtext = true;
				//new Profwnd(new Coord(100, 100), this, gprof, "Glob prof");
			} */else if (Config.profile && (key == '~')) {
				GameUI gi = findchild(GameUI.class);
				if ((gi != null) && (gi.map != null))
					new Profwnd(new Coord(100, 100), this, gi.map.prof,
							"MV prof");
			} else if (key == ':') {
				entercmd();
			} else if (key != 0) {
				wdgmsg("gk", (int) key);
			}
		}
		return (true);
	}

	public void draw(GOut g) {
		super.draw(g);
		drawcmd(g, new Coord(20, sz.y - 20));
	}

	public void error(String msg) {
	}
}
