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

import java.lang.ref.WeakReference;
import java.util.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;

import org.apxeolog.salem.ALS;
import org.apxeolog.salem.ChatWrapper;
import org.apxeolog.salem.HConfig;

public class ChatUI extends Widget {
	public static final RichText.Foundry fnd = new RichText.Foundry(
			TextAttribute.FAMILY, "SansSerif", TextAttribute.SIZE, 9,
			TextAttribute.FOREGROUND, Color.BLACK);
	public static final Text.Foundry qfnd = new Text.Foundry(new java.awt.Font(
			"SansSerif", java.awt.Font.PLAIN, 12), new java.awt.Color(192, 255,
			192));
	public static final int selw = 100;
	public Channel sel = null;
	private final Selector chansel;
	public boolean expanded = false;
	private Coord base;
	private QuickLine qline = null;
	private final LinkedList<Notification> notifs = new LinkedList<Notification>();

	public ChatUI(Coord c, int w, Widget parent) {
		super(c.add(0, -50), new Coord(w, 50), parent);
		chansel = new Selector(Coord.z, new Coord(selw, sz.y));
		chansel.hide();
		base = c;
		setfocusctl(true);
		setcanfocus(false);
	}

	public static abstract class Channel extends Widget {
		public final List<Message> msgs = new LinkedList<Message>();
		private final Scrollbar sb;

		public static abstract class Message {
			public final long time = System.currentTimeMillis();

			public abstract Tex tex();

			public abstract Coord sz();
		}

		public String name(int bid) {
			BuddyWnd.Buddy b = getparent(GameUI.class).buddies.find(bid);
			if (b == null)
				return "???";
			else
				return b.name;
		}

		public static class SimpleMessage extends Message {
			private final Text t;

			public SimpleMessage(String text, Color col, int w) {
				if (col == null)
					this.t = fnd.render(RichText.Parser.quote(text), w);
				else
					this.t = fnd.render(RichText.Parser.quote(text), w,
							TextAttribute.FOREGROUND, col);
			}

			public Tex tex() {
				return (t.tex());
			}

			public Coord sz() {
				return (t.sz());
			}
		}

		public Channel(Coord c, Coord sz, Widget parent) {
			super(c, sz, parent);
			sb = new Scrollbar(new Coord(sz.x, 0), ih(), this, 0, -ih());
		}

		public Channel(Widget parent) {
			this(new Coord(selw, 0), parent.sz.sub(selw, 0), parent);
		}

		public void append(Message msg) {
			synchronized (msgs) {
				msgs.add(msg);
				int y = 0;
				for (Message m : msgs)
					y += m.sz().y;
				boolean b = sb.val >= sb.max;
				sb.max = y - ih();
				if (b)
					sb.val = sb.max;
			}
		}

		public void append(String line, Color col) {
			append(new SimpleMessage(line, col, iw()));
		}

		public int iw() {
			return (sz.x - sb.sz.x);
		}

		public int ih() {
			return (sz.y);
		}

		public void draw(GOut g) {
			g.chcolor(0, 0, 0, 255);
			g.frect(Coord.z, sz);
			g.chcolor();
			int y = 0;
			synchronized (msgs) {
				for (Message msg : msgs) {
					int y1 = y - sb.val;
					int y2 = y1 + msg.sz().y;
					if ((y2 > 0) && (y1 < ih()))
						g.image(msg.tex(), new Coord(0, y1));
					y += msg.sz().y;
				}
			}
			sb.max = y - ih();
			super.draw(g);
		}

		public boolean mousewheel(Coord c, int amount) {
			sb.ch(amount * 15);
			return (true);
		}

		public void resize(Coord sz) {
			super.resize(sz);
			if (sb != null) {
				sb.resize(ih());
				sb.move(new Coord(sz.x, 0));
				int y = 0;
				for (Message m : msgs)
					y += m.sz().y;
				boolean b = sb.val >= sb.max;
				sb.max = y - ih();
				if (b)
					sb.val = sb.max;
			}
		}

		public void notify(Message msg) {
			getparent(ChatUI.class).notify(this, msg);
		}

		public abstract String name();
	}

	public static class Log extends Channel {
		private final String name;

		public Log(Widget parent, String name) {
			super(parent);
			this.name = name;
		}

		public String name() {
			return (name);
		}
	}

	public static abstract class EntryChannel extends Channel {
		private final TextEntry in;

		public EntryChannel(Widget parent) {
			super(parent);
			setfocusctl(true);
			this.in = new TextEntry(new Coord(0, sz.y - 20),
					new Coord(sz.x, 20), this, "") {
				public void activate(String text) {
					if (text.length() > 0)
						send(text);
					settext("");
				}
			};
		}

		public int ih() {
			return (sz.y - 20);
		}

		public void resize(Coord sz) {
			super.resize(sz);
			if (in != null) {
				in.c = new Coord(0, this.sz.y - 20);
				in.resize(new Coord(this.sz.x, 20));
			}
		}

		public void send(String text) {
			wdgmsg("msg", text);
		}
	}

	public static class MultiChat extends EntryChannel {
		protected final String name;
		private final boolean notify;

		public class NamedMessage extends Message {
			public final int from;
			public final String text;
			public final int w;
			public final Color col;
			private String cn;
			private Text r = null;

			public NamedMessage(int from, String text, Color col, int w) {
				this.from = from;
				this.text = text;
				this.w = w;
				this.col = col;
			}

			public Tex tex() {
				BuddyWnd.Buddy b = getparent(GameUI.class).buddies.find(from);
				String nm = (b == null) ? "???" : (b.name);
				if ((r == null) || !nm.equals(cn)) {
					r = fnd.render(RichText.Parser.quote(String.format(
							"%s: %s", nm, text)), w, TextAttribute.FOREGROUND,
							col);
					cn = nm;
				}
				return (r.tex());
			}

			public Coord sz() {
				if (r == null)
					return (tex().sz());
				else
					return (r.sz());
			}
		}

		public class MyMessage extends SimpleMessage {
			public MyMessage(String text, int w) {
				super(text, new Color(192, 192, 255), w);
			}
		}

		public MultiChat(Widget parent, String name, boolean notify) {
			super(parent);
			this.name = name;
			this.notify = notify;
		}

		public void uimsg(String msg, Object... args) {
			if (msg == "msg") {
				Integer from = null;
				String line = null;
				if (args[0] instanceof String) {
					line = (String) args[0];
				} else {
					from = (Integer) args[0];
					line = (String) args[1];
				}
				
				// BDSChat Wrap
				Color textColor = name.equals("Village") ? Color.GREEN : Color.WHITE;
				if (from != null) {
					BuddyWnd.Buddy buddy = getparent(GameUI.class).buddies.find(from.intValue());
					String hname = (buddy == null) ? "???" : (buddy.name);
					Color hcolor = (buddy == null) ? Color.WHITE : (BuddyWnd.gc[buddy.group]);
					getparent(GameUI.class).bdsChat.recieveMessage(line, textColor, hname, hcolor, this);
				} else {
					getparent(GameUI.class).bdsChat.recieveMessage(line, textColor, Config.currentCharName, Color.WHITE, this);
				}
				
				if (from != null) {
					Message cmsg = new NamedMessage(from.intValue(), line, Color.WHITE, iw());
					append(cmsg);
					if (notify)
						notify(cmsg);
				} else if (from == null) {
					append(new MyMessage(line, iw()));
				}
			}
		}

		public String name() {
			return (name);
		}
	}

	public static class PartyChat extends MultiChat {
		public PartyChat(Widget parent) {
			super(parent, "Party", true);
		}

		public void uimsg(String msg, Object... args) {
			if (msg == "msg") {
				Integer from = (Integer) args[0];
				int gobid = (Integer) args[1];
				String line = (String) args[2];
				Color col = Color.WHITE;
				synchronized (ui.sess.glob.party.memb) {
					Party.Member pm = ui.sess.glob.party.memb.get((long) gobid);
					if (pm != null)
						col = pm.col;
				}
				
				// BDSChat Wrap
				if (from != null) {
					BuddyWnd.Buddy buddy = getparent(GameUI.class).buddies.find(from.intValue());
					String hname = (buddy == null) ? "???" : (buddy.name);
					Color hcolor = (buddy == null) ? Color.WHITE : (BuddyWnd.gc[buddy.group]);
					getparent(GameUI.class).bdsChat.recieveMessage(line, Color.CYAN, hname, hcolor, this);
				} else {
					getparent(GameUI.class).bdsChat.recieveMessage(line, Color.CYAN, Config.currentCharName, Color.WHITE, this);
				}
				
				if (from != null) {
					Message cmsg = new NamedMessage(from.intValue(), line, col, iw());
					append(cmsg);
					notify(cmsg);
				} else if (from == null) {
					append(new MyMessage(line, iw()));
				}
			}
		}
	}

	public static class PrivChat extends EntryChannel {
		private final int other;

		public class InMessage extends SimpleMessage {
			public InMessage(String text, int w) {
				super(text, new Color(192, 0, 0, 255), w);
			}
		}

		public class OutMessage extends SimpleMessage {
			public OutMessage(String text, int w) {
				super(text, new Color(0, 0, 192, 255), w);
			}
		}

		public PrivChat(Widget parent, int other) {
			super(parent);
			this.other = other;
			if (getparent(GameUI.class).bdsChat.isWaitingForChat()) {
				getparent(GameUI.class).bdsChat.receiveChat(new WeakReference<Widget>(this));
			}
		}

		public void uimsg(String msg, Object... args) {
			if (msg == "msg") {
				String t = (String) args[0];
				String line = (String) args[1];
				
				// BDSChat Wrap
				if (t.equals("in")) {
					BuddyWnd.Buddy buddy = getparent(GameUI.class).buddies.find(other);
					String hname = (buddy == null) ? "???" : (buddy.name);
					Color hcolor = (buddy == null) ? Color.WHITE : (BuddyWnd.gc[buddy.group]);
					getparent(GameUI.class).bdsChat.recieveMessage(line, Color.PINK, hname, hcolor, this);
				} else {
					getparent(GameUI.class).bdsChat.recieveMessage(line, Color.PINK, Config.currentCharName, Color.WHITE, this);
				}
				
				if (t.equals("in")) {
					Message cmsg = new InMessage(line, iw());
					append(cmsg);
					notify(cmsg);
				} else if (t.equals("out")) {
					append(new OutMessage(line, iw()));
				}
			} else if (msg == "err") {
				String err = (String) args[0];
				
				// BDSChat Wrap
				BuddyWnd.Buddy buddy = getparent(GameUI.class).buddies.find(other);
				String hname = (buddy == null) ? "???" : (buddy.name);
				Color hcolor = (buddy == null) ? Color.WHITE : (BuddyWnd.gc[buddy.group]);
				getparent(GameUI.class).bdsChat.recieveMessage(err, Color.RED, hname, hcolor, this);
				
				Message cmsg = new SimpleMessage(err, Color.RED, iw());
				append(cmsg);
				notify(cmsg);
			}
		}

		public String name() {
			BuddyWnd.Buddy b = getparent(GameUI.class).buddies.find(other);
			if (b == null)
				return ("???");
			else
				return (b.name);
		}
	}

	static {
		addtype("mchat", new WidgetFactory() {
			public Widget create(Coord c, Widget parent, Object[] args) {
				String name = (String) args[0];
				boolean notify = ((Integer) args[1]) != 0;
				return (new MultiChat(parent, name, notify));
			}
		});
		addtype("pchat", new WidgetFactory() {
			public Widget create(Coord c, Widget parent, Object[] args) {
				return (new PartyChat(parent));
			}
		});
		addtype("pmchat", new WidgetFactory() {
			public Widget create(Coord c, Widget parent, Object[] args) {
				int other = (Integer) args[0];
				
				return (new PrivChat(parent, other));
			}
		});
	}

	public Widget makechild(String type, Object[] pargs, Object[] cargs) {
		return (gettype(type).create(Coord.z, this, cargs));
	}

	private class Selector extends Widget {
		public final Text.Foundry nf = new Text.Foundry("SansSerif", 10);
		private final List<DarkChannel> chls = new ArrayList<DarkChannel>();
		private int s = 0;

		private class DarkChannel {
			public final Channel chan;
			public Text rname;

			private DarkChannel(Channel chan) {
				this.chan = chan;
			}
		}

		public Selector(Coord c, Coord sz) {
			super(c, sz, ChatUI.this);
		}

		private void add(Channel chan) {
			synchronized (chls) {
				chls.add(new DarkChannel(chan));
			}
		}

		private void rm(Channel chan) {
			synchronized (chls) {
				for (Iterator<DarkChannel> i = chls.iterator(); i.hasNext();) {
					DarkChannel c = i.next();
					if (c.chan == chan)
						i.remove();
				}
			}
		}

		public void draw(GOut g) {
			g.chcolor(64, 64, 64, 192);
			g.frect(Coord.z, sz);
			int i = s;
			int y = 0;
			synchronized (chls) {
				while (i < chls.size()) {
					DarkChannel ch = chls.get(i);
					if (ch.chan == sel) {
						g.chcolor(128, 128, 192, 255);
						g.frect(new Coord(0, y), new Coord(sz.x, 19));
					}
					g.chcolor(255, 255, 255, 255);
					if ((ch.rname == null)
							|| !ch.rname.text.equals(ch.chan.name()))
						ch.rname = nf.render(ch.chan.name());
					g.aimage(ch.rname.tex(), new Coord(sz.x / 2, y + 10), 0.5,
							0.5);
					g.line(new Coord(5, y + 19), new Coord(sz.x - 5, y + 19), 1);
					y += 20;
					if (y >= sz.y)
						break;
					i++;
				}
			}
			g.chcolor();
		}

		public boolean up() {
			Channel prev = null;
			for (DarkChannel ch : chls) {
				if (ch.chan == sel) {
					if (prev != null) {
						select(prev);
						return (true);
					} else {
						return (false);
					}
				}
				prev = ch.chan;
			}
			return (false);
		}

		public boolean down() {
			for (Iterator<DarkChannel> i = chls.iterator(); i.hasNext();) {
				DarkChannel ch = i.next();
				if (ch.chan == sel) {
					if (i.hasNext()) {
						select(i.next().chan);
						return (true);
					} else {
						return (false);
					}
				}
			}
			return (false);
		}

		private Channel bypos(Coord c) {
			int i = (c.y / 20) - s;
			if ((i >= 0) && (i < chls.size()))
				return (chls.get(i).chan);
			return (null);
		}

		public boolean mousedown(Coord c, int button) {
			if (button == 1) {
				Channel chan = bypos(c);
				if (chan != null)
					select(chan);
			}
			return (true);
		}

		public boolean mousewheel(Coord c, int amount) {
			s += amount;
			if (s >= chls.size() - (sz.y / 20))
				s = chls.size() - (sz.y / 20);
			if (s < 0)
				s = 0;
			return (true);
		}
	}

	public void select(Channel chan) {
		Channel prev = sel;
		sel = chan;
		if (expanded) {
			if (prev != null)
				prev.hide();
			sel.show();
			resize(sz);
		}
	}

	private class Notification {
		public final Channel chan;
		public final Text chnm;
		public final Channel.Message msg;
		public final long time = System.currentTimeMillis();

		private Notification(Channel chan, Channel.Message msg) {
			this.chan = chan;
			this.msg = msg;
			this.chnm = chansel.nf.render(chan.name(), Color.WHITE);
		}
	}

	private Text.Line rqline = null;
	private int rqpre;

	public void drawsmall(GOut g, Coord br, int h) {
		Coord c;
		if (qline != null) {
			if ((rqline == null) || !rqline.text.equals(qline.line)) {
				String pre = String.format("%s> ", qline.chan.name());
				rqline = qfnd.render(pre + qline.line);
				rqpre = pre.length();
			}
			c = br.sub(0, 20);
			g.image(rqline.tex(), c);
			int lx = rqline.advance(qline.point + rqpre);
			g.line(new Coord(br.x + lx + 1, br.y - 18), new Coord(
					br.x + lx + 1, br.y - 6), 1);
		} else {
			c = br.sub(0, 5);
		}
		long now = System.currentTimeMillis();
		synchronized (notifs) {
			for (Iterator<Notification> i = notifs.iterator(); i.hasNext();) {
				Notification n = i.next();
				if (now - n.time > 5000) {
					i.remove();
					continue;
				}
				if ((c.y -= n.msg.sz().y) < br.y - h)
					break;
				g.image(n.chnm.tex(), c, br.sub(0, h), br.add(selw - 10, 0));
				g.image(n.msg.tex(), c.add(selw, 0));
			}
		}
	}

	public static final Resource notifsfx = Resource.load("sfx/tick");

	public void notify(Channel chan, Channel.Message msg) {
		synchronized (notifs) {
			notifs.addFirst(new Notification(chan, msg));
		}
		Audio.play(notifsfx);
	}

	public void newchild(Widget w) {
		if (w instanceof Channel) {
			Channel chan = (Channel) w;
			select(chan);
			chansel.add(chan);
			if (!expanded)
				chan.hide();
		}
	}

	public void cdestroy(Widget w) {
		if (w instanceof Channel) {
			Channel chan = (Channel) w;
			if (chan == sel)
				sel = null;
			chansel.rm(chan);
		}
	}

	public void resize(Coord sz) {
		super.resize(sz);
		this.c = base.add(0, -this.sz.y);
		chansel.resize(new Coord(selw, this.sz.y));
		if (sel != null)
			sel.resize(new Coord(this.sz.x - selw, this.sz.y));
	}

	public void resize(int w) {
		resize(new Coord(w, sz.y));
	}

	public void move(Coord base) {
		this.c = (this.base = base).add(0, -sz.y);
	}

	private void expand() {
		resize(new Coord(sz.x, 100));
		setcanfocus(true);
		if (sel != null)
			sel.show();
		chansel.show();
		expanded = true;
	}

	private void contract() {
		resize(new Coord(sz.x, 50));
		setcanfocus(false);
		if (sel != null)
			sel.hide();
		chansel.hide();
		expanded = false;
	}

	private class QuickLine extends LineEdit {
		public final EntryChannel chan;

		private QuickLine(EntryChannel chan) {
			this.chan = chan;
		}

		private void cancel() {
			qline = null;
			ui.grabkeys(null);
		}

		protected void done(String line) {
			if (line.length() > 0)
				chan.send(line);
			cancel();
		}

		public boolean key(char c, int code, int mod) {
			if (c == 27) {
				cancel();
			} else {
				return (super.key(c, code, mod));
			}
			return (true);
		}
	}

	public boolean keydown(KeyEvent ev) {
		if (HConfig.cl_use_new_chat) return (super.keydown(ev));
		
		boolean M = (ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
		if (qline != null) {
			if (M && (ev.getKeyCode() == KeyEvent.VK_UP)) {
				Channel prev = this.sel;
				while (chansel.up()) {
					if (this.sel instanceof EntryChannel)
						break;
				}
				if (!(this.sel instanceof EntryChannel)) {
					select(prev);
					return (true);
				}
				qline = new QuickLine((EntryChannel) sel);
				return (true);
			} else if (M && (ev.getKeyCode() == KeyEvent.VK_DOWN)) {
				Channel prev = this.sel;
				while (chansel.down()) {
					if (this.sel instanceof EntryChannel)
						break;
				}
				if (!(this.sel instanceof EntryChannel)) {
					select(prev);
					return (true);
				}
				qline = new QuickLine((EntryChannel) sel);
				return (true);
			}
			qline.key(ev);
			return (true);
		} else {
			if (M && (ev.getKeyCode() == KeyEvent.VK_UP)) {
				chansel.up();
				return (true);
			} else if (M && (ev.getKeyCode() == KeyEvent.VK_DOWN)) {
				chansel.down();
				return (true);
			}
			return (super.keydown(ev));
		}
	}

	public void toggle() {
		if (!expanded) {
			expand();
			parent.setfocus(this);
		} else {
			if (hasfocus) {
				if (sz.y == 100)
					resize(new Coord(sz.x, 300));
				else
					contract();
			} else {
				parent.setfocus(this);
			}
		}
	}

	public boolean type(char key, KeyEvent ev) {
		if (HConfig.cl_use_new_chat) return (super.type(key, ev));
		
		if (qline != null) {
			qline.key(ev);
			return (true);
		} else {
			return (super.type(key, ev));
		}
	}

	public boolean globtype(char key, KeyEvent ev) {
		if (HConfig.cl_use_new_chat) return (super.globtype(key, ev));
		
		if (key == 10) {
			if (!expanded && (sel instanceof EntryChannel)) {
				ui.grabkeys(this);
				qline = new QuickLine((EntryChannel) sel);
				return (true);
			}
		}
		return (super.globtype(key, ev));
	}
	
	public Widget getAreaChat() {
		for (Widget w = lchild; w != null; w = w.prev) {
			if (w instanceof MultiChat) {
				if (((MultiChat)w).name.equals("Area Chat")) return w;
			}
		}
		return null;
	}
	
	public Widget getVillageChat() {
		for (Widget w = lchild; w != null; w = w.prev) {
			if (w instanceof MultiChat) {
				if (((MultiChat)w).name.equals("Village")) return w;
			}
		}
		return null;
	}
	
	public Widget getPartyChat() {
		for (Widget w = lchild; w != null; w = w.prev) {
			if (w instanceof PartyChat) {
				if (((MultiChat)w).name.equals("Party")) return w;
			}
		}
		return null;
	}
	
	public Widget getPrivChat(int bid) {
		for (Widget w = lchild; w != null; w = w.prev) {
			if (w instanceof PrivChat) {
				if (((PrivChat)w).other == bid) return w;
			}
		}
		return null;
	}
}
