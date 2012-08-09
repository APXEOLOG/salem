package org.apxeolog.salem.config;

import java.util.ArrayList;

import org.apxeolog.salem.ALS;
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

	public enum ChannelTypes { AREA, PARTY, VILLAGE, PM, SYSTEM, IRC };

	public static class ChatTabConfig {
		protected String tabName;
		protected ArrayList<ChannelTypes> tabChannels;

		public ChatTabConfig(String name) {
			tabChannels = new ArrayList<ChatConfig.ChannelTypes>();
			tabName = name;
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

		public void addElement(Element root) {
			root.setAttribute("name", tabName);
			StringBuilder sb = new StringBuilder();
			for (ChannelTypes type : tabChannels)  {
				sb.append(type.toString());
				sb.append(";");
			}
			root.setAttribute("channels", sb.substring(0, sb.length() - 1));
		}
	}

	@Override
	public void addElement(Element rootElement, Document document) {
		for (ChatTabConfig tab : chatTabs) {
			Element bufElem = document.createElement("tab");
			tab.addElement(bufElem);
			rootElement.appendChild(bufElem);
		}
	}

	public static ArrayList<ChatTabConfig> chatTabs = new ArrayList<ChatConfig.ChatTabConfig>();

	@Override
	public void init(Element rootElement) {
		if (rootElement != null) {
			// restore defaults
			cachedElement = rootElement;
			chatTabs.clear();
		}
		NodeList list = cachedElement.getElementsByTagName("tab");
		Element currentNode = null;
		for (int i = 0; i < list.getLength(); i++) {
			currentNode = (Element) list.item(i);

			String tName = currentNode.getAttribute("name");
			ChatTabConfig tab = new ChatTabConfig(tName);

			if (currentNode.hasAttribute("channels")) {
				String raw = currentNode.getAttribute("channels");
				for (String str : raw.split(";")) {
					ALS.alDebugPrint(str);
					tab.addChannel(ChannelTypes.valueOf(str));
				}
			}

			chatTabs.add(tab);
		}
	}

	public ChatConfig() {

	}

	protected Element cachedElement = null;

	public ChatConfig(Element rootElement) {
		cachedElement = rootElement;
	}
}
