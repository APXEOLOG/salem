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

import java.util.*;

import org.apxeolog.salem.ALS;

public class Gob implements Sprite.Owner, Rendered {
	public Coord rc, sc;
	public Coord3f sczu;
	public double a;
	public boolean virtual = false;
	int clprio = 0;
	public long id;
	public int frame, initdelay = (int) (Math.random() * 3000) + 3000;
	public final Glob glob;
	Map<Class<? extends GAttrib>, GAttrib> attr = new HashMap<Class<? extends GAttrib>, GAttrib>();
	public Collection<Overlay> ols = new LinkedList<Overlay>();

	public static class Overlay {
		public Indir<Resource> res;
		public Message sdt;
		public Sprite spr;
		public int id;
		public boolean delign = false;

		public Overlay(int id, Indir<Resource> res, Message sdt) {
			this.id = id;
			this.res = res;
			this.sdt = sdt;
			spr = null;
		}

		public Overlay(Sprite spr) {
			this.id = -1;
			this.res = null;
			this.sdt = null;
			this.spr = spr;
		}

		public static interface CDel {
			public void delete();
		}

		public static interface CUpd {
			public void update(Message sdt);
		}

		public static interface SetupMod {
			public void setupgob(GLState.Buffer buf);

			public void setupmain(RenderList rl);
		}
	}

	public Resource getres() {
		Resource res = null;
		ResDrawable dw = getattr(ResDrawable.class);
		if (dw != null) {
			res = dw.res.get();
		}
		//		else {
		//			Layered ld = getattr(Layered.class);
		//			if ((ld != null) && (ld.layers.size() > 0)) {
		//				res = ld.layers.get(0).get();
		//			}
		//		}
		return res;
	}

	public String resname() {
		Resource res = getres();
		String name = "";
		if (res != null) {
			name = res.name;
		}
		return name;
	}

	public void dumpAttrs() {
		ALS.alDebugPrint("<dumpattr>", "sz=", attr.size());
		for (GAttrib atr : attr.values()) {
			ALS.alDebugPrint(atr.getClass().getCanonicalName());
		}
		ALS.alDebugPrint("</dumpattr>");
	}

	public String getAttrs() {
		String str = "";
		for (GAttrib atr : attr.values()) {
			str += atr.getClass().getSimpleName() + ",";
		}
		return str;
	}

	public String getOverlays() {
		String str = "";
		for (Overlay ol : ols) {
			str += ol.getClass().getSimpleName() + ",";
		}
		return str;
	}

	public void dumpOverlays() {
		ALS.alDebugPrint("<dumpol>");
		for (Overlay ol : ols) {
			ALS.alDebugPrint(ol.getClass().getCanonicalName());
		}
		ALS.alDebugPrint("</dumpol>");
	}

	public Gob(Glob glob, Coord c, long id, int frame) {
		this.glob = glob;
		this.rc = c;
		this.id = id;
		this.frame = frame;
	}

	public Gob(Glob glob, Coord c) {
		this(glob, c, -1, 0);
	}

	public static interface ANotif<T extends GAttrib> {
		public void ch(T n);
	}

	public void ctick(int dt) {
		int dt2 = dt + initdelay;
		initdelay = 0;
		for (GAttrib a : attr.values()) {
			if (a instanceof Drawable)
				a.ctick(dt2);
			else
				a.ctick(dt);
		}
		for (Iterator<Overlay> i = ols.iterator(); i.hasNext();) {
			Overlay ol = i.next();
			if (ol.spr == null) {
				try {
					ol.sdt.off = 0;
					ol.spr = Sprite.create(this, ol.res.get(), ol.sdt);
				} catch (Loading e) {
				}
			} else {
				boolean done = ol.spr.tick(dt);
				if ((!ol.delign || (ol.spr instanceof Overlay.CDel)) && done)
					i.remove();
			}
		}
		if (virtual && ols.isEmpty())
			glob.oc.remove(id);
		loc.tick();
	}

	public Overlay findol(int id) {
		for (Overlay ol : ols) {
			if (ol.id == id)
				return (ol);
		}
		return (null);
	}

	public void tick() {
		for (GAttrib a : attr.values())
			a.tick();
	}

	public void dispose() {
		for (GAttrib a : attr.values())
			a.dispose();
	}

	public void move(Coord c, double a) {
		Moving m = getattr(Moving.class);
		if (m != null)
			m.move(c);
		this.rc = c;
		this.a = a;
	}

	public Coord3f getc() {
		Moving m = getattr(Moving.class);
		if (m != null)
			return (m.getc());
		else
			return (getrc());
	}

	public Coord3f getrc() {
		return (new Coord3f(rc.x, rc.y, glob.map.getcz(rc)));
	}

	private Class<? extends GAttrib> attrclass(Class<? extends GAttrib> cl) {
		while (true) {
			Class<?> p = cl.getSuperclass();
			if (p == GAttrib.class)
				return (cl);
			cl = p.asSubclass(GAttrib.class);
		}
	}

	public void setattr(GAttrib a) {
		Class<? extends GAttrib> ac = attrclass(a.getClass());
		attr.put(ac, a);
	}

	public <C extends GAttrib> C getattr(Class<C> c) {
		GAttrib attr = this.attr.get(attrclass(c));
		if (!c.isInstance(attr))
			return (null);
		return (c.cast(attr));
	}

	public void delattr(Class<? extends GAttrib> c) {
		attr.remove(attrclass(c));
	}

	@Override
	public void draw(GOut g) {
	}

	@Override
	public boolean setup(RenderList rl) {
		for (Overlay ol : ols) {
			if (ol.spr != null)
				rl.add(ol.spr, null);
		}
		for (Overlay ol : ols) {
			if (ol.spr instanceof Overlay.SetupMod)
				((Overlay.SetupMod) ol.spr).setupmain(rl);
		}
		GobHealth hlt = getattr(GobHealth.class);
		if (hlt != null)
			rl.prepc(hlt.getfx());
		Drawable d = getattr(Drawable.class);
		if (d != null)
			d.setup(rl);
		Speaking sp = getattr(Speaking.class);
		if (sp != null)
			rl.add(sp.fx, null);
		KinInfo ki = getattr(KinInfo.class);
		if (ki != null)
			rl.add(ki.fx, null);
		return (false);
	}

	@Override
	public Random mkrandoom() {
		return (new Random(id));
	}

	public Glob glob() {
		return(glob);
	}

	@Override
	public Resource.Neg getneg() {
		Drawable d = getattr(Drawable.class);
		if (d != null)
			return (d.getneg());
		return (null);
	}

	public final GLState olmod = new GLState() {
		@Override
		public void apply(GOut g) {
		}

		@Override
		public void unapply(GOut g) {
		}

		@Override
		public void prep(Buffer buf) {
			for (Overlay ol : ols) {
				if (ol.spr instanceof Overlay.SetupMod) {
					((Overlay.SetupMod) ol.spr).setupgob(buf);
				}
			}
		}
	};

	public static final GLState.Slot<Save> savepos = new GLState.Slot<Save>(
			GLState.Slot.Type.SYS, Save.class, PView.loc);

	public class Save extends GLState {
		public Matrix4f cam = new Matrix4f(), wxf = new Matrix4f(),
				mv = new Matrix4f();
		public Projection proj = null;

		@Override
		public void apply(GOut g) {
			mv.load(cam.load(g.st.cam)).mul1(wxf.load(g.st.wxf));
			Projection proj = g.st.cur(PView.proj);
			Coord3f s = proj.toscreen(mv.mul4(Coord3f.o), g.sz);
			Gob.this.sc = new Coord(s);
			Gob.this.sczu = proj.toscreen(mv.mul4(Coord3f.zu), g.sz).sub(s);
			this.proj = proj;
		}

		@Override
		public void unapply(GOut g) {
		}

		@Override
		public void prep(Buffer buf) {
			buf.put(savepos, this);
		}
	}

	public final Save save = new Save();

	public class GobLocation extends Location {
		private Coord3f c = null;
		private double a = 0.0;
		@SuppressWarnings("unused")
		private Matrix4f update = null;

		public GobLocation() {
			super(Matrix4f.id);
		}

		public void tick() {
			try {
				Coord3f c = getc();
				c.y = -c.y;
				if ((this.c == null) || !c.equals(this.c)
						|| (this.a != Gob.this.a)) {
					update(makexlate(new Matrix4f(), this.c = c).mul1(
							makerot(new Matrix4f(), Coord3f.zu,
									(float) -(this.a = Gob.this.a))));
				}
			} catch (Loading l) {
			}
		}
	}

	public final GobLocation loc = new GobLocation();
}
