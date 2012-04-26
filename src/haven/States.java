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
import java.awt.Color;

public abstract class States extends GLState {
    private States() {}
    
    public static final Slot<ColState> color = new Slot<ColState>(Slot.Type.DRAW, ColState.class, HavenPanel.global);
    public static class ColState extends GLState {
	public final Color c;
	public final float[] ca;
	
	public ColState(Color c) {
	    this.c = c;
	    this.ca = Utils.c2fa(c);
	}
	
	public void apply(GOut g) {
	    GL gl = g.gl;
	    gl.glColor4fv(ca, 0);
	}
	
	public int capply() {
	    return(1);
	}
	
	public void unapply(GOut g) {
	    GL gl = g.gl;
	    gl.glColor3f(1, 1, 1);
	}
	
	public int capplyfrom(GLState o) {
	    if(o instanceof ColState)
		return(1);
	    return(-1);
	}
	
	public void applyfrom(GOut g, GLState o) {
	    apply(g);
	}
	
	public void prep(Buffer buf) {
	    buf.put(color, this);
	}
	
	public boolean equals(Object o) {
	    return((o instanceof ColState) && (((ColState)o).c == c));
	}
	
	public String toString() {
	    return("ColState(" + c + ")");
	}
    }

    public static final StandAlone xray = new StandAlone(Slot.Type.GEOM, PView.proj) {
	    public void apply(GOut g) {
		g.gl.glDisable(GL.GL_DEPTH_TEST);
	    }
	    
	    public void unapply(GOut g) {
		g.gl.glEnable(GL.GL_DEPTH_TEST);
	    }
	    
	    public void prep(Buffer buf) {
		super.prep(buf);
		buf.put(Rendered.order, Rendered.last);
	    }
	};
    
    public static final StandAlone fsaa = new StandAlone(Slot.Type.SYS, PView.proj) {
	    public void apply(GOut g) {
		g.gl.glEnable(GL.GL_MULTISAMPLE);
	    }
	    
	    public void unapply(GOut g) {
		g.gl.glDisable(GL.GL_MULTISAMPLE);
	    }
	};
    
    public static final Slot<Coverage> coverage = new Slot<Coverage>(Slot.Type.DRAW, Coverage.class, PView.proj);
    public static class Coverage extends GLState {
	public final float cov;
	public final boolean inv;
	
	public Coverage(float cov, boolean inv) {
	    this.cov = cov;
	    this.inv = inv;
	}
	
	public void apply(GOut g) {
	    GL gl = g.gl;
	    gl.glEnable(GL.GL_SAMPLE_COVERAGE);
	    gl.glSampleCoverage(cov, inv);
	}
	
	public void unapply(GOut g) {
	    GL gl = g.gl;
	    gl.glSampleCoverage(1.0f, false);
	    gl.glDisable(GL.GL_SAMPLE_COVERAGE);
	}
	
	public void prep(Buffer buf) {
	    buf.put(coverage, this);
	}
    };
    
    public static final StandAlone presdepth = new StandAlone(Slot.Type.GEOM, PView.proj) {
	    public void apply(GOut g) {
		g.gl.glDepthMask(false);
	    }
	    
	    public void unapply(GOut g) {
		g.gl.glDepthMask(true);
	    }
	};
    
    public static final Slot<Fog> fog = new Slot<Fog>(Slot.Type.DRAW, Fog.class, PView.proj);
    public static class Fog extends GLState {
	public final Color c;
	public final float[] ca;
	public final float s, e;
	
	public Fog(Color c, float s, float e) {
	    this.c = c;
	    this.ca = Utils.c2fa(c);
	    this.s = s;
	    this.e = e;
	}
	
	public void apply(GOut g) {
	    GL gl = g.gl;
	    gl.glFogi(GL.GL_FOG_MODE, GL.GL_LINEAR);
	    gl.glFogf(GL.GL_FOG_START, s);
	    gl.glFogf(GL.GL_FOG_END, e);
	    gl.glFogfv(GL.GL_FOG_COLOR, ca, 0);
	    gl.glEnable(GL.GL_FOG);
	}
	
	public void unapply(GOut g) {
	    GL gl = g.gl;
	    gl.glDisable(GL.GL_FOG);
	}
	
	public void prep(Buffer buf) {
	    buf.put(fog, this);
	}
    }

    public static final Slot<DepthOffset> depthoffset = new Slot<DepthOffset>(Slot.Type.GEOM, DepthOffset.class, PView.proj);
    public static class DepthOffset extends GLState {
	public final int mode;
	public final float factor, units;
	
	public DepthOffset(int mode, float factor, float units) {
	    this.mode = mode;
	    this.factor = factor;
	    this.units = units;
	}
	
	public DepthOffset(float factor, float units) {
	    this(GL.GL_POLYGON_OFFSET_FILL, factor, units);
	}
	
	public void apply(GOut g) {
	    GL gl = g.gl;
	    gl.glPolygonOffset(factor, units);
	    gl.glEnable(mode);
	}
	
	public void unapply(GOut g) {
	    GL gl = g.gl;
	    gl.glDisable(mode);
	}
	
	public void prep(Buffer buf) {
	    buf.put(depthoffset, this);
	}
    }
    
    public static final StandAlone nullprog = new StandAlone(Slot.Type.DRAW, PView.proj) {
	    private final GLShader[] sh;
	    {
		sh = new GLShader[] {
		    new GLShader.VertexShader("void null() {}", "void null();", "null", 0),
		    new GLShader.FragmentShader("void null(vec4 res) {}", "void null(vec4 res);", "null", 0),
		};
	    }
	    
	    public void apply(GOut g) {}
	    public void unapply(GOut g) {}
	    
	    public GLShader[] shaders() {
		return(sh);
	    }
	    
	    public boolean reqshaders() {return(true);}
	};
    
    public static final Slot<GLState> adhoc = new Slot<GLState>(Slot.Type.DRAW, GLState.class, PView.wnd);
    public static class AdHoc extends GLState {
	private final GLShader[] sh;
	
	public AdHoc(GLShader[] sh) {
	    this.sh = sh;
	}
	
	public void apply(GOut g) {}
	public void unapply(GOut g) {}
	
	public GLShader[] shaders() {return(sh);}
	public boolean reqshaders() {return(sh != null);}
	
	public void prep(Buffer buf) {
	    buf.put(adhoc, this);
	}
    }
}
