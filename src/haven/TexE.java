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

import javax.media.opengl.*;

public class TexE extends TexGL {
    public final int ifmt, dfmt, dtype;
    public boolean invert;
    
    public TexE(Coord sz, int ifmt, int dfmt, int dtype, boolean invert) {
	super(sz, sz);
	this.ifmt = ifmt;
	this.dfmt = dfmt;
	this.dtype = dtype;
	this.invert = invert;
    }
    
    public TexE(Coord sz, int ifmt, int dfmt, int dtype) {
	this(sz, ifmt, dfmt, dtype, true);
    }
    
    protected void fill(GOut g) {
	GL gl = g.gl;
	gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, ifmt, tdim.x, tdim.y, 0, dfmt, dtype, null);
    }

    public void render(GOut g, Coord c, Coord ul, Coord br, Coord sz) {
	if(invert)
	    super.render(g, c, new Coord(ul.x, br.y), new Coord(br.x, ul.y), sz);
	else
	    super.render(g, c, ul, br, sz);
    }
}
