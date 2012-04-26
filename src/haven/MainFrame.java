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

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.apxeolog.salem.HConfig;

public class MainFrame extends Frame implements Runnable, Console.Directory {
	HavenPanel p;
	private final ThreadGroup g;
	public final Thread mt;
	DisplayMode fsmode = null, prefs = null;

	static {
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager
					.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}
	}

	DisplayMode findmode(int w, int h) {
		GraphicsDevice dev = getGraphicsConfiguration().getDevice();
		if (!dev.isFullScreenSupported())
			return (null);
		DisplayMode b = null;
		for (DisplayMode m : dev.getDisplayModes()) {
			int d = m.getBitDepth();
			if ((m.getWidth() == w)
					&& (m.getHeight() == h)
					&& ((d == 24) || (d == 32) || (d == DisplayMode.BIT_DEPTH_MULTI))) {
				if ((b == null)
						|| (d > b.getBitDepth())
						|| ((d == b.getBitDepth()) && (m.getRefreshRate() > b
								.getRefreshRate())))
					b = m;
			}
		}
		return (b);
	}

	public void setfs() {
		GraphicsDevice dev = getGraphicsConfiguration().getDevice();
		if (prefs != null)
			return;
		prefs = dev.getDisplayMode();
		try {
			setVisible(false);
			dispose();
			setUndecorated(true);
			setVisible(true);
			dev.setFullScreenWindow(this);
			dev.setDisplayMode(fsmode);
			pack();
		} catch (Exception e) {
			throw (new RuntimeException(e));
		}
	}

	public void setwnd() {
		GraphicsDevice dev = getGraphicsConfiguration().getDevice();
		if (prefs == null)
			return;
		try {
			dev.setDisplayMode(prefs);
			dev.setFullScreenWindow(null);
			setVisible(false);
			dispose();
			setUndecorated(false);
			setVisible(true);
		} catch (Exception e) {
			throw (new RuntimeException(e));
		}
		prefs = null;
	}

	public boolean hasfs() {
		return (prefs != null);
	}

	private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
	{
		cmdmap.put("sz", new Console.Command() {
			public void run(Console cons, String[] args) {
				if (args.length == 3) {
					int w = Integer.parseInt(args[1]), h = Integer
							.parseInt(args[2]);
					p.setSize(w, h);
					pack();
					Utils.setprefc("wndsz", new Coord(w, h));
				} else if (args.length == 2) {
					if (args[1].equals("dyn")) {
						setResizable(true);
						Utils.setprefb("wndlock", false);
					} else if (args[1].equals("lock")) {
						setResizable(false);
						Utils.setprefb("wndlock", true);
					}
				}
			}
		});
		cmdmap.put("fsmode", new Console.Command() {
			public void run(Console cons, String[] args) throws Exception {
				if (args.length == 3) {
					DisplayMode mode = findmode(Integer.parseInt(args[1]),
							Integer.parseInt(args[2]));
					if (mode == null)
						throw (new Exception("No such mode is available"));
					fsmode = mode;
					Utils.setprefc("fsmode",
							new Coord(mode.getWidth(), mode.getHeight()));
				}
			}
		});
		cmdmap.put("fs", new Console.Command() {
			public void run(Console cons, String[] args) {
				if (args.length >= 2) {
					Runnable r;
					if (Utils.atoi(args[1]) != 0) {
						r = new Runnable() {
							public void run() {
								setfs();
							}
						};
					} else {
						r = new Runnable() {
							public void run() {
								setwnd();
							}
						};
					}
					getToolkit().getSystemEventQueue().invokeLater(r);
				}
			}
		});
	}

	public Map<String, Console.Command> findcmds() {
		return (cmdmap);
	}

	private void seticon() {
		Image icon;
		try {
			InputStream data = MainFrame.class.getResourceAsStream("icon.gif");
			icon = javax.imageio.ImageIO.read(data);
			data.close();
		} catch (IOException e) {
			throw (new Error(e));
		}
		setIconImage(icon);
	}

	public MainFrame(Coord isz) {
		super("Salem :: BDSM 1.3");
		Coord sz;
		if (isz == null) {
			sz = Utils.getprefc("wndsz", new Coord(800, 600));
			if (sz.x < 640)
				sz.x = 640;
			if (sz.y < 480)
				sz.y = 480;
		} else {
			sz = isz;
		}
		this.g = new ThreadGroup(HackThread.tg(), "Haven client");
		this.mt = new HackThread(this.g, this, "Haven main thread");
		p = new HavenPanel(sz.x, sz.y);
		if (fsmode == null) {
			Coord pfm = Utils.getprefc("fsmode", null);
			if (pfm != null)
				fsmode = findmode(pfm.x, pfm.y);
		}
		if (fsmode == null) {
			DisplayMode cm = getGraphicsConfiguration().getDevice()
					.getDisplayMode();
			fsmode = findmode(cm.getWidth(), cm.getHeight());
		}
		if (fsmode == null)
			fsmode = findmode(800, 600);
		add(p);
		pack();
		setResizable(!Utils.getprefb("wndlock", false));
		p.requestFocus();
		seticon();
		setVisible(true);
		p.init();
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				g.interrupt();
			}
		});
		if ((isz == null) && Utils.getprefb("wndmax", false))
			setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
	}

	private void savewndstate() {
		if (prefs == null) {
			if (getExtendedState() == NORMAL)
			/*
			 * Apparent, getSize attempts to return the "outer size" of the
			 * window, including WM decorations, even though setSize sets the
			 * "inner size" of the window. Therefore, use the Panel's size
			 * instead; it ought to correspond to the inner size at all times.
			 */{
				Dimension dim = p.getSize();
				Utils.setprefc("wndsz", new Coord(dim.width, dim.height));
			}
			Utils.setprefb("wndmax", (getExtendedState() & MAXIMIZED_BOTH) != 0);
		}
	}

	public void run() {
		if (Thread.currentThread() != this.mt)
			throw (new RuntimeException(
					"MainFrame is being run from an invalid context"));
		Thread ui = new HackThread(p, "Haven UI thread");
		ui.start();
		try {
			try {
				while (true) {
					Bootstrap bill = new Bootstrap(Config.defserv,
							Config.mainport);
					if ((Config.authuser != null) && (Config.authck != null)) {
						bill.setinitcookie(Config.authuser, Config.authck);
						Config.authck = null;
					}
					Session sess = bill.run(p);
					RemoteUI rui = new RemoteUI(sess);
					rui.run(p.newui(sess));
				}
			} catch (InterruptedException e) {
			}
			savewndstate();
		} finally {
			ui.interrupt();
			dispose();
		}
	}

	public static void setupres() {
		if (ResCache.global != null)
			Resource.addcache(ResCache.global);
		if (Config.resurl != null)
			Resource.addurl(Config.resurl);
		if (ResCache.global != null) {
			try {
				Resource.loadlist(ResCache.global.fetch("tmp/allused"), -10);
			} catch (IOException e) {
			}
		}
		if (!Config.nopreload) {
			try {
				InputStream pls;
				pls = Resource.class.getResourceAsStream("res-preload");
				if (pls != null)
					Resource.loadlist(pls, -5);
				pls = Resource.class.getResourceAsStream("res-bgload");
				if (pls != null)
					Resource.loadlist(pls, -10);
			} catch (IOException e) {
				throw (new Error(e));
			}
		}
	}

	static {
		WebBrowser.self = JnlpBrowser.create();
	}

	private static void javabughack() throws InterruptedException {
		/* Work around a stupid deadlock bug in AWT. */
		try {
			javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					PrintStream bitbucket = new PrintStream(
							new ByteArrayOutputStream());
					bitbucket.print(LoginScreen.textf);
					bitbucket.print(LoginScreen.textfs);
				}
			});
		} catch (java.lang.reflect.InvocationTargetException e) {
			/* Oh, how I love Swing! */
			throw (new Error(e));
		}
		/* Work around another deadl bug in Sun's JNLP client. */
		javax.imageio.spi.IIORegistry.getDefaultInstance();
	}

	private static void main2(String[] args) {
		HConfig.loadConfig();
		Config.cmdline(args);
		try {
			javabughack();
		} catch (InterruptedException e) {
			return;
		}
		setupres();
		MainFrame f = new MainFrame(null);
		if (Utils.getprefb("fullscreen", false))
			f.setfs();
		f.mt.start();
		try {
			f.mt.join();
		} catch (InterruptedException e) {
			f.g.interrupt();
			return;
		}
		dumplist(Resource.loadwaited, Config.loadwaited);
		dumplist(Resource.cached(), Config.allused);
		if (ResCache.global != null) {
			try {
				Collection<Resource> used = new LinkedList<Resource>();
				for (Resource res : Resource.cached()) {
					if (res.prio >= 0) {
						try {
							res.checkerr();
						} catch (Exception e) {
							continue;
						}
						used.add(res);
					}
				}
				Writer w = new OutputStreamWriter(
						ResCache.global.store("tmp/allused"), "UTF-8");
				try {
					Resource.dumplist(used, w);
				} finally {
					w.close();
				}
			} catch (IOException e) {
			}
		}
		System.exit(0);
	}

	public static void main(final String[] args) {
		/* Set up the error handler as early as humanly possible. */
		ThreadGroup g = new ThreadGroup("Haven main group");
		String ed;
		if (!(ed = Utils.getprop("haven.errorurl",
				"http://plymouth.seatribe.se/java/error")).equals("")) {
			try {
				final haven.error.ErrorHandler hg = new haven.error.ErrorHandler(
						new java.net.URL(ed));
				hg.sethandler(new haven.error.ErrorGui(null) {
					public void errorsent() {
						hg.interrupt();
					}
				});
				g = hg;
			} catch (java.net.MalformedURLException e) {
			}
		}
		Thread main = new HackThread(g, new Runnable() {
			public void run() {
				main2(args);
			}
		}, "Haven main thread");
		main.start();
	}

	private static void dumplist(Collection<Resource> list, String fn) {
		try {
			if (fn != null) {
				Writer w = new OutputStreamWriter(new FileOutputStream(fn),
						"UTF-8");
				try {
					Resource.dumplist(list, w);
				} finally {
					w.close();
				}
			}
		} catch (IOException e) {
			throw (new RuntimeException(e));
		}
	}
}
