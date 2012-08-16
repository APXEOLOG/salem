package org.apxeolog.salem;

import java.util.Locale;

import org.apxeolog.salem.widgets.STextArea;

import haven.Config;
import haven.Coord;
import haven.Label;
import haven.TextEntry;
import haven.Widget;

public class SIRCWidget extends Widget {
	// IRC Chat
	protected STextArea ircTextArea = null;
	// IRC Connection interface
	protected Widget ircSetupPanel = null;
	protected TextEntry ircSetupServer = null;
	protected TextEntry ircSetupChannel = null;
	protected TextEntry ircSetupNickname = null;
	protected TextEntry ircSetupPassword = null;

	public String getDefaultLocaleChannel() {
		String local = Locale.getDefault().getLanguage();
		if (local.equals("ru")) {
			return "#salem";
		} else return "#salem";
	}

	public SIRCWidget(Coord c, Coord sz, Widget parent) {
		super(c, sz, parent);
		ircSetupPanel = new Widget(Coord.z, Coord.z, this);
		new Label(Coord.z, ircSetupPanel, "Enter IRC settings here. All changes will be saved.");
		new Label(new Coord(0, 16), ircSetupPanel, "IRC Server:");
		ircSetupServer = new TextEntry(new Coord(0, 32), new Coord(100, 16), ircSetupPanel, "irc.synirc.net");
		new Label(new Coord(0, 48), ircSetupPanel, "Default channel:");
		ircSetupChannel = new TextEntry(new Coord(0, 64), new Coord(100, 16), ircSetupPanel, getDefaultLocaleChannel());
		new Label(new Coord(0, 80), ircSetupPanel, "Username:");
		ircSetupNickname = new TextEntry(new Coord(0, 96), new Coord(100, 16), ircSetupPanel, Config.currentCharName);
		new Label(new Coord(110, 80), ircSetupPanel, "Password:");
		ircSetupPassword = new TextEntry(new Coord(110, 96), new Coord(100, 16), ircSetupPanel, "");
	}

}
