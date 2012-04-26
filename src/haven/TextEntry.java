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

import java.awt.*;
import java.awt.event.KeyEvent;

public class TextEntry extends Widget {
    LineEdit buf;
    int sx;
    boolean pw = false;
    static Text.Foundry fnd = new Text.Foundry(new Font("SansSerif", Font.PLAIN, 12), Color.BLACK);
    Text.Line tcache = null;
    public String text;
	
    static {
	Widget.addtype("text", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    return(new TextEntry(c, (Coord)args[0], parent, (String)args[1]));
		}
	    });
    }
	
    public void settext(String text) {
	buf.setline(text);
    }
	
    public void uimsg(String name, Object... args) {
	if(name == "settext") {
	    settext((String)args[0]);
	} else if(name == "get") {
	    wdgmsg("text", buf.line);
	} else if(name == "pw") {
	    pw = ((Integer)args[0]) == 1;
	} else {
	    super.uimsg(name, args);
	}
    }
	
    public void draw(GOut g) {
	super.draw(g);
	String dtext;
	if(pw) {
	    dtext = "";
	    for(int i = 0; i < buf.line.length(); i++)
		dtext += "*";
	} else {
	    dtext = buf.line;
	}
	g.frect(Coord.z, sz);
	if((tcache == null) || !tcache.text.equals(dtext))
	    tcache = fnd.render(dtext);
	int cx = tcache.advance(buf.point);
	if(cx < sx) sx = cx;
	if(cx > sx + (sz.x - 1)) sx = cx - (sz.x - 1);
	g.image(tcache.tex(), new Coord(-sx, 0));
	if(hasfocus && ((System.currentTimeMillis() % 1000) > 500)) {
	    int lx = cx - sx + 1;
	    g.chcolor(0, 0, 0, 255);
	    g.line(new Coord(lx, 1), new Coord(lx, tcache.sz().y - 1), 1);
	    g.chcolor();
	}
    }
	
    public TextEntry(Coord c, Coord sz, Widget parent, String deftext) {
	super(c, sz, parent);
	buf = new LineEdit(text = deftext) {
		protected void done(String line) {
		    activate(line);
		}
		
		protected void changed() {
		    TextEntry.this.text = line;
		}
	    };
	setcanfocus(true);
    }
	
    public void activate(String text) {
	if(canactivate)
	    wdgmsg("activate", text);
    }

    public boolean type(char c, KeyEvent ev) {
	return(buf.key(ev));
    }
	
    public boolean keydown(KeyEvent e) {
	buf.key(e);
	return(true);
    }
	
    public boolean mousedown(Coord c, int button) {
	parent.setfocus(this);
	if(tcache != null) {
	    buf.point = tcache.charat(c.x + sx);
	}
	return(true);
    }
}
