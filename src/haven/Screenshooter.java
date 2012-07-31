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

import javax.imageio.*;
import java.io.*;
import java.net.*;

public class Screenshooter extends Window {
	public final URL tgt;
	public final TexI[] ss;
	private final CheckBox decobox;
	private final int w, h;
	private Label prog;
	private Coord btnc;
	private Button btn;

	public Screenshooter(Coord c, Widget parent, URL tgt, TexI[] ss) {
		super(c, Coord.z, parent, "Screenshot");
		this.tgt = tgt;
		this.ss = ss;
		this.w = Math.min(200 * ss[0].sz().x / ss[0].sz().y, 150);
		this.h = w * ss[0].sz().y / ss[0].sz().x;
		this.decobox = new CheckBox(new Coord(w, (h / 2) - CheckBox.box.sz().y
				+ 5), this, "Include interface");
		btnc = new Coord(w + 5, h - 19);
		btn = new Button(btnc, 125, this, "Upload") {
			@Override
			public void click() {
				upload();
			}
		};
		Coord csz = contentsz();
		resize(new Coord(Math.max(csz.x, 300), csz.y));
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if ((sender == this) && (msg == "close")) {
			ui.destroy(this);
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	@Override
	public void cdraw(GOut g) {
		TexI tex = ss[this.decobox.a ? 1 : 0];
		g.image(tex, new Coord(0, (asz.y - h) / 2), new Coord(w, h));
	}

	public class Uploader extends HackThread {
		private final TexI img;

		public Uploader(TexI img) {
			super("Screenshot uploader");
			this.img = img;
		}

		@Override
		public void run() {
			try {
				upload(img);
			} catch (InterruptedIOException e) {
				setstate("Cancelled");
				synchronized (ui) {
					ui.destroy(btn);
					btn = new Button(btnc, 125, Screenshooter.this, "Retry") {
						@Override
						public void click() {
							Screenshooter.this.upload();
						}
					};
				}
			} catch (IOException e) {
				setstate("Could not upload image");
				synchronized (ui) {
					ui.destroy(btn);
					btn = new Button(btnc, 125, Screenshooter.this, "Retry") {
						@Override
						public void click() {
							Screenshooter.this.upload();
						}
					};
				}
			}
		}

		private void setstate(String t) {
			synchronized (ui) {
				if (prog != null)
					ui.destroy(prog);
				prog = new Label(btnc.sub(0, 15), Screenshooter.this, t);
			}
		}

		public void upload(TexI ss) throws IOException {
			setstate("Connecting...");
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			ImageIO.write(ss.back, "PNG", buf);
			byte[] data = buf.toByteArray();
			buf = null;
			URLConnection conn = tgt.openConnection();
			conn.setDoOutput(true);
			conn.addRequestProperty("Content-Type", "image/png");
			Message auth = new Message(0);
			auth.addstring2(ui.sess.username + "/");
			auth.addbytes(ui.sess.sesskey);
			conn.addRequestProperty("Authorization",
					"Haven " + Utils.base64enc(auth.blob));
			conn.connect();
			OutputStream out = conn.getOutputStream();
			try {
				int off = 0;
				while (off < data.length) {
					setstate(String.format("Uploading (%d%%)...", (off * 100)
							/ data.length));
					int len = Math.min(1024, data.length - off);
					out.write(data, off, len);
					off += len;
				}
			} finally {
				out.close();
			}
			setstate("Awaiting response...");
			InputStream in = conn.getInputStream();
			final URL result;
			try {
				if (!conn.getContentType().equals("text/x-target-url"))
					throw (new IOException(
							"Unexpected type of reply from server"));
				byte[] b = Utils.readall(in);
				try {
					result = new URL(new String(b, "utf-8"));
				} catch (MalformedURLException e) {
					throw ((IOException) new IOException(
							"Unexpected reply from server").initCause(e));
				}
			} finally {
				in.close();
			}
			setstate("Done");
			synchronized (ui) {
				ui.destroy(btn);
				btn = new Button(btnc, 125, Screenshooter.this,
						"Open in browser") {
					@Override
					public void click() {
						if (WebBrowser.self != null)
							WebBrowser.self.show(result);
					}
				};
			}
		}
	}

	public void upload() {
		final Uploader th = new Uploader(Screenshooter.this.ss[decobox.a ? 1
				: 0]);
		th.start();
		ui.destroy(btn);
		btn = new Button(btnc, 125, this, "Cancel") {
			@Override
			public void click() {
				th.interrupt();
			}
		};
	}

	public static void take(final GameUI gameui, final URL tgt) {
		new Object() {
			TexI[] ss = { null, null };
			{
				gameui.map.delay2(new MapView.Delayed() {
					@Override
					public void run(GOut g) {
						ss[0] = new TexI(g.getimage(Coord.z, g.sz));
						ss[0].minfilter = javax.media.opengl.GL.GL_LINEAR;
						checkcomplete();
					}
				});
				gameui.ui.drawafter(new UI.AfterDraw() {
					@Override
					public void draw(GOut g) {
						ss[1] = new TexI(g.getimage(Coord.z, g.sz));
						checkcomplete();
					}
				});
			}

			private void checkcomplete() {
				if ((ss[0] != null) && (ss[1] != null)) {
					new Screenshooter(new Coord(100, 100), gameui, tgt, ss);
				}
			}
		};
	}
}
