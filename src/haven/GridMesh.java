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

import java.awt.Color;
import java.nio.*;
import javax.media.opengl.*;

public class GridMesh extends QuadMesh {

	public GridMesh(VertexBuf vert, short[] ind) {
		super(vert, ind);
	}

	public Color getCol(float z1, float z2) {
		float val = z1 - z1;
		if (val == 0.0f) return Color.GREEN;
		else return Color.RED;
	}
	
	public void sdraw(GOut g) {
		GL gl = g.gl;
		//VertexBuf.GLArray[] data = new VertexBuf.GLArray[vert.bufs.length];
		VertexBuf.VertexArray vbuf = null;
		//int n = 0;
		for (int i = 0; i < vert.bufs.length; i++) {
			if (vert.bufs[i] instanceof VertexBuf.VertexArray)
				vbuf = (VertexBuf.VertexArray) vert.bufs[i];
			/*else if (vert.bufs[i] instanceof VertexBuf.GLArray)
				data[n++] = (VertexBuf.GLArray) vert.bufs[i];*/
		}
		//gl.glLineWidth(5);
		//gl.glDisable(GL.GL_TEXTURE_2D);
		float zshare = 0.15f;
		gl.glBegin(GL.GL_LINES);
		for (int i = 0; i < num * 4; i += 4) {
			int v1 = indb.get(i);
			int v2 = indb.get(i + 1);
			int v3 = indb.get(i + 2);
			int v4 = indb.get(i + 3);
			
			float z1 = vbuf.getZ(v1);
			float z2 = vbuf.getZ(v1);
			float z3 = vbuf.getZ(v1);
			float z4 = vbuf.getZ(v1);
			
			float diff = 0.0f;
			/*for (int o = 0; o < n; o++)
				data[o].set(g, idx);*/
			//g.chcolor(Color.GRAY);
			
			//gl.glTexCoord2f(0.0f, 0.0f);
			g.chcolor(getCol(z1, z2));
			vbuf.setAddZ(g, v1, zshare);
			vbuf.setAddZ(g, v2, zshare);
			//gl.glTexCoord2f(1.0f, 0.0f);
			g.chcolor(getCol(z2, z3));
			vbuf.setAddZ(g, v2, zshare);
			vbuf.setAddZ(g, v3, zshare);
			//gl.glTexCoord2f(1.0f, 1.0f);
			g.chcolor(getCol(z3, z4));
			vbuf.setAddZ(g, v3, zshare);
			vbuf.setAddZ(g, v4, zshare);
			//gl.glTexCoord2f(0.0f, 1.0f);
			g.chcolor(getCol(z4, z1));
			vbuf.setAddZ(g, v4, zshare);
			vbuf.setAddZ(g, v1, zshare);
			
			//gl.glTexCoord2f(0.0f, 1.0f);
			/*vbuf.set(g, v1);
			vbuf.set(g, v2);*/
			//vbuf.set(g, v2);
			//vbuf.set(g, v1);
		}
		gl.glEnd();
		//gl.glEnable(GL.GL_TEXTURE_2D);
	}
}
