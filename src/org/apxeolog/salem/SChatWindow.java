package org.apxeolog.salem;

import java.util.HashMap;

import org.apxeolog.salem.SChat.ChatType;

import haven.Coord;
import haven.Widget;

public class SChatWindow extends SWindow {
	protected HashMap<Widget, ChatType> currentChats = null;
	
	public void addChatType(Widget wdg, ChatType type) {
		currentChats.put(wdg, type);
	}
	
	public void reciveMessage(Widget wdg, String message, ChatType type) {
		//ChatType type = currentChats.get(wdg);
		if (type != null) {
			chatWidget.addMessage(type, message);
		}
	}
	
	protected SChat chatWidget = null;
	protected SLineEdit lineEdit = null;
	
	public SChatWindow(Coord c, Coord sz, Widget parent) {
		super(c, sz, parent, "Chat");
		currentChats = new HashMap<Widget, ChatType>();
		chatWidget = new SChat(Coord.z, sz, this);
		lineEdit = new SLineEdit(new Coord(0, chatWidget.sz.y + 5), new Coord(sz.x, 20), this, "", SChat.textFoundry, SChat.chatFontContext);
		pack();
	}

	public SChat getChat() {
		return chatWidget;
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
//		if (sender == lineEdit) {
//			chatWidget.addMessage(lineEdit.getText());
//			lineEdit.clear();
//		} else super.wdgmsg(sender, msg, args);
	}
}
