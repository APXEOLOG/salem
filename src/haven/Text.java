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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;

public class Text {
    public static final Foundry std;
    public BufferedImage img;
    public final String text;
    private Tex tex;
    public static final Color black = Color.BLACK;
    public static final Color white = Color.WHITE;
	
    static {
	std = new Foundry(new Font("SansSerif", Font.PLAIN, 10));
    }
	
    public static class Line extends Text {
	private FontMetrics m;
	
	private Line(String text) {
	    super(text);
	}
	
	public Coord base() {
	    return(new Coord(0, m.getAscent()));
	}
	
	public int advance(int pos) {
	    return(m.stringWidth(text.substring(0, pos)));
	}
	
	public int charat(int x) {
	    int l = 0, r = text.length() + 1;
	    while(true) {
		int p = (l + r) / 2;
		int a = advance(p);
		if((a < x) && (l < p)) {
		    l = p;
		} else if((a > x) && (r > p)) {
		    r = p;
		} else {
		    return(p);
		}
	    }
	}
    }

    public static int[] findspaces(String text) {
	java.util.List<Integer> l = new ArrayList<Integer>();
	for(int i = 0; i < text.length(); i++) {
	    char c = text.charAt(i);
	    if(Character.isWhitespace(c))
		l.add(i);
	}
	int[] ret = new int[l.size()];
	for(int i = 0; i < ret.length; i++)
	    ret[i] = l.get(i);
	return(ret);
    }
        
    public static class Foundry {
	private FontMetrics m;
	Font font;
	Color defcol;
	public boolean aa = false;
	private RichText.Foundry wfnd = null;
		
	public Foundry(Font f, Color defcol) {
	    font = f;
	    this.defcol = defcol;
	    BufferedImage junk = TexI.mkbuf(new Coord(10, 10));
	    Graphics tmpl = junk.getGraphics();
	    tmpl.setFont(f);
	    m = tmpl.getFontMetrics();
	}
		
	public Foundry(Font f) {
	    this(f, Color.WHITE);
	}
	
	public Foundry(String font, int psz) {
	    this(new Font(font, Font.PLAIN, psz));
	}
		
	public int height() {
	    /* XXX: Should leading go into this, when it's mostly
	     * supposed to be used for one-liners? */
	    return(m.getAscent() + m.getDescent());
	}

	private Coord strsize(String text) {
	    return(new Coord(m.stringWidth(text), height()));
	}
                
	public Text renderwrap(String text, Color c, int width) {
	    if(wfnd == null)
		wfnd = new RichText.Foundry(font, defcol);
	    wfnd.aa = aa;
	    text = RichText.Parser.quote(text);
	    if(c != null)
		text = String.format("$col[%d,%d,%d,%d]{%s}", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha(), text);
	    return(wfnd.render(text, width));
	}
                
	public Text renderwrap(String text, int width) {
	    return(renderwrap(text, null, width));
	}
                
	public Line render(String text, Color c) {
	    Line t = new Line(text);
	    Coord sz = strsize(text);
	    if(sz.x < 1)
		sz = sz.add(1, 0);
	    t.img = TexI.mkbuf(sz);
	    Graphics g = t.img.createGraphics();
	    if(aa)
		Utils.AA(g);
	    g.setFont(font);
	    g.setColor(c);
	    t.m = g.getFontMetrics();
	    g.drawString(text, 0, t.m.getAscent());
	    g.dispose();
	    return(t);
	}
		
	public Line render(String text) {
	    return(render(text, defcol));
	}
                
	public Line renderf(String fmt, Object... args) {
	    return(render(String.format(fmt, args)));
	}
    }
	
    protected Text(String text) {
	this.text = text;
    }
	
    public Coord sz() {
	return(Utils.imgsz(img));
    }
	
    public static Line render(String text, Color c) {
	return(std.render(text, c));
    }
	
    public static Line renderf(Color c, String text, Object... args) {
	return(std.render(String.format(text, args), c));
    }
	
    public static Line render(String text) {
	return(render(text, Color.WHITE));
    }
	
    public Tex tex() {
	if(tex == null)
	    tex = new TexI(img);
	return(tex);
    }
    
    public static void main(String[] args) throws Exception {
	String cmd = args[0].intern();
	if(cmd == "render") {
	    PosixArgs opt = PosixArgs.getopt(args, 1, "aw:f:s:");
	    boolean aa = false;
	    String font = "SansSerif";
	    int width = 100, size = 10;
	    for(char c : opt.parsed()) {
		if(c == 'a') {
		    aa = true;
		} else if(c == 'f') {
		    font = opt.arg;
		} else if(c == 'w') {
		    width = Integer.parseInt(opt.arg);
		} else if(c == 's') {
		    size = Integer.parseInt(opt.arg);
		}
	    }
	    Foundry f = new Foundry(font, size);
	    f.aa = aa;
	    Text t = f.renderwrap(opt.rest[0], width);
	    java.io.OutputStream out = new java.io.FileOutputStream(opt.rest[1]);
	    javax.imageio.ImageIO.write(t.img, "PNG", out);
	    out.close();
	}
    }
}
