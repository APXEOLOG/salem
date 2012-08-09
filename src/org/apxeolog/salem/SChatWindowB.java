package org.apxeolog.salem;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apxeolog.salem.config.ChatConfig;
import org.apxeolog.salem.config.ChatConfig.ChannelTypes;
import org.apxeolog.salem.config.ChatConfig.ChatTabConfig;
import org.apxeolog.salem.widgets.STextArea;
import org.apxeolog.salem.widgets.SVerticalTextButton;

import haven.Coord;
import haven.Widget;

public class SChatWindowB extends SWindow {
	protected HashMap<ChatTabConfig, Pair<SVerticalTextButton, STextArea>> chatWidgets;

	public SChatWindowB(Coord c, Coord sz, Widget parent, String cap) {
		super(c, sz, parent, cap);
		chatWidgets = new HashMap<ChatConfig.ChatTabConfig, Pair<SVerticalTextButton,STextArea>>();
		// Load tabs
		int x = 0, y = 0;
		for (ChatTabConfig tab : ChatConfig.chatTabs) {
			SVerticalTextButton btn = new SVerticalTextButton(new Coord(0, y), Coord.z, this, tab.getName()) {
				@Override
				public void click() {
					ALS.alDebugPrint(rendered.text);
				}
			};
			y += btn.sz.y + 1; x = btn.sz.x;
			btn.c.x = -btn.sz.x - 4;
			STextArea area = new STextArea(Coord.z, Coord.z, this);
			area.hide();
			chatWidgets.put(tab, new Pair<SVerticalTextButton, STextArea>(btn, area));
		}
		// Resize
		windowBox.marginLeft = x;
		resize(windowBox.getContentSize());
		setResizable(true);
	}

	@Override
	public void resize(Coord newSize) {
		super.resize(newSize);
		for (Pair<SVerticalTextButton, STextArea> pair : chatWidgets.values()) {
			pair.getSecond().resize(windowBox.getContentSize());
		}
	}

	@Override
	public void resizeFinish() {
		for (Pair<SVerticalTextButton, STextArea> pair : chatWidgets.values()) {
			pair.getSecond().resizeFinish();
		}
	}

	public void addString(String str, ChannelTypes type) {
		for (Entry<ChatTabConfig, Pair<SVerticalTextButton, STextArea>> entry : chatWidgets.entrySet()) {
			if (entry.getKey().containsChannel(type)) {
				entry.getValue().getSecond().addString(str);
			}
		}
	}
}
