package org.apxeolog.salem;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import com.sun.org.apache.bcel.internal.classfile.PMGClass;

import haven.BuddyWnd.Buddy;
import haven.ChatUI;
import haven.ChatUI.EntryChannel;
import haven.ChatUI.PrivChat;
import haven.Coord;
import haven.GameUI;
import haven.Text;
import haven.Widget;

public class SChatWindow extends SWindow {
	public static final int CHAT_BUFFER_SIZE = 300;
	
	public static final int MODE_AREA = 0;
	public static final int MODE_VILLAGE = 1;
	public static final int MODE_PARTY = 2;
	public static final int MODE_LINKED = 3;
	
	public static final Color[] MODE_COLORS = new Color[4];
	public static final Text[] MODE_HEADERS = new Text[4];
	
	static {
		MODE_COLORS[MODE_AREA] = Color.WHITE;
		MODE_COLORS[MODE_VILLAGE] = Color.GREEN;
		MODE_COLORS[MODE_PARTY] = Color.CYAN;
		MODE_COLORS[MODE_LINKED] = Color.PINK;
		
		MODE_HEADERS[MODE_AREA] = SChat.textFoundry.render("[Area]: ", MODE_COLORS[MODE_AREA]);
		MODE_HEADERS[MODE_VILLAGE] = SChat.textFoundry.render("[Village]: ", MODE_COLORS[MODE_VILLAGE]);
		MODE_HEADERS[MODE_PARTY] = SChat.textFoundry.render("[Party]: ", MODE_COLORS[MODE_PARTY]);
		MODE_HEADERS[MODE_LINKED] = SChat.textFoundry.render("[PM]: ", MODE_COLORS[MODE_LINKED]);
	}
	
	
	protected SChat chatWidget = null;
	protected SLineEdit lineEdit = null;
	
	protected int currentWriteMode = MODE_AREA;
	protected Object linkedObject = null;
	
	public SChatWindow(Coord c, Coord sz, Widget parent) {
		super(c, sz, parent, "Chat");
		chatWidget = new SChat(Coord.z, windowBox.getContentSize(), this);
		lineEdit = new SLineEdit(new Coord(0, chatWidget.sz.y + 5), new Coord(sz.x, 20), this, "", SChat.textFoundry, SChat.chatFontContext);
		lineEdit.hide();
		pack();
	}
	
	@Override
	public boolean globtype(char key, KeyEvent ev) {
		if (!HConfig.cl_use_new_chat) return super.globtype(key, ev);
		
		boolean ctrl = ev.isControlDown();
		boolean alt = ev.isAltDown() || ev.isMetaDown();
		boolean shift = ev.isShiftDown();

		if (ev.getKeyCode() == KeyEvent.VK_ENTER && !ctrl && !alt && !shift) {
			// Area
			if (getparent(GameUI.class).chat.getAreaChat() != null) {
				currentWriteMode = MODE_AREA;
				showLine();
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_ENTER && !ctrl && !alt && shift) {
			// Village
			if (getparent(GameUI.class).chat.getVillageChat() != null) {
				currentWriteMode = MODE_VILLAGE;
				showLine();
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_ENTER && ctrl && !alt && !shift) {
			// Party
			if (getparent(GameUI.class).chat.getPartyChat() != null) {
				currentWriteMode = MODE_PARTY;
				showLine();
			}
		} else return super.globtype(key, ev);
		
		return true;
	}

	public SChat getChat() {
		return chatWidget;
	}
	
	public static class MessageBuf {
		public String bMsg;
		public Color bMColor;
		public String bHName;
		public Color bHColor;
		public WeakReference<Widget> bSender;
		
		public MessageBuf(String msg, Color mColor, String hName, Color hColor, WeakReference<Widget> sender) {
			bMsg = msg;
			bMColor = mColor;
			bHName = hName;
			bHColor = hColor;
			bSender = sender;
		}
	}
	
	public ArrayList<MessageBuf> chatCache = new ArrayList<SChatWindow.MessageBuf>(CHAT_BUFFER_SIZE);
	
	public void recieveMessage(String msg, Color mColor, String hName, Color hColor, Widget sender) {
		WeakReference<Widget> ref = new WeakReference<Widget>(sender);
		
		// Cache message
		MessageBuf cachedMsg = new MessageBuf(msg, mColor, hName, hColor, ref);
		chatCache.add(cachedMsg);
		if (chatCache.size() > CHAT_BUFFER_SIZE) chatCache.remove(0);
		
		chatWidget.addMessage(cachedMsg);
	}
	
	@Override
	public void resizeFinish() {
		chatWidget.clear();
		chatWidget.resize(windowBox.getContentSize());
		
		for (int i = 0; i < chatCache.size(); i++) {
			chatWidget.addMessage(chatCache.get(i));
		}
	}
	
	public void showLine(Object... objs) {
		lineEdit.show();
		
		Text header = MODE_HEADERS[currentWriteMode];
		if (objs.length > 0 && objs[0] instanceof Text) header = (Text)objs[0];
		
		lineEdit.setupLine(header, MODE_COLORS[currentWriteMode]);
		setfocus(lineEdit);
		ui.grabkeys(lineEdit);
	}
	
	public void setLinkedMode(String pureName, Text header, WeakReference<Widget> wdgRef, boolean pm) {
		if (!pm && wdgRef.get() != null) {
			if (getparent(GameUI.class).chat.getAreaChat() == wdgRef.get()) {
				currentWriteMode = MODE_AREA;
				showLine();
			} else if (getparent(GameUI.class).chat.getVillageChat() == wdgRef.get()) {
				currentWriteMode = MODE_VILLAGE;
				showLine();
			} else if (getparent(GameUI.class).chat.getPartyChat() == wdgRef.get()) {
				currentWriteMode = MODE_PARTY;
				showLine();
			} else {
				// Private chat
				currentWriteMode = MODE_LINKED;
				linkedObject = wdgRef;
				showLine(header);
			}
		} else if (pm) {
			if (pureName.equals("???")) return;
			
			Buddy buddy = getparent(GameUI.class).buddies.find(pureName);
			if (buddy != null) {
				Widget chat = getparent(GameUI.class).chat.getPrivChat(buddy.id);
				if (chat != null) {
					currentWriteMode = MODE_LINKED;
					linkedObject = new WeakReference<Widget>(chat);
					showLine(header);
				} else {
					buddy.chat();
					currentWriteMode = MODE_LINKED;
					linkedObject = new Integer(buddy.id);
					showLine(header);
				}
			}
		}
	}
	
	/* GOD I HATE THIS UGLY CODE */
	protected boolean waitingForChat = false;
	
	public boolean isWaitingForChat() {
		return waitingForChat;
	}
	
	public void setWaitingForChat() {
		waitingForChat = true;
	}
	
	public void receiveChat(WeakReference<Widget> wdgRef) {
		waitingForChat = false;
		currentWriteMode = MODE_LINKED;
		linkedObject = wdgRef;
		showLine(SChat.textFoundry.render(String.format("[%s]: ", ((PrivChat)(wdgRef.get())).name()), MODE_COLORS[MODE_LINKED]));
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (sender == lineEdit) {
			String text = lineEdit.getText();
			
			if (!text.equals("")) {
				
				switch (currentWriteMode) {
				case MODE_AREA:
					((ChatUI.EntryChannel) getparent(GameUI.class).chat.getAreaChat()).send(text);
					break;
				case MODE_VILLAGE:
					((ChatUI.EntryChannel) getparent(GameUI.class).chat.getVillageChat()).send(text);
					break;
				case MODE_PARTY:
					((ChatUI.EntryChannel) getparent(GameUI.class).chat.getPartyChat()).send(text);
					break;
				case MODE_LINKED:
					if (linkedObject instanceof WeakReference<?>) {
						Widget reffered = ((WeakReference<Widget>)linkedObject).get();
						if (reffered instanceof EntryChannel) {
							((EntryChannel)reffered).send(text);
						}
					} else if (linkedObject instanceof Integer) {
						Widget chat = getparent(GameUI.class).chat.getPrivChat((Integer)linkedObject);
						if (chat != null) {
							((EntryChannel)chat).send(text);
						}
					}
					break;
				default:
					break;
				}
			}
			lineEdit.clear();
			parent.setfocus(this);
			ui.grabkeys(null);
			lineEdit.hide();
		} else super.wdgmsg(sender, msg, args);
	}
}
