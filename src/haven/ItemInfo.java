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
import java.awt.image.BufferedImage;
import java.awt.Graphics;

public abstract class ItemInfo {
	public final Owner owner;

	public interface Owner {
		public Glob glob();

		public List<ItemInfo> info();
	}

	public interface ResOwner extends Owner {
		public Resource resource();
	}

	@Resource.PublishedCode(name = "tt")
	public static interface InfoFactory {
		public ItemInfo build(Owner owner, Object... args);
	}

	public ItemInfo(Owner owner) {
		this.owner = owner;
	}

	public static abstract class Tip extends ItemInfo {
		public abstract BufferedImage longtip();

		public Tip(Owner owner) {
			super(owner);
		}
	}

	public static class AdHoc extends Tip {
		public final Text str;

		public AdHoc(Owner owner, String str) {
			super(owner);
			this.str = Text.render(str);
		}

		@Override
		public BufferedImage longtip() {
			return (str.img);
		}
	}

	public static class Name extends Tip {
		public final Text str;

		public Name(Owner owner, Text str) {
			super(owner);
			this.str = str;
		}

		public Name(Owner owner, String str) {
			this(owner, Text.render(str));
		}

		@Override
		public BufferedImage longtip() {
			return (str.img);
		}
	}

	public static class Contents extends Tip {
		public final List<ItemInfo> sub;
		private static final Text.Line ch = Text.render("Contents:");

		public Contents(Owner owner, List<ItemInfo> sub) {
			super(owner);
			this.sub = sub;
		}

		@Override
		public BufferedImage longtip() {
			BufferedImage stip = longtip(sub);
			BufferedImage img = TexI.mkbuf(new Coord(stip.getWidth() + 10, stip
					.getHeight() + 15));
			Graphics g = img.getGraphics();
			g.drawImage(ch.img, 0, 0, null);
			g.drawImage(stip, 10, 15, null);
			g.dispose();
			return (img);
		}
	}

	public static BufferedImage catimgs(int margin, BufferedImage... imgs) {
		int w = 0, h = -margin;
		for (BufferedImage img : imgs) {
			if (img.getWidth() > w)
				w = img.getWidth();
			h += img.getHeight() + margin;
		}
		BufferedImage ret = TexI.mkbuf(new Coord(w, h));
		Graphics g = ret.getGraphics();
		int y = 0;
		for (BufferedImage img : imgs) {
			g.drawImage(img, 0, y, null);
			y += img.getHeight() + margin;
		}
		g.dispose();
		return (ret);
	}

	public static BufferedImage catimgsh(int margin, BufferedImage... imgs) {
		int w = -margin, h = 0;
		for (BufferedImage img : imgs) {
			if (img.getHeight() > h)
				h = img.getHeight();
			w += img.getWidth() + margin;
		}
		BufferedImage ret = TexI.mkbuf(new Coord(w, h));
		Graphics g = ret.getGraphics();
		int x = 0;
		for (BufferedImage img : imgs) {
			g.drawImage(img, x, (h - img.getHeight()) / 2, null);
			x += img.getWidth() + margin;
		}
		g.dispose();
		return (ret);
	}

	public static BufferedImage longtip(List<ItemInfo> info) {
		List<BufferedImage> buf = new ArrayList<BufferedImage>();
		for (ItemInfo ii : info) {
			if (ii instanceof Tip) {
				Tip tip = (Tip) ii;
				buf.add(tip.longtip());
			}
		}
		return (catimgs(0, buf.toArray(new BufferedImage[0])));
	}

	public static <T> T find(Class<T> cl, List<ItemInfo> il) {
		for (ItemInfo inf : il) {
			if (cl.isInstance(inf))
				return (cl.cast(inf));
		}
		return (null);
	}

	public static List<ItemInfo> buildinfo(Owner owner, Object[] rawinfo) {
		List<ItemInfo> ret = new ArrayList<ItemInfo>();
		for (Object o : rawinfo) {
			if (o instanceof Object[]) {
				Object[] a = (Object[]) o;
				Resource ttres = owner.glob().sess.getres((Integer) a[0]).get();
				InfoFactory f = ttres.layer(Resource.CodeEntry.class).get(
						InfoFactory.class);
				ret.add(f.build(owner, a));
			} else if (o instanceof String) {
				ret.add(new AdHoc(owner, (String) o));
			} else {
				throw (new ClassCastException("Unexpected object type "
						+ o.getClass() + " in item info array."));
			}
		}
		return (ret);
	}

	@SuppressWarnings("unused")
	private static String dump(Object arg) {
		if (arg instanceof Object[]) {
			StringBuilder buf = new StringBuilder();
			buf.append("[");
			boolean f = true;
			for (Object a : (Object[]) arg) {
				if (!f)
					buf.append(", ");
				buf.append(dump(a));
				f = false;
			}
			buf.append("]");
			return (buf.toString());
		} else {
			return (arg.toString());
		}
	}
}
