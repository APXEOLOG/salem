package org.apxeolog.salem;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SToolbarConfig {
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
	
	protected String tbName;
	protected ArrayList<SToolbarConfigSlot> slotList;
	protected boolean enabled = true;
	
	public SToolbarConfig(String name) {
		tbName = name;
		slotList = new ArrayList<SToolbarConfig.SToolbarConfigSlot>();
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
	
	public static HashMap<String, SToolbarConfig> definedToolbars = new HashMap<String, SToolbarConfig>();
	
	public static void load() {
		File tbConf = new File("toolbars.xml");
		if (tbConf.exists() && tbConf.canRead()) {
			Document tbConfXML = HXml.readXMLFile(tbConf);
			if (tbConfXML != null) {
				NodeList toolbars = tbConfXML.getElementsByTagName("toolbar");
				for (int i = 0; i < toolbars.getLength(); i++) {
					Element currentToolbar = (Element) toolbars.item(i);
					SToolbarConfig tcfg = new SToolbarConfig(currentToolbar.getAttribute("name"));
					NodeList slots = currentToolbar.getElementsByTagName("slot");
					for (int j = 0; j < slots.getLength(); j++) {
						Element slot = (Element) slots.item(j);
						int mode = Integer.valueOf(slot.getAttribute("mode"));
						int key = Integer.valueOf(slot.getAttribute("key"));
						tcfg.addSlot(mode, key);
					}
					definedToolbars.put(tcfg.tbName, tcfg);
				}
			}
		}
	}
	
	public static void save() {
		File tbConf = new File("toolbars.xml");
		if (tbConf.exists() && tbConf.canWrite()) {
			Document tbConfXML = HXml.newDoc();
			if (tbConfXML != null) {
				Element root = tbConfXML.createElement("root");
				
				for (SToolbarConfig cfg : definedToolbars.values()) {
					Element currentToolbar = tbConfXML.createElement("toolbar");
					currentToolbar.setAttribute("name", cfg.tbName);
					
					for (int j = 0; j < cfg.slotList.size(); j++) {
						int mode = cfg.slotList.get(j).sMode;
						int key = cfg.slotList.get(j).sKey;
						
						Element slot = tbConfXML.createElement("slot");
						slot.setAttribute("mode", String.valueOf(mode));
						slot.setAttribute("key", String.valueOf(key));
						
						currentToolbar.appendChild(slot);
					}
					
					root.appendChild(currentToolbar);
				}
				
				tbConfXML.appendChild(root);
			}
			HXml.saveXML(tbConfXML, tbConf);
		}
	}
}
