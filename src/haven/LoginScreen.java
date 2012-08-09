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

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;

import org.apxeolog.salem.Pair;
import org.apxeolog.salem.SUtils;

public class LoginScreen extends Widget {
	Login cur;
	Text error;
	IButton btn;
	static Text.Foundry textf, textfs;
	static Tex bg = Resource.loadtex("gfx/loginscr");
	Text progress = null;

	static {
		textf = new Text.Foundry(new java.awt.Font("Sans", java.awt.Font.PLAIN,
				16));
		textfs = new Text.Foundry(new java.awt.Font("Sans",
				java.awt.Font.PLAIN, 14));
	}

	public ArrayList<Button> login_btns = new ArrayList<Button>();
	public ArrayList<Button> del_btns = new ArrayList<Button>();


	public void spawnLoginButtons() {
		SUtils._sa_load_data();
		for (Button btn : login_btns) {
			btn.unlink();
			btn.destroy();
			btn = null;
		}
		login_btns.clear();
		for (Button btn : del_btns) {
			btn.unlink();
			btn.destroy();
			btn = null;
		}
		del_btns.clear();

		int i = 0;
		int j = 0;
		Iterator<Pair<String, String>> iterator = SUtils.accounts.values().iterator();
		while (iterator.hasNext()) {
			if(j == 20) j = 0;
			Pair<String, String> info = iterator.next();
			Button btn = new Button(Coord.z.add(0 + 140 * (i/20), j * 30), 100, this, info.getFirst()) {
				@SuppressWarnings("unchecked")
				@Override
				public void click() {
					LoginScreen ls = getparent(LoginScreen.class);
					Pair<String, String> info = (Pair<String, String>) additionalInfo;
					ls.wdgmsg(ls, "forget");
					ls.wdgmsg(ls, "login", new Object[] { new ParadoxCreds(info.getFirst(), info.getSecond()), true });
				}
			};
			btn.additionalInfo = info;
			login_btns.add(btn);

			Button btn_del = new Button(Coord.z.add(105 + 140 * (i/20), j * 30), 15, this, "X") {
				@Override
				@SuppressWarnings("unchecked")
				public void click() {
					Pair<String, String> info = (Pair<String, String>) additionalInfo;
					SUtils._sa_delete_account(info.getFirst());
					LoginScreen ls = getparent(LoginScreen.class);
					ls.spawnLoginButtons();
				}
			};
			btn_del.additionalInfo = info;
			del_btns.add(btn_del);
			i++;
			j++;
		}
	}

	public LoginScreen(Widget parent) {
		super(parent.sz.div(2).sub(bg.sz().div(2)), bg.sz(), parent);
		setfocustab(true);
		parent.setfocus(this);
		new Img(Coord.z, bg, this);
		spawnLoginButtons();

		//new SVerticalTextButton(Coord.z, Coord.z, this, "Game");
		//new SVerticalTextButton(new Coord(40, 40), Coord.z, this, "IRC");
		/*SChatWindowB cw = new SChatWindowB(Coord.z, new Coord(100, 100), this, "New chat");
		cw.addString("Hey [c=233,12,32]Joe![/c] I [b]wanna[/b] give you this [s]focking cool link[/s] https://www.google.ru/search?sugexp=chrome,mod=5&sourceid=chrome&ie=UTF-8&q=java+BB+code+parser man:[b] http://ru20.voyna-plemyon.ru/game.php?village=12178&screen=overview .[/b] How r u? [u]PEWPEWPE[/u]WPEW!", ChannelTypes.AREA);
		cw.addString("tat was too big message bro!", ChannelTypes.AREA);*/
	}

	private static abstract class Login extends Widget {
		private Login(Coord c, Coord sz, Widget parent) {
			super(c, sz, parent);
		}

		abstract Object[] data();

		abstract boolean enter();
	}

	private class Pwbox extends Login {
		TextEntry user, pass;
		CheckBox savepass;

		private Pwbox(String username, boolean save) {
			super(new Coord(345, 310), new Coord(150, 150), LoginScreen.this);
			setfocustab(true);
			new Label(new Coord(0, 0), this, "User name", textf);
			user = new TextEntry(new Coord(0, 20), new Coord(150, 20), this,
					username);
			new Label(new Coord(0, 60), this, "Password", textf);
			pass = new TextEntry(new Coord(0, 80), new Coord(150, 20), this, "");
			pass.pw = true;
			savepass = new CheckBox(new Coord(0, 110), this, "Remember me");
			savepass.a = save;
			if (user.text.equals(""))
				setfocus(user);
			else
				setfocus(pass);
		}

		@Override
		public void wdgmsg(Widget sender, String name, Object... args) {
		}

		@Override
		Object[] data() {
			return (new Object[] { new AuthClient.NativeCred(user.text, pass.text), savepass.a });
		}

		@Override
		boolean enter() {
			if (user.text.equals("")) {
				setfocus(user);
				return (false);
			} else if (pass.text.equals("")) {
				setfocus(pass);
				return (false);
			} else {
				return (true);
			}
		}

		@Override
		public boolean globtype(char k, KeyEvent ev) {
			if ((k == 'r')
					&& ((ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0)) {
				savepass.set(!savepass.a);
				return (true);
			}
			return (false);
		}
	}

	private class Pdxbox extends Login {
		TextEntry user, pass;
		CheckBox savepass;

		private Pdxbox(String username, boolean save) {
			super(new Coord(345, 310), new Coord(150, 150), LoginScreen.this);
			setfocustab(true);
			new Label(new Coord(0, 0), this, "User name", textf);
			user = new TextEntry(new Coord(0, 20), new Coord(150, 20), this,
					username);
			new Label(new Coord(0, 60), this, "Password", textf);
			pass = new TextEntry(new Coord(0, 80), new Coord(150, 20), this, "");
			pass.pw = true;
			savepass = new CheckBox(new Coord(0, 110), this, "Remember me");
			savepass.a = save;
			if (user.text.equals(""))
				setfocus(user);
			else
				setfocus(pass);
		}

		@Override
		public void wdgmsg(Widget sender, String name, Object... args) {
		}

		@Override
		Object[] data() {
			SUtils._sa_add_data(user.text, pass.text);
			return (new Object[] { new ParadoxCreds(user.text, pass.text),
					savepass.a });
		}

		@Override
		boolean enter() {
			if (user.text.equals("")) {
				setfocus(user);
				return (false);
			} else if (pass.text.equals("")) {
				setfocus(pass);
				return (false);
			} else {
				return (true);
			}
		}

		@Override
		public boolean globtype(char k, KeyEvent ev) {
			if ((k == 'r')
					&& ((ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0)) {
				savepass.set(!savepass.a);
				return (true);
			}
			return (false);
		}
	}

	private class Tokenbox extends Login {
		Text label;
		Button btn;

		private Tokenbox(String username) {
			super(new Coord(295, 310), new Coord(250, 100), LoginScreen.this);
			label = textfs.render("Identity is saved for " + username,
					java.awt.Color.WHITE);
			btn = new Button(new Coord(75, 30), 100, this, "Forget me");
		}

		@Override
		Object[] data() {
			return (new Object[0]);
		}

		@Override
		boolean enter() {
			return (true);
		}

		@Override
		public void wdgmsg(Widget sender, String name, Object... args) {
			if (sender == btn) {
				LoginScreen.this.wdgmsg("forget");
				return;
			}
			super.wdgmsg(sender, name, args);
		}

		@Override
		public void draw(GOut g) {
			g.image(label.tex(), new Coord((sz.x / 2) - (label.sz().x / 2), 0));
			super.draw(g);
		}

		@Override
		public boolean globtype(char k, KeyEvent ev) {
			if ((k == 'f')
					&& ((ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0)) {
				LoginScreen.this.wdgmsg("forget");
				return (true);
			}
			return (false);
		}
	}

	private void mklogin() {
		synchronized (ui) {
			btn = new IButton(new Coord(373, 460), this,
					Resource.loadimg("gfx/hud/buttons/loginu"),
					Resource.loadimg("gfx/hud/buttons/logind"));
			progress(null);
		}
	}

	private void error(String error) {
		synchronized (ui) {
			if (this.error != null)
				this.error = null;
			if (error != null)
				this.error = textf.render(error, java.awt.Color.RED);
		}
	}

	private void progress(String p) {
		synchronized (ui) {
			if (progress != null)
				progress = null;
			if (p != null)
				progress = textf.render(p, java.awt.Color.WHITE);
		}
	}

	private void clear() {
		if (cur != null) {
			ui.destroy(cur);
			cur = null;
			ui.destroy(btn);
			btn = null;
		}
		progress(null);
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (sender == btn) {
			if (cur.enter())
				super.wdgmsg("login", cur.data());
			return;
		}
		super.wdgmsg(sender, msg, args);
	}

	@Override
	public void uimsg(String msg, Object... args) {
		synchronized (ui) {
			if (msg == "passwd") {
				clear();
				if (Config.authmech.equals("native")) {
					cur = new Pwbox((String) args[0], (Boolean) args[1]);
				} else if (Config.authmech.equals("paradox")) {
					cur = new Pdxbox((String) args[0], (Boolean) args[1]);
				} else {
					throw (new RuntimeException("Unknown authmech `"
							+ Config.authmech + "' specified"));
				}
				mklogin();
			} else if (msg == "token") {
				clear();
				cur = new Tokenbox((String) args[0]);
				mklogin();
			} else if (msg == "error") {
				error((String) args[0]);
			} else if (msg == "prg") {
				error(null);
				clear();
				progress((String) args[0]);
			}
		}
	}

	@Override
	public void presize() {
		c = parent.sz.div(2).sub(sz.div(2));
	}

	@Override
	public void draw(GOut g) {
		super.draw(g);
		if (error != null)
			g.image(error.tex(), new Coord(420 - (error.sz().x / 2), 500));
		if (progress != null)
			g.image(progress.tex(), new Coord(420 - (progress.sz().x / 2), 350));
	}

	@Override
	public boolean type(char k, KeyEvent ev) {
		if (k == 10) {
			if ((cur != null) && cur.enter())
				wdgmsg("login", cur.data());
			return (true);
		}
		return (super.type(k, ev));
	}
}
