package org.apxeolog.salem;

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

}
