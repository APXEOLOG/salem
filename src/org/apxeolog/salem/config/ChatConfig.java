package org.apxeolog.salem.config;

import haven.Text;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.apxeolog.salem.Pair;
import org.apxeolog.salem.utils.STextProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ChatConfig implements IConfigExport {
	static {
		XMLConfigProvider.registerConfig("chat", new IConfigFactory() {
			@Override
			public IConfigExport create(Element sectionRoot) {
				if (sectionRoot == null) return new ChatConfig();
				else return new ChatConfig(sectionRoot);
			}
		});
	}

	public enum ChannelTypes { AREA, PARTY, VILLAGE, PM, SYSTEM, IRC, NONE };

	public static class ChatTabConfig {
		protected String tabName;
		protected ArrayList<ChannelTypes> tabChannels;
		protected String ircChannel = null;
		protected String ircKey = null;
		protected Boolean isDefault = null;

		public ChatTabConfig(String name) {
			tabChannels = new ArrayList<ChatConfig.ChannelTypes>();
			tabName = name;
		}

		public void setDefault(boolean def) {
			isDefault = def;
		}

		public boolean isDefault() {
			return isDefault != null ? isDefault : false;
		}

		public String getName() {
			return tabName;
		}

		public void addChannel(ChannelTypes type) {
			if (!tabChannels.contains(type)) tabChannels.add(type);
		}

		public boolean containsChannel(ChannelTypes type) {
			return tabChannels.contains(type);
		}

		public void setIRCChannel(String chan) {
			ircChannel = chan;
		}

		public String getIRCChannel() {
			return ircChannel;
		}

		public void setIRCKey(String key) {
			ircKey = key;
		}

		public String getIRCKey() {
			return ircKey;
		}

		public void addElement(Element root) {
			root.setAttribute("name", tabName);
			StringBuilder sb = new StringBuilder();
			for (ChannelTypes type : tabChannels)  {
				sb.append(type.toString());
				sb.append(";");
			}
			root.setAttribute("channels", sb.substring(0, sb.length() - 1));
			if (ircChannel != null) root.setAttribute("irc-channel", ircChannel);
			if (ircKey != null) root.setAttribute("irc-key", ircKey);
			if (isDefault != null) root.setAttribute("default", String.valueOf(isDefault.booleanValue()));
		}
	}

	public static class ChatModeInfo {
		private ChannelTypes modeType;
		private String shortTag;
		private String longTag;
		private Color modeColor;

		public ChatModeInfo(ChannelTypes type) {
			modeType = type;
		}

		public Color getColor() {
			return modeColor;
		}

		public String getShortTag() {
			return shortTag;
		}

		public String getLongTag() {
			return longTag;
		}

		protected Text rendered = null;

		public Text getLongText() {
			if (rendered == null) rendered = STextProcessor.FOUNDRY.render(getLongTag(), getColor());
			return rendered;
		}

		public void parse(Element modeElement) {
			if (modeElement.hasAttribute("short-tag")) {
				shortTag = modeElement.getAttribute("short-tag");
			}
			if (modeElement.hasAttribute("long-tag")) {
				longTag = modeElement.getAttribute("long-tag");
			}
			if (modeElement.hasAttribute("color")) {
				modeColor = Color.decode(modeElement.getAttribute("color"));
			}
		}

		public void addElement(Element rootElement, Document document) {
			Element modeElement = document.createElement("mode");
			modeElement.setAttribute("short-tag", shortTag);
			modeElement.setAttribute("long-tag", longTag);
			modeElement.setAttribute("color", "0x" + Integer.toHexString(modeColor.getRGB()).substring(2).toUpperCase());
			modeElement.setAttribute("type", modeType.toString());
			rootElement.appendChild(modeElement);
		}
	}

	@Override
	public void addElement(Element rootElement, Document document) {
		rootElement.setAttribute("font-name", CHAT_FONT.getName());
		rootElement.setAttribute("font-style-bold", String.valueOf(CHAT_FONT.isBold()));
		rootElement.setAttribute("font-size", String.valueOf(CHAT_FONT.getSize()));
		for (ChatTabConfig tab : chatTabs) {
			Element bufElem = document.createElement("tab");
			tab.addElement(bufElem);
			rootElement.appendChild(bufElem);
		}
		for (ChatModeInfo info : chatModes.values()) {
			info.addElement(rootElement, document);
		}
	}

	public static ChatModeInfo getModeInfo(ChannelTypes type) {
		return chatModes.get(type);
	}

	public static ArrayList<ChatTabConfig> chatTabs = new ArrayList<ChatConfig.ChatTabConfig>();
	public static HashMap<ChannelTypes, ChatModeInfo> chatModes = new HashMap<ChatConfig.ChannelTypes, ChatConfig.ChatModeInfo>();

	public static Font CHAT_FONT = new Font("Serif", Font.BOLD, 14);

	public String getDefaultLocaleChannel() {
		String local = Locale.getDefault().getLanguage();
		if (local.equals("ru")) {
			return "#salemrus";
		} else return "#salem";
	}

	public static String getFirstIRCChannel() {
		for (ChatTabConfig tab : chatTabs) {
			if (tab.containsChannel(ChannelTypes.IRC)) {
				return tab.getIRCChannel();
			}
		}
		return null;
	}

	public static ArrayList<Pair<String, String>> getIRCChannels() {
		ArrayList<Pair<String, String>> ret = new ArrayList<Pair<String,String>>();
		for (ChatTabConfig tab : chatTabs) {
			if (tab.containsChannel(ChannelTypes.IRC)) {
				ret.add(new Pair<String, String>(tab.getIRCChannel(), tab.getIRCKey()));
			}
		}
		return ret;
	}

	@Override
	public void init(Element rootElement) {
		if (rootElement != null) {
			// restore defaults
			cachedElement = rootElement;
			chatTabs.clear();
		}
		String fName = "Serif"; int fSize = 14; boolean fBold = true;
		if (cachedElement.hasAttribute("font-name")) {
			fName = cachedElement.getAttribute("font-name");
		}
		if (cachedElement.hasAttribute("font-size")) {
			fSize = Integer.valueOf(cachedElement.getAttribute("font-size"));
		}
		if (cachedElement.hasAttribute("font-style-bold")) {
			fBold = Boolean.valueOf(cachedElement.getAttribute("font-style-bold"));
		}
		CHAT_FONT = new Font(fName, fBold ? Font.BOLD : Font.PLAIN, fSize);
		STextProcessor.setFont(CHAT_FONT);

		NodeList list = cachedElement.getElementsByTagName("tab");
		Element currentNode = null;
		for (int i = 0; i < list.getLength(); i++) {
			currentNode = (Element) list.item(i);

			String tName = currentNode.getAttribute("name");
			ChatTabConfig tab = new ChatTabConfig(tName);

			if (currentNode.hasAttribute("channels")) {
				String raw = currentNode.getAttribute("channels");
				for (String str : raw.split(";")) {
					tab.addChannel(ChannelTypes.valueOf(str));
				}
			}

			if (tab.containsChannel(ChannelTypes.IRC)){
				if (currentNode.hasAttribute("irc-channel")) {
					tab.setIRCChannel(currentNode.getAttribute("irc-channel"));
				} else {
					tab.setIRCChannel(getDefaultLocaleChannel());
				}
				if (currentNode.hasAttribute("irc-key")) {
					tab.setIRCKey(currentNode.getAttribute("irc-key"));
				}
				if (currentNode.hasAttribute("default")) {
					tab.setDefault(Boolean.valueOf(currentNode.getAttribute("default")));
				}
			}

			chatTabs.add(tab);
		}
		list = cachedElement.getElementsByTagName("mode");
		for (int i = 0; i < list.getLength(); i++) {
			currentNode = (Element) list.item(i);

			ChannelTypes type = ChannelTypes.valueOf(currentNode.getAttribute("type"));
			ChatModeInfo info = new ChatModeInfo(type);
			info.parse(currentNode);

			chatModes.put(type, info);
		}
	}

	public static String getDefaultChannel() {
		if (chatTabs.size() <= 0) return "";
		for (ChatTabConfig cfg : chatTabs) {
			if (cfg.isDefault()) return cfg.getName();
		}
		return chatTabs.get(0).getName();
	}

	public ChatConfig() {

	}

	protected Element cachedElement = null;

	public ChatConfig(Element rootElement) {
		cachedElement = rootElement;
	}
}
