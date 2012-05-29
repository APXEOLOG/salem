package org.apxeolog.salem;

import java.awt.Color;
import java.util.HashMap;

import haven.Coord;
import haven.Widget;

public class SChatWindow extends SWindow {
	protected SChat chatWidget = null;
	protected SLineEdit lineEdit = null;
	
	public SChatWindow(Coord c, Coord sz, Widget parent) {
		super(c, sz, parent, "Chat");
		chatWidget = new SChat(Coord.z, sz, this);
		lineEdit = new SLineEdit(new Coord(0, chatWidget.sz.y + 5), new Coord(sz.x, 20), this, "", SChat.textFoundry, SChat.chatFontContext);
		pack();
	}

	public SChat getChat() {
		return chatWidget;
	}
	
	public void recieveMessage(String msg, Color mColor, String hName, Color hColor) {
		chatWidget.addMessage(msg, mColor, "[" + hName + "]: ", hColor);
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
//		if (sender == lineEdit) {
//			chatWidget.addMessage(lineEdit.getText());
//			lineEdit.clear();
//		} else super.wdgmsg(sender, msg, args);
	}
}
