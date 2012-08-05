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

import org.apxeolog.salem.SUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class MinimapHighlightConfig implements IConfigExport {
	static {
		XMLConfigProvider.registerConfig("minimap", new IConfigFactory() {
			@Override
			public IConfigExport create(Element sectionRoot) {
				if (sectionRoot == null) return new MinimapHighlightConfig();
				else return new MinimapHighlightConfig(sectionRoot);
			}
		});
	}

	public static abstract class HighlightInfo {
		protected boolean highlight = true;
		protected Tex resIcon = null;
		protected String strIcon = null;
		protected String hTooltip = null;

		public HighlightInfo() {

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
			hTooltip = tooltip;
		}

		public String getTooltip() {
			return hTooltip;
		}

		public abstract void draw(GOut g, Coord ul, Gob gob);

		public abstract void getElement(Element rootElement, Document document);
	}

	public static class InvobjsHighlightInfo extends HighlightInfo {
		public InvobjsHighlightInfo() {

		}

		@Override
		public void setIcon(String icon) {
			Resource base = Resource.load(icon);
			base.loadwait();
			strIcon = icon;
			resIcon = base.layer(Resource.imgc).tex();
			hTooltip = base.layer(Resource.tooltip).t;
		}

		@Override
		public void draw(GOut g, Coord ul, Gob gob) {
			g.chcolor(Color.BLACK);
			g.fellipse(ul, SUtils.minimapIconSize.div(2));
			g.chcolor();
			g.image(getTex(), ul.sub(SUtils.minimapIconSize.div(2)), SUtils.minimapIconSize);
		}

		@Override
		public void getElement(Element rootElement, Document document) {

		}
	}

	public static class CompositeHighlightInfo extends HighlightInfo {
		public CompositeHighlightInfo() {

		}

		@Override
		public void draw(GOut g, Coord ul, Gob gob) {
			g.chcolor(Color.BLACK);
			g.fellipse(ul, SUtils.minimapIconSize.div(2));
			g.chcolor();
			g.image(getTex(), ul.sub(SUtils.minimapIconSize.div(2)), SUtils.minimapIconSize);
		}

		@Override
		public void getElement(Element rootElement, Document document) {
			rootElement.setAttribute("tooltip", hTooltip);
		}
	}

	public static class DotHighlightInfo extends CompositeHighlightInfo {
		protected Color hlColor = new Color(150, 75, 0);

		public DotHighlightInfo(Color clr) {
			hlColor = clr;
		}

		public Color getColor() {
			return hlColor;
		}

		@Override
		public void draw(GOut g, Coord ul, Gob gob) {
			g.chcolor(Color.BLACK);
			g.fellipse(ul, new Coord(5, 5));
			g.chcolor(hlColor);
			g.fellipse(ul, new Coord(4 ,4));
		}

		@Override
		public void getElement(Element rootElement, Document document) {
			super.getElement(rootElement, document);
			rootElement.setAttribute("color", "0x" + Integer.toHexString(hlColor.getRGB()).substring(2).toUpperCase());
		}
	}


	public static class PlayerHighlightInfo extends CompositeHighlightInfo {
		@Override
		public void draw(GOut g, Coord ul, Gob gob) {
			int state = 0; // 2 - enemy | 0 - neutral | 1 - friend
			KinInfo kin = gob.getattr(KinInfo.class);
			if (kin != null) {
				state = kin.getGroup();
				if (kin.inYourVillage() && state == 0) state = 1;
			}

			if (!gob.glob.party.haveMember(gob.id)) {
				g.chcolor(Color.BLACK);
				g.fellipse(ul, new Coord(5, 5));
				g.chcolor(BuddyWnd.gc[state]);
				g.fellipse(ul, new Coord(4 ,4));
			}
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
		for (Entry<String, HighlightInfo> entry : highlights.entrySet()) {
			Element bufElem = (Element) rootElement.appendChild(document.createElement("highlight"));
			HighlightInfo info = entry.getValue();
			info.getElement(bufElem, document);
			bufElem.setAttribute("icon", info.getIcon());
			bufElem.setAttribute("val", String.valueOf(info.getBool()));
			bufElem.setAttribute("type", info.getClass().getSimpleName());
			bufElem.setAttribute("res", entry.getKey());
		}
	}

	@Override
	public void init() {
		NodeList list = cachedElement.getElementsByTagName("highlight");
		Element currentNode = null;
		for (int i = 0; i < list.getLength(); i++) {
			currentNode = (Element) list.item(i);

			String hName = currentNode.getAttribute("res");
			String hType = currentNode.getAttribute("type");
			Boolean hVal = Boolean.valueOf(currentNode.getAttribute("val"));
			String hIcon = currentNode.getAttribute("icon");
			String hTooltip = currentNode.getAttribute("tooltip");

			if (hIcon == null || hIcon.isEmpty()) hIcon = "apx/gfx/mmap/icon-bear";

			HighlightInfo parsed = null;

			if (hType.equals("InvobjsHighlightInfo")) {
				parsed = new InvobjsHighlightInfo();
				parsed.setIcon(hIcon);
			} else if (hType.equals("CompositeHighlightInfo")) {
				parsed = new CompositeHighlightInfo();
				parsed.setTooltip(hTooltip);
				parsed.setIcon(hIcon);
			} else if (hType.equals("DotHighlightInfo")) {
				Color color = Color.decode(currentNode.getAttribute("color"));
				parsed = new DotHighlightInfo(color);
				parsed.setTooltip(hTooltip);
				parsed.setIcon(hIcon);
			} else if (hType.equals("PlayerHighlightInfo")) {
				parsed = new PlayerHighlightInfo();
				parsed.setTooltip(hTooltip);
				parsed.setIcon(hIcon);
			}
			parsed.setBool(hVal);

			highlights.put(hName, parsed);
		}
		cachedElement = null;
	}
}
