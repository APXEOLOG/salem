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
import java.net.*;
import java.util.*;

import org.apxeolog.salem.SChat;
import org.apxeolog.salem.SChatWindow;
import org.apxeolog.salem.SWindow;

public class Bootstrap implements UI.Receiver {
	UI ui;
	Session sess;
	String hostname;
	int port;
	Queue<Message> msgs = new LinkedList<Message>();
	String inituser = null;
	byte[] initcookie = null;

	public static class Message {
		int id;
		String name;
		Object[] args;

		public Message(int id, String name, Object... args) {
			this.id = id;
			this.name = name;
			this.args = args;
		}
	}

	public Bootstrap(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}

	public void setinitcookie(String username, byte[] cookie) {
		inituser = username;
		initcookie = cookie;
	}

	private String getpref(String name, String def) {
		return (Utils.getpref(name + "@" + hostname, def));
	}

	private void setpref(String name, String val) {
		Utils.setpref(name + "@" + hostname, val);
	}

	public Session run(HavenPanel hp) throws InterruptedException {
		ui = hp.newui(null);
		ui.setreceiver(this);
		ui.bind(new LoginScreen(ui.root), 1);
		new SChatWindow(new Coord(100, 100), new Coord(300, 250), ui.root);
		String loginname = getpref("loginname", "");
		boolean savepw = false;
		byte[] token = null;
		if (getpref("savedtoken", "").length() == 64)
			token = Utils.hex2byte(getpref("savedtoken", null));
		String authserver = (Config.authserv == null) ? hostname
				: Config.authserv;
		int authport = Config.authport;
		retry: do {
			byte[] cookie;
			String acctname, tokenname;
			if (initcookie != null) {
				acctname = inituser;
				cookie = initcookie;
				initcookie = null;
			} else if ((token != null)
					&& ((tokenname = getpref("tokenname", null)) != null)) {
				savepw = true;
				ui.uimsg(1, "token", loginname);
				while (true) {
					Message msg;
					synchronized (msgs) {
						while ((msg = msgs.poll()) == null)
							msgs.wait();
					}
					if (msg.id == 1) {
						if (msg.name == "login") {
							break;
						} else if (msg.name == "forget") {
							token = null;
							setpref("savedtoken", "");
							continue retry;
						}
					}
				}
				ui.uimsg(1, "prg", "Authenticating...");
				try {
					AuthClient auth = new AuthClient(authserver, authport);
					try {
						if ((acctname = auth.trytoken(tokenname, token)) == null) {
							token = null;
							setpref("savedtoken", "");
							ui.uimsg(1, "error", "Invalid save");
							continue retry;
						}
						cookie = auth.getcookie();
					} finally {
						auth.close();
					}
				} catch (java.io.IOException e) {
					ui.uimsg(1, "error", e.getMessage());
					continue retry;
				}
			} else {
				AuthClient.Credentials creds;
				ui.uimsg(1, "passwd", loginname, savepw);
				while (true) {
					Message msg;
					synchronized (msgs) {
						while ((msg = msgs.poll()) == null)
							msgs.wait();
					}
					if (msg.id == 1) {
						if (msg.name == "login") {
							creds = (AuthClient.Credentials) msg.args[0];
							savepw = (Boolean) msg.args[1];
							loginname = creds.name();
							break;
						}
					}
				}
				ui.uimsg(1, "prg", "Authenticating...");
				try {
					AuthClient auth = new AuthClient(authserver, authport);
					try {
						try {
							acctname = creds.tryauth(auth);
						} catch (AuthClient.Credentials.AuthException e) {
							ui.uimsg(1, "error", e.getMessage());
							continue retry;
						}
						cookie = auth.getcookie();
						if (savepw) {
							setpref("savedtoken",
									Utils.byte2hex(auth.gettoken()));
							setpref("tokenname", acctname);
						}
					} finally {
						auth.close();
					}
				} catch (UnknownHostException e) {
					ui.uimsg(1, "error", "Could not locate server");
					continue retry;
				} catch (java.io.IOException e) {
					ui.uimsg(1, "error", e.getMessage());
					continue retry;
				}
			}
			ui.uimsg(1, "prg", "Connecting...");
			try {
				sess = new Session(new InetSocketAddress(
						InetAddress.getByName(hostname), port), acctname,
						cookie);
			} catch (UnknownHostException e) {
				ui.uimsg(1, "error", "Could not locate server");
				continue retry;
			}
			Thread.sleep(100);
			while (true) {
				if (sess.state == "") {
					setpref("loginname", loginname);
					ui.destroy(1);
					break retry;
				} else if (sess.connfailed != 0) {
					String error;
					switch (sess.connfailed) {
					case 1:
						error = "Invalid authentication token";
						break;
					case 2:
						error = "Already logged in";
						break;
					case 3:
						error = "Could not connect to server";
						break;
					case 4:
						error = "This client is too old";
						break;
					case 5:
						error = "Authentication token expired";
						break;
					default:
						error = "Connection failed";
						break;
					}
					ui.uimsg(1, "error", error);
					sess = null;
					continue retry;
				}
				synchronized (sess) {
					sess.wait();
				}
			}
		} while (true);
		haven.error.ErrorHandler.setprop("usr", sess.username);
		return (sess);
		// (new RemoteUI(sess, ui)).start();
	}

	public void rcvmsg(int widget, String msg, Object... args) {
		synchronized (msgs) {
			msgs.add(new Message(widget, msg, args));
			msgs.notifyAll();
		}
	}
}
