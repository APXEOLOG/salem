package org.apxeolog.salem;

import java.awt.Color;
import java.awt.font.TextAttribute;
import java.util.Hashtable;

import org.apxeolog.salem.config.ChatConfig;
import org.apxeolog.salem.config.ChatConfig.ChannelTypes;
import org.apxeolog.salem.config.ChatConfig.ChatModeInfo;
import org.apxeolog.salem.config.XConfig;
import org.apxeolog.salem.irc.IRCProvider;
import org.apxeolog.salem.irc.RegisteredListener;
import org.apxeolog.salem.utils.STextProcessor;
import org.apxeolog.salem.utils.STextProcessor.NodeAttribute;
import org.apxeolog.salem.utils.STextProcessor.ProcessedText;
import org.apxeolog.salem.utils.STextProcessor.TextColor;

import f00f.net.irc.martyr.commands.RawCommand;
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
		protected String name;

		public TextBackLink(int wid, String nm) {
			widgetId = wid;
			name = nm;
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
		public void act(int button, boolean ctrl, boolean shft) {
			onHeaderAct(widgetId, button, ctrl, shft, name);
		}
	}

	protected static GameUI gameUI = null;

	public static void bindGameUI(GameUI gui) {
		gameUI = gui;
		if (XConfig.mp_irc_autoconnect) startIRCProvider();
	}

	public static void unbindGameUI() {
		gameUI = null;
		if (ircProvider != null) ircProvider.disconnect();
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

	public static String getOpponentName(int wdgId) {
		if (gameUI != null) {
			Widget wdg = gameUI.ui.getWidget(wdgId);
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

	public static void onHeaderAct(int wdgId,int button, boolean ctrl, boolean shft, String name) {
		if (gameUI != null) {
			if (!ctrl)
				gameUI.bdsChatB.showLine(wdgId);
			else {
				Buddy bud = gameUI.buddies.find(name);
				if (bud != null) bud.chat();
			}
		}
	}

	protected static boolean waitingForChat = false;

	public static void waitChat() {
		waitingForChat = true;
	}

	public static void tryStartChat(int bid) {
		if (gameUI != null) {
			int privChat = gameUI.chat.getPrivChat(bid);
			if (privChat != -1) {
				gameUI.bdsChatB.showLine(privChat, ChannelTypes.PM);
			} else {
				waitChat();
			}
		}
	}

	public static boolean isWaitingForChat() {
		if (waitingForChat) {
			waitingForChat = false;
			return true;
		}
		return waitingForChat;
	}

	protected static int lastPMWdgId = -1;

	public static int getLastPMWidget() {
		return lastPMWdgId;
	}

	protected static IRCProvider ircProvider = null;

	public static void startIRCProvider() {
		if (ircProvider == null) {
			ircProvider = IRCProvider.ircConnect(new RegisteredListener() {
				@Override
				public void onRegister() {
					ircLogToChat("You was registered on IRC!");
					joinIRCChannels();
				}
			});
		}
	}

	public static void joinIRCChannels() {
		if (ircProvider == null || !ircProvider.isReady()) return;
		for (Pair<String, String> pair : ChatConfig.getIRCChannels()) {
			ircLogToChat("Joining channel " + pair.getFirst());
			ircProvider.joinChannel(pair.getFirst(), pair.getSecond());
		}
	}

	public static void ircLogToChat(String text) {
		ircMessageRecieved(ChatConfig.getFirstIRCChannel(), text);
	}

	public static void ircMessageRecieved(String channel, String msg) {
		if (gameUI != null) {
			gameUI.bdsChatB.addString(msg, ChannelTypes.IRC, channel);
		}
	}

	public static void ircMessageRecieved(String channel, String msg, Color clr) {
		if (gameUI != null) {
			ProcessedText ptext = STextProcessor.fromString(channel);
			ptext.addAttribute(new TextColor(clr));
			gameUI.bdsChatB.addPText(ptext, ChannelTypes.IRC, channel);
		}
	}

	public static void sendIRCMessage(String text, String channel) {
		if (ircProvider != null && ircProvider.isReady()) {
			if (text.startsWith("/")) {
				// Command
				String command = null, params = "";
				if (text.contains(" ")) {
					command = text.substring(1, text.indexOf(' '));
					params = text.substring(text.indexOf(' '));
				} else {
					command = text.substring(1);
				}
				if (command.equals("ml")) {
					ircMessageRecieved(channel, ircProvider.getMembers(channel), Color.GRAY);
				} else {
					RawCommand raw = new RawCommand(command, params);
					ircProvider.sendCommand(channel, raw);
				}
			} else {
				ircProvider.say(channel, text);
				ircMessageRecieved(channel, ircProvider.getNickName() + ": " + text);
			}
		}
	}



	public static void sendMessage(int wdgId, String text) {
		if (gameUI != null) {
			Widget chat = gameUI.ui.getWidget(wdgId);
			if (chat instanceof PrivChat) {
				lastPMWdgId = wdgId;
			}
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
		if (gameUI != null) {
			String name = Config.currentCharName;
			Color color = getColor(messageType);
			if (from != null) {
				// Other
				Buddy messager = gameUI.buddies.find(from.intValue());
				name = (messager == null) ? "???" : (messager.name);
				color = (messager == null) ? Color.WHITE : (BuddyWnd.gc[messager.group]);
			}
			Integer wdgId = wdg.ui.getWidgetId(wdg);
			ProcessedText header = getLineHeader(messageType, name, color);
			header.addAttribute(new TextBackLink(wdgId.intValue(), name));

			ProcessedText text = STextProcessor.fromString(str);
			text.addAttribute(new TextColor(getColor(messageType)));
			header.append(text);
			gameUI.bdsChatB.addPText(header, messageType);
		}
	}

	public static void wrapPartyChat(PartyChat wdg, Integer from, String str, Color memberColor) {
		ChannelTypes messageType = ChannelTypes.PARTY;
		if (gameUI != null) {
			String name = Config.currentCharName;
			Color color = memberColor;
			if (from != null) {
				// Other
				Buddy messager = gameUI.buddies.find(from.intValue());
				name = (messager == null) ? "???" : (messager.name);
			}
			Integer wdgId = wdg.ui.getWidgetId(wdg);
			ProcessedText header = getLineHeader(messageType, name, color);
			header.addAttribute(new TextBackLink(wdgId.intValue(), name));

			ProcessedText text = STextProcessor.fromString(str);
			text.addAttribute(new TextColor(getColor(messageType)));
			header.append(text);
			gameUI.bdsChatB.addPText(header, messageType);
		}
	}

	public static void wrapPMChat(PrivChat wdg, int from, String str, String mode) {
		ChannelTypes messageType = ChannelTypes.PM;
		if (gameUI != null) {
			StringBuilder builder = new StringBuilder();
			if (mode.equals("in")) {
				builder.append(STextProcessor.getColoredText(getPrefix(messageType) + "[", getColor(messageType)));
			} else {
				builder.append(STextProcessor.getColoredText(getPrefix(messageType) + " To [", getColor(messageType)));
			}
			Buddy messager = gameUI.buddies.find(from);
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
			header.addAttribute(new TextBackLink(wdgId.intValue(), name));

			ProcessedText text = STextProcessor.fromString(str);
			text.addAttribute(new TextColor(getColor(messageType)));
			header.append(text);
			gameUI.bdsChatB.addPText(header, messageType);
		}
	}

	public static void wrapPMChatError(PrivChat wdg, Integer from, String str) {
		ChannelTypes messageType = ChannelTypes.PM;
		if (gameUI != null) {
			Buddy messager = gameUI.buddies.find(from);
			String name = (messager == null) ? "???" : (messager.name);
			Color color = (messager == null) ? Color.WHITE : (BuddyWnd.gc[messager.group]);

			Integer wdgId = wdg.ui.getWidgetId(wdg);
			ProcessedText header = getLineHeader(messageType, name, color);
			header.addAttribute(new TextBackLink(wdgId.intValue(), name));

			ProcessedText text = STextProcessor.fromString(str);
			text.addAttribute(new TextColor(Color.RED));
			header.append(text);
			gameUI.bdsChatB.addPText(header, messageType);
		}
	}
}
