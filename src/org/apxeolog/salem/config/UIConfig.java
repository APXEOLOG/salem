package org.apxeolog.salem.config;

import haven.Coord;

import java.util.HashMap;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UIConfig implements IConfigExport {
	static {
		XMLConfigProvider.registerConfig("ui", new IConfigFactory() {
			@Override
			public IConfigExport create(Element sectionRoot) {
				if (sectionRoot == null) return new UIConfig();
				else return new UIConfig(sectionRoot);
			}
		});
	}

	public static class WidgetState {
		private HashMap<String, String> additionalTokens;
		public Coord wSize;
		public Coord wPos;

		public WidgetState(Coord sz, Coord pos) {
			wSize = sz;
			wPos = pos;
			additionalTokens = new HashMap<String, String>();
		}

		public void setToken(String name, String value) {
			additionalTokens.put(name, value);
		}

		public String getToken(String name) {
			return additionalTokens.get(name);
		}

		public void addElement(Element rootElement) {
			for (Entry<String, String> entry : additionalTokens.entrySet()) {
				rootElement.setAttribute(entry.getKey(), entry.getValue());
			}
			rootElement.setAttribute("size", wSize.toString());
			rootElement.setAttribute("pos", wPos.toString());
		}
	}

	protected static HashMap<String, WidgetState> storedTokens = new HashMap<String, WidgetState>();

	@Override
	public void addElement(Element rootElement, Document document) {
		for (Entry<String, WidgetState> entry : storedTokens.entrySet()) {
			Element bufElem = (Element) rootElement.appendChild(document.createElement("widget"));
			entry.getValue().addElement(bufElem);
			bufElem.setAttribute("name", entry.getKey());
		}
	}

	@Override
	public void init(Element rootElement) {

	}

	public UIConfig() {

	}

	public static WidgetState getWidgetState(String rawName) {
		String cleared = rawName.replaceAll("\\W", "").replaceAll("\\s+", " ");
		return storedTokens.get(cleared);
	}

	public static WidgetState getNewWidgetState(String rawName) {
		String cleared = rawName.replaceAll("\\W", "").replaceAll("\\s+", " ");
		WidgetState ws = storedTokens.get(cleared);
		if (ws == null) {
			ws = new WidgetState(Coord.z, Coord.z);
			storedTokens.put(cleared, ws);
		}
		return ws;
	}

	public UIConfig(Element rootElement) {
		NodeList list = rootElement.getElementsByTagName("widget");
		Element currentNode = null;
		for (int i = 0; i < list.getLength(); i++) {
			currentNode = (Element) list.item(i);

			String wdgName = currentNode.getAttribute("name");
			currentNode.removeAttribute("name");
			Coord wdgSize = Coord.fromString(currentNode.getAttribute("size"));
			currentNode.removeAttribute("size");
			Coord wdgPos = Coord.fromString(currentNode.getAttribute("pos"));
			currentNode.removeAttribute("pos");
			WidgetState ws = new WidgetState(wdgSize, wdgPos);

			NamedNodeMap nnm = currentNode.getAttributes();
			for (int j = 0; j < nnm.getLength(); j++) {
				Node attr = nnm.item(j);
				ws.setToken(attr.getNodeName(), attr.getNodeValue());
			}
			storedTokens.put(wdgName, ws);
		}
	}
}
