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

	public static class TBSlot {
		public int sMode = 0;
		public int sKey = 0;
		public int sGLobal = 0;

		protected String slotString = null;

		public TBSlot(int mode, int key) {
			sMode = mode;
			sKey = key;
			sGLobal = getFreeGlobalSlot();
			rebuildString();
		}

		public TBSlot(int mode, int key, int global) {
			sMode = mode;
			sKey = key;
			sGLobal = global;
			rebuildString();
		}

		public void rebuildString() {
			slotString = "";
			if((sMode & InputEvent.CTRL_DOWN_MASK) != 0)
				slotString += "C+";
			if((sMode & (InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0)
				slotString += "A+";
			if((sMode & InputEvent.SHIFT_DOWN_MASK) != 0)
				slotString += "S+";
			slotString += KeyEvent.getKeyText(sKey);
			if(slotString.contains("Numpad-"))
				slotString = slotString.replace("Numpad-", "N+");
			if(slotString.contains("Minus"))
				slotString = slotString.replace("Minus", "-");
			if(slotString.contains("Equals"))
				slotString = slotString.replace("Equals", "=");
		}

		public String getString() {
			return slotString;
		}
	}

	public static class TBConfig {
		public ArrayList<TBSlot> tbSlots;
		public String tbName;
		public boolean isEnabled = true;
		public boolean isVertical = false;
		public boolean isLocked = true;

		public TBConfig(String name) {
			tbName = name;
			tbSlots = new ArrayList<ToolbarsConfig.TBSlot>();
		}
	}

	public String tbName;
	public ArrayList<TBSlot> slotList;
	public boolean enabled = true;

	public ToolbarsConfig(String name) {
		tbName = name;
		slotList = new ArrayList<ToolbarsConfig.TBSlot>();
	}

	public void addSlot(int mode, int key, int global) {
		slotList.add(new TBSlot(mode, key, global));
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
				TBSlot slotc = cfg.slotList.get(j);
				Element slot = document.createElement("slot");
				slot.setAttribute("mode", String.valueOf(slotc.sMode));
				slot.setAttribute("key", String.valueOf(slotc.sKey));
				slot.setAttribute("id", String.valueOf(slotc.sGLobal));
				currentNode.appendChild(slot);
			}
			rootElement.appendChild(currentNode);
		}
	}

	private static int[] slotIndexes = new int[144];

	public static int getFreeGlobalSlot() {
		for (int i = 0; i < slotIndexes.length; i++) {
			if (slotIndexes[i] < 0) return i;
		}
		return -1;
	}

	public static void markGlobalSlot(int slot) {
		if (slot >= 0 && slot < slotIndexes.length) slotIndexes[slot] = 1;
	}

	@Override
	public void init(Element rootElement) {
		if (rootElement != null) {
			// restore defaults
			cachedElement = rootElement;
			definedToolbars.clear();
		}
		for (int i = 0; i < slotIndexes.length; i++) slotIndexes[i] = -1;
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
				int global = getFreeGlobalSlot();
				if (slot.hasAttribute("id")) {
					global = Integer.valueOf(slot.getAttribute("id"));
				}
				markGlobalSlot(global);
				tcfg.addSlot(mode, key, global);
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
