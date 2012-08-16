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
	protected Pair<SVerticalTextButton, STextArea> currentTab = null;
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
					wdgmsg("show_area", this.rendered.text);
				}
			};
			y += btn.sz.y + 1; x = btn.sz.x;
			btn.c.x = -btn.sz.x - 4;
			btn.setData(tab);
			STextArea area = new STextArea(Coord.z, Coord.z, this);
			area.hide();

			chatWidgets.put(tab, new Pair<SVerticalTextButton, STextArea>(btn, area));
		}
		// Resize
		windowBox.marginLeft = x;
		lineEdit = new SLineEdit(Coord.z, Coord.z, parent, "", STextProcessor.FOUNDRY);
		lineEdit.hide();
		windowBox.marginBottom += 30;

		resize(windowBox.getContentSize());

		showTab(ChatConfig.getDefaultChannel());
	}

	public void showTab(String tabName) {
		if (chatWidgets == null) return;
		for (Entry<ChatTabConfig, Pair<SVerticalTextButton, STextArea>> entry : chatWidgets.entrySet()) {
			if (entry.getKey().getName().equals(tabName)) {
				currentTab = entry.getValue();
				currentTab.getSecond().show();
				currentTab.getFirst().select();
			} else {
				entry.getValue().getFirst().unselect();
				entry.getValue().getSecond().hide();
			}
		}
	}

	public void showLine(int wdgId) {
		showLine(wdgId, SChatWrapper.getChatType(wdgId, this));
	}

	public void showLine(int wdgId, ChannelTypes type) {
		ChatModeInfo info = SChatWrapper.getModeInfo(type);
		if (type == ChannelTypes.PM) {
			String name = SChatWrapper.getOpponentName(wdgId);
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
			showLine(-255, ChannelTypes.IRC);
			/*int wdgId = SChatWrapper.getLastPMWidget();
			if (wdgId != -1) {
				showLine(wdgId, ChannelTypes.PM);
			}*/
		} else return super.globtype(key, ev);
		return true;
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (msg.equals("show_area")) {
			showTab((String) args[0]);
		} else if (msg.equals("sle_activate")) {
			if (args.length > 1) {
				String text = (String) args[0];
				int wdgId = (Integer) args[1];

				lineEdit.clear();
				parent.setfocus(this);
				ui.grabkeys(null);
				lineEdit.hide();

				if (!text.equals("")) {
					if (wdgId == -255) {
						// IRC
						SChatWrapper.sendIRCMessage(text, ((ChatTabConfig)currentTab.getFirst().getData()).getIRCChannel());
					} else SChatWrapper.sendMessage(wdgId, text);
				}
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
		updateLine();
	}

	public void updateLine() {
		if (lineEdit != null) {
			lineEdit.c = new Coord(c.add(windowBox.getContentPosition()).x, c.add(windowBox.getContentPosition()).add(windowBox.getContentSize()).y + 5);
			lineEdit.sz = new Coord(windowBox.getContentSize().x, 20);
		}
	}

	@Override
	public void drag() {
		updateLine();
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
				entry.getValue().getFirst().setnotify();
			}
		}
	}

	public void addString(String str, ChannelTypes type, Object...objects) {
		for (Entry<ChatTabConfig, Pair<SVerticalTextButton, STextArea>> entry : chatWidgets.entrySet()) {
			if (entry.getKey().containsChannel(type)) {
				if (type == ChannelTypes.IRC && objects.length > 0) {
					String channel = (String) objects[0];
					ALS.alDebugPrint("got channel msg", channel);
					if (entry.getKey().getIRCChannel().equals(channel)) {
						entry.getValue().getSecond().addString(str);
						entry.getValue().getFirst().setnotify();
					}
				} else {
					entry.getValue().getSecond().addString(str);
					entry.getValue().getFirst().setnotify();
				}
			}
		}
	}
}
