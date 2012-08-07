package org.apxeolog.salem.config;
import haven.BuddyWnd;
import haven.Coord;
import haven.GOut;
import haven.Gob;
import haven.KinInfo;
import haven.Resource;
import haven.Tex;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class MinimapHighlightConfig implements IConfigExport {
	public static Coord minimapIconSize = new Coord(24, 24);
	public static Coord minimapDotSize = new Coord(5, 5);

	static {
		XMLConfigProvider.registerConfig("minimap", new IConfigFactory() {
			@Override
			public IConfigExport create(Element sectionRoot) {
				if (sectionRoot == null) return new MinimapHighlightConfig();
				else return new MinimapHighlightConfig(sectionRoot);
			}
		});
	}

	public enum HighlightType { ICON, DOT, PLAYER };

	public static class HighlightInfo {
		protected boolean highlight = true;
		protected String strIcon = null;
		protected String strTooltip = null;

		protected boolean allowClick = true;
		protected boolean allowMapTooltip = false;

		protected HighlightType type = HighlightType.ICON;
		protected Tex resIcon = null;
		protected Color drawColor = null;

		public HighlightInfo(HighlightType htype) {
			type = htype;
		}

		public void setBool(boolean val) {
			highlight = val;
			XMLConfigProvider.save();
		}

		public boolean getBool() {
			return highlight;
		}

		public Tex getTex() {
			return resIcon;
		}

		public void setIcon(String icon) {
			strIcon = icon;
			resIcon = Resource.loadtex(icon);
		}

		public String getIcon() {
			return strIcon;
		}

		public void setTooltip(String tooltip) {
			strTooltip = tooltip;
		}

		public String getTooltip() {
			return strTooltip;
		}

		public String getTooltip(Gob gob) {
			if (!allowMapTooltip) return null;
			if (type == HighlightType.PLAYER) {
				KinInfo kin = gob.getattr(KinInfo.class);
				if (kin != null) {
					return kin.getKinName();
				}
			}
			return strTooltip;
		}

		public void setColor(Color clr) {
			drawColor = clr;
		}

		public boolean allowClick() {
			return allowClick;
		}

		public void setAllowClick(boolean click) {
			allowClick = click;
		}

		public void setAllowMapTooltip(boolean att) {
			allowMapTooltip = att;
		}

		public Coord getSize() {
			switch (type) {
			case DOT: return minimapDotSize;
			case ICON: return minimapIconSize;
			case PLAYER: return minimapDotSize;
			default: return minimapIconSize;
			}
		}

		public void draw(GOut g, Coord ul, Gob gob) {
			if (type == HighlightType.ICON) {
				g.chcolor(Color.BLACK);
				g.fellipse(ul.add(minimapIconSize.div(2)), minimapIconSize.div(2));
				g.chcolor();
				g.image(getTex(), ul, minimapIconSize);
			} else if (drawColor != null) {
				g.chcolor(Color.BLACK);
				g.fellipse(ul.add(minimapDotSize.div(2)), minimapDotSize);
				g.chcolor(drawColor);
				g.fellipse(ul.add(minimapDotSize.div(2)), minimapDotSize.sub(1, 1));
			} else if (type == HighlightType.PLAYER && gob != null) {
				int state = 0; // 2 - enemy | 0 - neutral | 1 - friend
				KinInfo kin = gob.getattr(KinInfo.class);
				if (kin != null) {
					state = kin.getGroup();
					if (kin.inYourVillage() && state == 0) state = 1;
				}

				if (!gob.glob.party.haveMember(gob.id)) {
					g.chcolor(Color.BLACK);
					g.fellipse(ul.add(minimapDotSize.div(2)), minimapDotSize);
					g.chcolor(BuddyWnd.gc[state]);
					g.fellipse(ul.add(minimapDotSize.div(2)), minimapDotSize.sub(1, 1));
				}
			}
		}

		public void getElement(Element rootElement, Document document) {
			if (strTooltip != null) rootElement.setAttribute("tooltip", strTooltip);
			if (drawColor != null) rootElement.setAttribute("color", "0x" + Integer.toHexString(drawColor.getRGB()).substring(2).toUpperCase());
			if (strIcon != null) rootElement.setAttribute("icon", strIcon);
			rootElement.setAttribute("clickable", String.valueOf(allowClick));
			rootElement.setAttribute("mmap-tooltip", String.valueOf(allowMapTooltip));
			rootElement.setAttribute("val", String.valueOf(highlight));
			rootElement.setAttribute("type", type.toString());
		}
	}

	protected static HashMap<String, HighlightInfo> highlights = new HashMap<String, HighlightInfo>();

	public static HighlightInfo getHighlightInfo(String key) {
		return highlights.get(key);
	}

	public static HashMap<String, HighlightInfo> getHashMap() {
		return highlights;
	}

	protected Element cachedElement;

	public MinimapHighlightConfig() {

	}

	public MinimapHighlightConfig(Element sectionRoot) {
		cachedElement = sectionRoot;
	}

	@Override
	public void addElement(Element rootElement, Document document) {
		rootElement.setAttribute("icon-size", minimapIconSize.toString());
		rootElement.setAttribute("dot-size", minimapDotSize.toString());
		for (Entry<String, HighlightInfo> entry : highlights.entrySet()) {
			Element bufElem = (Element) rootElement.appendChild(document.createElement("highlight"));
			HighlightInfo info = entry.getValue();
			info.getElement(bufElem, document);
			bufElem.setAttribute("res", entry.getKey());
		}
	}

	@Override
	public void init(Element rootElement) {
		if (rootElement != null) {
			highlights.clear();
			cachedElement = rootElement;
		}
		if (cachedElement.hasAttribute("icon-size")) {
			minimapIconSize = Coord.fromString(cachedElement.getAttribute("icon-size"));
		}
		if (cachedElement.hasAttribute("dot-size")) {
			minimapDotSize = Coord.fromString(cachedElement.getAttribute("dot-size"));
		}

		NodeList list = cachedElement.getElementsByTagName("highlight");
		Element currentNode = null;
		for (int i = 0; i < list.getLength(); i++) {
			currentNode = (Element) list.item(i);

			String hName = "default", hIcon = "gfx/invobjs/missing", hTooltip = "???";
			Boolean hVal = true; HighlightType hType = HighlightType.DOT;

			if (currentNode.hasAttribute("res"))
				hName = currentNode.getAttribute("res");
			if (currentNode.hasAttribute("type"))
				hType = HighlightType.valueOf(currentNode.getAttribute("type"));
			if (currentNode.hasAttribute("val"))
				hVal = Boolean.valueOf(currentNode.getAttribute("val"));
			if (currentNode.hasAttribute("icon"))
				hIcon = currentNode.getAttribute("icon");
			if (currentNode.hasAttribute("tooltip"))
				hTooltip = currentNode.getAttribute("tooltip");
			HighlightInfo parsed = new HighlightInfo(hType);

			if (currentNode.hasAttribute("color"))
				parsed.setColor(Color.decode(currentNode.getAttribute("color")));
			if (currentNode.hasAttribute("clickable"))
				parsed.setAllowClick(Boolean.parseBoolean(currentNode.getAttribute("clickable")));
			if (currentNode.hasAttribute("mmap-tooltip"))
				parsed.setAllowMapTooltip(Boolean.parseBoolean(currentNode.getAttribute("mmap-tooltip")));

			parsed.setBool(hVal);
			parsed.setIcon(hIcon);
			parsed.setTooltip(hTooltip);
			highlights.put(hName, parsed);
		}
		cachedElement = null;
	}
}
