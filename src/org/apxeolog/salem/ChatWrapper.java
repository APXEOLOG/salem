package org.apxeolog.salem;

import haven.BuddyWnd;
import haven.Coord;
import haven.GameUI;
import haven.Widget;
import haven.WidgetFactory;

import org.apxeolog.salem.SChat.AreaChatType;
import org.apxeolog.salem.SChat.ChatType;
import org.apxeolog.salem.SChat.PMChatType;
import org.apxeolog.salem.SChat.PartyChatType;
import org.apxeolog.salem.SChat.UndefinedChatType;
import org.apxeolog.salem.SChat.VillageChatType;

public class ChatWrapper extends Widget {
	public ChatWrapper(Coord c, Coord sz, Widget parent) {
		super(c, sz, parent);
	}

//	static {
//		addtype("mchat", new WidgetFactory() {
//			public Widget create(Coord c, Widget parent, Object[] args) {
//				String name = (String) args[0];
//				boolean notify = ((Integer) args[1]) != 0;
//				return (new MultiChatWrap(parent, name, notify));
//			}
//		});
//		addtype("pchat", new WidgetFactory() {
//			public Widget create(Coord c, Widget parent, Object[] args) {
//				return (new PartyChatWrap(parent));
//			}
//		});
//		addtype("pmchat", new WidgetFactory() {
//			public Widget create(Coord c, Widget parent, Object[] args) {
//				int other = (Integer) args[0];
//				return (new PrivChatWrap(parent, other));
//			}
//		});
//	}
	
	public static class ChatWrap extends Widget {
		protected ChatType chatType;
		
		public ChatWrap(Widget parent) {
			super(Coord.z, Coord.z, parent);
		}
		
		public String name(int bid) {
			BuddyWnd.Buddy b = getparent(GameUI.class).buddies.find(bid);
			if (b == null)
				return "???";
			else
				return b.name;
		}
	}
	
	public static class MultiChatWrap extends ChatWrap {
		public MultiChatWrap(Widget parent, String name, boolean notify) {
			super(parent);
			if (name.equals("Area Chat")) {
				chatType = new AreaChatType();
			} else if (name.equals("Village")) {
				chatType = new VillageChatType();
			} else {
				chatType = new UndefinedChatType();
			}
		}
		
		public void uimsg(String msg, Object... args) {
			if (msg == "msg") {
				int from = (Integer) args[0];
				String line = (String) args[1];
				if (from != 0) line = String.format("%s: %s", name(from), line);
				getparent(GameUI.class).bdsChat.reciveMessage(this, line, chatType);
			}
		}
	}
	
	public static class PartyChatWrap extends ChatWrap {
		public PartyChatWrap(Widget parent) {
			super(parent);
			chatType = new PartyChatType();
		}
		
		public void uimsg(String msg, Object... args) {
			if (msg == "msg") {
				int from = (Integer) args[0];
				//int gobid = (Integer) args[1];
				String line = (String) args[2];
				
				if (from != 0) line = String.format("%s: %s", name(from), line);
				getparent(GameUI.class).bdsChat.reciveMessage(this, line, chatType);
			}
		}
	}
	
	public static class PrivChatWrap extends ChatWrap {
		public PrivChatWrap(Widget parent, int other) {
			super(parent);
			chatType = new PMChatType(name(other));
		}
		
		public void uimsg(String msg, Object... args) {
			if (msg == "msg") {
				//String t = (String) args[0];
				String line = (String) args[1];
				getparent(GameUI.class).bdsChat.reciveMessage(this, line, chatType);
			}
		}
	}
}
