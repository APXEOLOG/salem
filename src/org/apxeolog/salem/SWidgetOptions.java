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

import java.awt.font.TextAttribute;
import java.util.Map.Entry;

public class SWidgetOptions extends Window {
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

	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (sender == cbtn)
			unlink();
	}
}
