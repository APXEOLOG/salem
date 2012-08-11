package org.apxeolog.salem;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apxeolog.salem.config.ChatConfig;
import org.apxeolog.salem.config.ChatConfig.ChatModeInfo;
import org.apxeolog.salem.config.XConfig;
import org.apxeolog.salem.config.ChatConfig.ChannelTypes;
import org.apxeolog.salem.config.ChatConfig.ChatTabConfig;
import org.apxeolog.salem.utils.STextProcessor;
import org.apxeolog.salem.utils.STextProcessor.ProcessedText;
import org.apxeolog.salem.widgets.STextArea;
import org.apxeolog.salem.widgets.SVerticalTextButton;

import haven.Coord;
import haven.GameUI;
import haven.Text;
import haven.Widget;

public class SChatWindowB extends SWindow {
	protected HashMap<ChatTabConfig, Pair<SVerticalTextButton, STextArea>> chatWidgets;
	protected SLineEdit lineEdit = null;

	public SChatWindowB(Coord c, Coord sz, Widget parent, String cap) {
		super(c, sz, parent, cap);
		chatWidgets = new HashMap<ChatConfig.ChatTabConfig, Pair<SVerticalTextButton,STextArea>>();
		// Load tabs
		int x = 0, y = 0;
		for (ChatTabConfig tab : ChatConfig.chatTabs) {
			SVerticalTextButton btn = new SVerticalTextButton(new Coord(0, y), Coord.z, this, tab.getName()) {
				@Override
				public void click() {
					wdgmsg("show_area");
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
		lineEdit = new SLineEdit(new Coord(0, sz.y - 40), new Coord(sz.x, 20), this, "", STextProcessor.FOUNDRY);
		lineEdit.hide();
		windowBox.marginBottom += 20;

		resize(windowBox.getContentSize());
	}

	public void showLine(int wdgId) {
		showLine(wdgId, SChatWrapper.getChatType(wdgId, this));
	}

	public void showLine(int wdgId, ChannelTypes type) {
		ChatModeInfo info = SChatWrapper.getModeInfo(type);
		if (type == ChannelTypes.PM) {
			String name = SChatWrapper.getOpponentName(wdgId, this);
			if (name != null) {
				showLine(wdgId, type, STextProcessor.FOUNDRY.render("[" + name + "]: ", info.getColor()));
			} else showLine(wdgId, type, info.getLongText());
		} else {
			showLine(wdgId, type, info.getLongText());
		}
	}

	public void showLine(int wdgId, ChannelTypes type, Text header) {
		lineEdit.show(wdgId);
		lineEdit.setupLine(header, SChatWrapper.getColor(type));
		setfocus(lineEdit);
		ui.grabkeys(lineEdit);
	}

	@Override
	public boolean globtype(char key, KeyEvent ev) {
		if (!XConfig.cl_use_new_chat) return false;

		boolean ctrl = ev.isControlDown();
		boolean alt = ev.isAltDown() || ev.isMetaDown();
		boolean shift = ev.isShiftDown();

		if (ev.getKeyCode() == KeyEvent.VK_ENTER && !ctrl && !alt && !shift) {
			// Area
			int wdgId = getparent(GameUI.class).chat.getAreaChat();
			if (wdgId != -1) {
				showLine(wdgId, ChannelTypes.AREA);
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_ENTER && !ctrl && !alt && shift) {
			// Village
			int wdgId = getparent(GameUI.class).chat.getVillageChat();
			if (wdgId != -1) {
				showLine(wdgId, ChannelTypes.VILLAGE);
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_ENTER && ctrl && !alt && !shift) {
			// Party
			int wdgId = getparent(GameUI.class).chat.getPartyChat();
			if (wdgId != -1) {
				showLine(wdgId, ChannelTypes.PARTY);
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_ENTER && !ctrl && alt && !shift) {
			//showLine();
		} else return super.globtype(key, ev);


		return true;
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (msg.equals("show_area")) {
			if (chatWidgets != null) {
				for (Pair<SVerticalTextButton, STextArea> pair : chatWidgets.values()) {
					if (pair.getFirst() == sender) {
						pair.getSecond().show();
					} else {
						pair.getSecond().hide();
					}
				}
			}
		} else if (msg.equals("sle_activate")) {
			if (args.length > 1) {
				String text = (String) args[0];
				int wdgId = (Integer) args[1];

				lineEdit.clear();
				parent.setfocus(this);
				ui.grabkeys(null);
				lineEdit.hide();

				if (!text.equals(""))
					SChatWrapper.sendMessage(wdgId, text, this);
			}
		} else super.wdgmsg(sender, msg, args);
	}

	@Override
	public void resize(Coord newSize) {
		super.resize(newSize);
		if (chatWidgets != null) {
			for (Pair<SVerticalTextButton, STextArea> pair : chatWidgets.values()) {
				pair.getSecond().resize(windowBox.getContentSize());
			}
		}
		if (lineEdit != null) {
			lineEdit.c = new Coord(windowBox.getBorderPosition().x, sz.y - 30);
			lineEdit.sz = new Coord(windowBox.getContentSize().x, 20);
		}
	}

	@Override
	public void resizeFinish() {
		for (Pair<SVerticalTextButton, STextArea> pair : chatWidgets.values()) {
			pair.getSecond().resizeFinish();
		}
	}

	public void addPText(ProcessedText str, ChannelTypes type) {
		for (Entry<ChatTabConfig, Pair<SVerticalTextButton, STextArea>> entry : chatWidgets.entrySet()) {
			if (entry.getKey().containsChannel(type)) {
				entry.getValue().getSecond().addPText(str);
			}
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
