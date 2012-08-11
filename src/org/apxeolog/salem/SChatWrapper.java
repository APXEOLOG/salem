package org.apxeolog.salem;

import java.awt.Color;
import java.awt.font.TextAttribute;
import java.util.Hashtable;

import org.apxeolog.salem.config.ChatConfig;
import org.apxeolog.salem.config.ChatConfig.ChannelTypes;
import org.apxeolog.salem.config.ChatConfig.ChatModeInfo;
import org.apxeolog.salem.utils.STextProcessor;
import org.apxeolog.salem.utils.STextProcessor.NodeAttribute;
import org.apxeolog.salem.utils.STextProcessor.ProcessedText;

import haven.BuddyWnd;
import haven.Config;
import haven.GameUI;
import haven.BuddyWnd.Buddy;
import haven.ChatUI.EntryChannel;
import haven.ChatUI.MultiChat;
import haven.ChatUI.PartyChat;
import haven.ChatUI.PrivChat;
import haven.Widget;

public class SChatWrapper {
	public static class TextBackLink extends NodeAttribute {
		protected int widgetId;
		protected GameUI gameUI;

		public TextBackLink(int wid, GameUI gui) {
			widgetId = wid;
			gameUI = gui;
		}

		@Override
		public String getData() {
			return "";
		}

		@Override
		public Hashtable<TextAttribute, Object> getFontAttrs() {
			Hashtable<TextAttribute, Object> fontAttrs = new Hashtable<TextAttribute, Object>();
			return fontAttrs;
		}

		@Override
		public Color getColor() {
			return null;
		}

		@Override
		public void act(int button) {
			onHeaderAct(widgetId, gameUI);
		}
	}

	public static ChannelTypes getChatType(int wdgId, Widget gui) {
		if (gui != null) {
			Widget wdg = gui.ui.getWidget(wdgId);
			if (wdg instanceof MultiChat) {
				MultiChat mchat = (MultiChat) wdg;
				if (mchat.getName().equals("Area Chat")) {
					return ChannelTypes.AREA;
				} else if (mchat.getName().equals("Village")) {
					return ChannelTypes.VILLAGE;
				}
			} else if (wdg instanceof PrivChat) {
				return ChannelTypes.PM;
			} else if (wdg instanceof PartyChat) {
				return ChannelTypes.PARTY;
			}
		}
		return ChannelTypes.NONE;
	}

	public static String getOpponentName(int wdgId, Widget gui) {
		if (gui != null) {
			GameUI gameUI = gui.getparent(GameUI.class);
			Widget wdg = gui.ui.getWidget(wdgId);
			if (wdg instanceof PrivChat) {
				Buddy messager = gameUI.buddies.find(((PrivChat)wdg).getOpponent());
				return (messager == null) ? "???" : (messager.name);
			}
		}
		return null;
	}

	public static Color getColor(ChannelTypes type) {
		return ChatConfig.getModeInfo(type).getColor();
	}

	public static String getPrefix(ChannelTypes type) {
		return ChatConfig.getModeInfo(type).getShortTag();
	}

	public static ChatModeInfo getModeInfo(ChannelTypes type) {
		return ChatConfig.getModeInfo(type);
	}

	public static void onHeaderAct(int wdgId, GameUI gameUI) {
		if (gameUI != null) {
			gameUI.bdsChatB.showLine(wdgId);
		}
	}

	public static void sendMessage(int wdgId, String text, Widget wdg) {
		GameUI gameUI = wdg.getparent(GameUI.class);
		if (gameUI != null) {
			Widget chat = gameUI.ui.getWidget(wdgId);
			if (chat instanceof EntryChannel) {
				((EntryChannel) chat).send(text);
			}
		}
	}

	public static ProcessedText getLineHeader(ChannelTypes type, String nickname, Color nickColor) {
		StringBuilder builder = new StringBuilder();
		builder.append(STextProcessor.getColoredText(getPrefix(type) + "[", getColor(type)));
		builder.append(STextProcessor.getColoredText(nickname, nickColor));
		builder.append(STextProcessor.getColoredText("]: ", getColor(type)));
		return STextProcessor.fromString(builder.toString());
	}

	public static void wrapMultiChat(MultiChat wdg, Integer from, String str) {
		ChannelTypes messageType = ChannelTypes.NONE;
		if (wdg.getName().equals("Area Chat")) {
			messageType = ChannelTypes.AREA;
		} else if (wdg.getName().equals("Village")) {
			messageType = ChannelTypes.VILLAGE;
		}
		GameUI gui = wdg.getparent(GameUI.class);
		if (gui != null) {
			String name = Config.currentCharName;
			Color color = getColor(messageType);
			if (from != null) {
				// Other
				Buddy messager = gui.buddies.find(from.intValue());
				name = (messager == null) ? "???" : (messager.name);
				color = (messager == null) ? Color.WHITE : (BuddyWnd.gc[messager.group]);
			}
			Integer wdgId = wdg.ui.getWidgetId(wdg);
			ProcessedText header = getLineHeader(messageType, name, color);
			header.addAttribute(new TextBackLink(wdgId.intValue(), gui));

			ProcessedText text = STextProcessor.fromString(STextProcessor.getColoredText(str, getColor(messageType)));
			header.append(text);
			gui.bdsChatB.addPText(header, messageType);
		}
	}

	public static void wrapPartyChat(PartyChat wdg, Integer from, String str, Color memberColor) {
		ChannelTypes messageType = ChannelTypes.PARTY;
		GameUI gui = wdg.getparent(GameUI.class);
		if (gui != null) {
			String name = Config.currentCharName;
			Color color = memberColor;
			if (from != null) {
				// Other
				Buddy messager = gui.buddies.find(from.intValue());
				name = (messager == null) ? "???" : (messager.name);
			}
			Integer wdgId = wdg.ui.getWidgetId(wdg);
			ProcessedText header = getLineHeader(messageType, name, color);
			header.addAttribute(new TextBackLink(wdgId.intValue(), gui));

			ProcessedText text = STextProcessor.fromString(STextProcessor.getColoredText(str, getColor(messageType)));
			header.append(text);
			gui.bdsChatB.addPText(header, messageType);
		}
	}

	public static void wrapPMChat(PrivChat wdg, int from, String str, String mode) {
		ChannelTypes messageType = ChannelTypes.PM;
		GameUI gui = wdg.getparent(GameUI.class);
		if (gui != null) {
			StringBuilder builder = new StringBuilder();
			if (mode.equals("in")) {
				builder.append(STextProcessor.getColoredText(getPrefix(messageType) + "[", getColor(messageType)));
			} else {
				builder.append(STextProcessor.getColoredText(getPrefix(messageType) + " To [", getColor(messageType)));
			}
			Buddy messager = gui.buddies.find(from);
			String name = (messager == null) ? "???" : (messager.name);
			Color color = (messager == null) ? getColor(messageType) : (BuddyWnd.gc[messager.group]);
			if (mode.equals("in")) {
				builder.append(STextProcessor.getColoredText(name, color));
			} else {
				builder.append(STextProcessor.getColoredText(name, getColor(messageType)));
			}
			builder.append(STextProcessor.getColoredText("]: ", getColor(messageType)));

			Integer wdgId = wdg.ui.getWidgetId(wdg);
			ProcessedText header = STextProcessor.fromString(builder.toString());
			header.addAttribute(new TextBackLink(wdgId.intValue(), gui));

			ProcessedText text = STextProcessor.fromString(STextProcessor.getColoredText(str, getColor(messageType)));
			header.append(text);
			gui.bdsChatB.addPText(header, messageType);
		}
	}

	public static void wrapPMChatError(PrivChat wdg, Integer from, String str) {
		ChannelTypes messageType = ChannelTypes.PM;
		GameUI gui = wdg.getparent(GameUI.class);
		if (gui != null) {
			StringBuilder builder = new StringBuilder();
			builder.append(STextProcessor.getColoredText(getPrefix(messageType) + "[", Color.RED));
			Buddy messager = gui.buddies.find(from);
			String name = (messager == null) ? "???" : (messager.name);
			Color color = (messager == null) ? Color.WHITE : (BuddyWnd.gc[messager.group]);
			builder.append(STextProcessor.getColoredText(name, color));
			builder.append(STextProcessor.getColoredText("]: ", Color.RED));

			Integer wdgId = wdg.ui.getWidgetId(wdg);
			ProcessedText header = STextProcessor.fromString(builder.toString());
			header.addAttribute(new TextBackLink(wdgId.intValue(), gui));

			ProcessedText text = STextProcessor.fromString(STextProcessor.getColoredText(str, Color.RED));
			header.append(text);
			gui.bdsChatB.addPText(header, messageType);
		}
	}
}
