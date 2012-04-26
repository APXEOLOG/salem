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

public class Progress extends Widget {
    Text text;
	
    static {
	Widget.addtype("prog", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    return(new Progress(c, parent, (Integer)args[0]));
		}
	    });
    }
	
    public Progress(Coord c, Widget parent, int p) {
	super(c, new Coord(75, 20), parent);
	text = Text.renderf(FlowerMenu.pink, "%d%%", p);
    }
	
    public void draw(GOut g) {
	g.image(text.tex(), new Coord(sz.x / 2 - text.tex().sz().x / 2, 0));
    }
	
    public void uimsg(String msg, Object... args) {
	if(msg == "p") {
	    text = Text.renderf(FlowerMenu.pink, "%d%%", (Integer)args[0]);
	} else {
	    super.uimsg(msg, args);
	}
    }
}
