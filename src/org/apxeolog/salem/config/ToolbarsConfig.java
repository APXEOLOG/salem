package org.apxeolog.salem.config;

import haven.Coord;
import haven.GameUI;
import haven.Widget;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;

import org.apxeolog.salem.widgets.SToolbar;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ToolbarsConfig implements IConfigExport {
	static {
		XMLConfigProvider.registerConfig("toolbars", new IConfigFactory() {
			@Override
			public IConfigExport create(Element sectionRoot) {
				if (sectionRoot == null) return new ToolbarsConfig();
				else return new ToolbarsConfig(sectionRoot);
			}
		});
	}

	public static class SToolbarConfigSlot {
		public int sMode = 0;
		public int sKey = 0;

		protected String slotString = null;

		public SToolbarConfigSlot(int mode, int key) {
			sMode = mode;
			sKey = key;
			rebuildString();
		}

		public void rebuildString() {
			slotString = "";
			if((sMode & InputEvent.CTRL_DOWN_MASK) != 0)
				slotString += "Ctrl+";
			if((sMode & (InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0)
				slotString += "Alt+";
			if((sMode & InputEvent.SHIFT_DOWN_MASK) != 0)
				slotString += "Shift";
			slotString += KeyEvent.getKeyText(sKey);
		}

		public String getString() {
			return slotString;
		}
	}

	public String tbName;
	public ArrayList<SToolbarConfigSlot> slotList;
	public boolean enabled = true;

	public ToolbarsConfig(String name) {
		tbName = name;
		slotList = new ArrayList<ToolbarsConfig.SToolbarConfigSlot>();
	}

	public void addSlot(int mode, int key) {
		slotList.add(new SToolbarConfigSlot(mode, key));
	}

	public void removeSlot(int index) {
		if (index >= 0 && index < slotList.size())
			slotList.remove(index);
	}

	@Override
	public String toString() {
		return String.format("Toolbar name: %s, %d slots", tbName, slotList.size());
	}

	public void toggle() {
		enabled = !enabled;
	}

	public static HashMap<String, ToolbarsConfig> definedToolbars = new HashMap<String, ToolbarsConfig>();

	public static void updateToolbars(Widget root) {
		GameUI gUI = root.findchild(GameUI.class);
		if (gUI != null) {
			if (gUI.bdsToolbars != null) {
				for (SToolbar tb : gUI.bdsToolbars) tb.unlink();
				gUI.bdsToolbars.clear();
			}

			if (XConfig.cl_use_new_toolbars) {
				for (ToolbarsConfig cfg : definedToolbars.values()) {
					if (cfg.enabled && cfg.slotList.size() > 0)
						new SToolbar(new Coord(10, 10), gUI, cfg);
				}
			}
		}
	}

	@Override
	public void addElement(Element rootElement, Document document) {
		for (ToolbarsConfig cfg : definedToolbars.values()) {
			Element currentNode = document.createElement("toolbar");
			currentNode.setAttribute("name", cfg.tbName);
			currentNode.setAttribute("enabled", String.valueOf(cfg.enabled));

			for (int j = 0; j < cfg.slotList.size(); j++) {
				int mode = cfg.slotList.get(j).sMode;
				int key = cfg.slotList.get(j).sKey;

				Element slot = document.createElement("slot");
				slot.setAttribute("mode", String.valueOf(mode));
				slot.setAttribute("key", String.valueOf(key));
				currentNode.appendChild(slot);
			}
			rootElement.appendChild(currentNode);
		}
	}

	@Override
	public void init(Element rootElement) {
		if (rootElement != null) {
			// restore defaults
			cachedElement = rootElement;
			definedToolbars.clear();
		}
		NodeList list = cachedElement.getElementsByTagName("toolbar");
		Element currentNode = null;
		for (int i = 0; i < list.getLength(); i++) {
			currentNode = (Element) list.item(i);

			ToolbarsConfig tcfg = new ToolbarsConfig(currentNode.getAttribute("name"));
			if (currentNode.hasAttribute("enabled"))
				tcfg.enabled = Boolean.valueOf(currentNode.getAttribute("enabled"));
			NodeList slots = currentNode.getElementsByTagName("slot");
			for (int j = 0; j < slots.getLength(); j++) {
				Element slot = (Element) slots.item(j);
				int mode = Integer.valueOf(slot.getAttribute("mode"));
				int key = Integer.valueOf(slot.getAttribute("key"));
				tcfg.addSlot(mode, key);
			}
			definedToolbars.put(tcfg.tbName, tcfg);
		}
	}

	public ToolbarsConfig() {

	}

	protected Element cachedElement = null;

	public ToolbarsConfig(Element rootElement) {
		cachedElement = rootElement;
	}
}
