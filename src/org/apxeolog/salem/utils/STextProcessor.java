package org.apxeolog.salem.utils;

import haven.Coord;
import haven.GOut;
import haven.TexI;
import haven.Text;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apxeolog.salem.ALS;
import org.apxeolog.salem.Triplet;

public class STextProcessor {
	public static final Color DEFAULT = Color.WHITE;
	public static Font FONT = new Font("Serif", Font.BOLD, 14);
	public static Text.Foundry FOUNDRY = new Text.Foundry(FONT);
	public static FontMetrics METRICS;
	public static Graphics CONTEXT;
	public static Graphics STANDALONE;

	static {
		updateMetrics();
	}

	public static void updateMetrics() {
		BufferedImage junkst = TexI.mkbuf(new Coord(10, 10));
		STANDALONE = junkst.getGraphics();
		BufferedImage junk = TexI.mkbuf(new Coord(10, 10));
		CONTEXT = junk.getGraphics();
		CONTEXT.setFont(FONT);
		METRICS = CONTEXT.getFontMetrics();
	}

	public static void setFont(Font newfnt) {
		FONT = newfnt;
		FOUNDRY = new Text.Foundry(FONT);
		updateMetrics();
	}

	public static Rectangle2D getStringBounds(String str) {
		return METRICS.getStringBounds(str, CONTEXT);
	}

	public static Rectangle2D getStringBounds(String str, Font fnt) {
		if (STANDALONE.getFont() != fnt) {
			ALS.alDebugPrint("set fnt");
			STANDALONE.setFont(fnt);
		}
		return STANDALONE.getFontMetrics().getStringBounds(str, STANDALONE);
	}

	public static abstract class NodeAttribute {
		@Override
		public String toString() {
			return this.getClass().getSimpleName();
		}

		public abstract String getData();

		public abstract Hashtable<TextAttribute, Object> getFontAttrs();

		public abstract Color getColor();

		public abstract void act(int button, boolean ctrl, boolean shft);
	}

	public static String getGroupData(Matcher matcher) {
		if (matcher.groupCount() > 1) {
			return matcher.group(1);
		} else
			return matcher.group(0);
	}

	public static class TextUrl extends NodeAttribute {
		protected String url;

		public TextUrl(Matcher match) {
			if (match.groupCount() > 1)
				url = match.group(1);
			else url = match.group();
		}

		@Override
		public String toString() {
			return url;
		}

		@Override
		public String getData() {
			return url;
		}

		@Override
		public Hashtable<TextAttribute, Object> getFontAttrs() {
			Hashtable<TextAttribute, Object> fontAttrs = new Hashtable<TextAttribute, Object>();
			fontAttrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
			fontAttrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
			return fontAttrs;
		}

		@Override
		public Color getColor() {
			return Color.BLUE;
		}

		@Override
		public void act(int button, boolean ctrl, boolean shft) {
			if (button == 1) {
				if (Desktop.isDesktopSupported()) {
					try {
						Desktop.getDesktop().browse(new URI(url));
					} catch (Exception e) {
						ALS.alDebugPrint("Desktop exception...");
					}
				}
			}
		}
	}

	public static class TextColor extends NodeAttribute {
		protected Color clr = Color.WHITE;
		protected String data;

		public TextColor(Matcher match) {
			if (match.groupCount() > 2) {
				String[] parts = match.group(1).substring(1).split(",");
				if (parts.length == 3) clr = new Color(Integer.parseInt(parts[0]),
						Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
				else clr = Color.WHITE;
				data = match.group(2);
			} else {
				data = match.group();
			}
		}

		@Override
		public String toString() {
			return clr.toString();
		}

		@Override
		public String getData() {
			return data;
		}

		@Override
		public Hashtable<TextAttribute, Object> getFontAttrs() {
			Hashtable<TextAttribute, Object> fontAttrs = new Hashtable<TextAttribute, Object>();
			return fontAttrs;
		}

		@Override
		public Color getColor() {
			return clr;
		}

		@Override
		public void act(int button, boolean ctrl, boolean shft) {
			// TODO Auto-generated method stub

		}
	}

	public static class TextBold extends NodeAttribute {
		protected String data;

		public TextBold(Matcher match) {
			if (match.groupCount() > 1)
				data = match.group(1);
			else data = match.group();
		}

		@Override
		public String getData() {
			return data;
		}

		@Override
		public Hashtable<TextAttribute, Object> getFontAttrs() {
			Hashtable<TextAttribute, Object> fontAttrs = new Hashtable<TextAttribute, Object>();
			fontAttrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
			return fontAttrs;
		}

		@Override
		public Color getColor() {
			return null;
		}

		@Override
		public void act(int button, boolean ctrl, boolean shft) {
			// TODO Auto-generated method stub

		}
	}

	public static class TextUnderlined extends NodeAttribute {
		protected String data;

		public TextUnderlined(Matcher match) {
			if (match.groupCount() > 1)
				data = match.group(1);
			else data = match.group();
		}

		@Override
		public String getData() {
			return data;
		}

		@Override
		public Hashtable<TextAttribute, Object> getFontAttrs() {
			Hashtable<TextAttribute, Object> fontAttrs = new Hashtable<TextAttribute, Object>();
			fontAttrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
			return fontAttrs;
		}

		@Override
		public Color getColor() {
			return null;
		}

		@Override
		public void act(int button, boolean ctrl, boolean shft) {
			// TODO Auto-generated method stub

		}
	}

	public static class TextStrike extends NodeAttribute {
		protected String data;

		public TextStrike(Matcher match) {
			if (match.groupCount() > 1)
				data = match.group(1);
			else data = match.group();
		}

		@Override
		public String getData() {
			return data;
		}

		@Override
		public Hashtable<TextAttribute, Object> getFontAttrs() {
			Hashtable<TextAttribute, Object> fontAttrs = new Hashtable<TextAttribute, Object>();
			fontAttrs.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
			return fontAttrs;
		}

		@Override
		public Color getColor() {
			return null;
		}

		@Override
		public void act(int button, boolean ctrl, boolean shft) {
			// TODO Auto-generated method stub

		}
	}

	public static class Node {
		protected String rawString;
		protected ArrayList<NodeAttribute> attributes;
		protected Font currentStateFont;
		protected double widthCache = -1, heightCache = -1;

		public Node(String str) {
			rawString = str;
			attributes = new ArrayList<STextProcessor.NodeAttribute>();
		}

		public String getString() {
			return rawString;
		}

		public void act(int button, boolean ctrl, boolean shft) {
			for (NodeAttribute attr : attributes) {
				attr.act(button, ctrl, shft);
			}
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private Font updateStyleFont() {
			Hashtable fontAttrs = new Hashtable();
			for (int i = 0; i < attributes.size(); i++) {
				NodeAttribute attr = attributes.get(i);
				for (Entry<TextAttribute, Object> kvp : attr.getFontAttrs().entrySet()) {
					fontAttrs.put(kvp.getKey(), kvp.getValue());
				}
			}
			currentStateFont = FONT.deriveFont(fontAttrs);
			return currentStateFont;
		}


		protected Color getColorFromAttrib() {
			for (NodeAttribute attr : attributes) {
				if (attr.getColor() != null) return attr.getColor();
			}
			return DEFAULT;
		}

		public Font getAttribFont() {
			if (currentStateFont == null) updateStyleFont();
			return currentStateFont;
		}

		public double getNodeWidth() {
			if (widthCache < 0) widthCache = getStringBounds(rawString, getAttribFont()).getWidth();
			return widthCache;
		}

		public double getNodeHeight() {
			if (heightCache < 0) heightCache = getStringBounds(rawString, getAttribFont()).getHeight();
			return heightCache;
		}

		public Node addAttributes(NodeAttribute... attr) {
			currentStateFont = null; heightCache = -1; widthCache = -1;
			for (NodeAttribute atr : attr)
				attributes.add(atr);
			return this;
		}

		public NodeAttribute[] getAttributes() {
			return attributes.toArray(new NodeAttribute[attributes.size()]);
		}

		public void draw(GOut g, Coord c) {

		}

		@Override
		public String toString() {
			String className = this.getClass().getSimpleName();
			String raw = "";
			for (int i = 0; i < attributes.size(); i++) {
				raw += String.format(" {%s=%s}", attributes.get(i).getClass().getSimpleName(), attributes.get(i).toString());
			}
			return String.format("[%s=%s]%s[/%s|W:%d|H:%d]", className, raw, rawString, className, (int)getNodeWidth(), (int)getNodeHeight());
		}
	}

	public static class RawNode extends Node {
		protected Text renderedPart;

		public RawNode(String str) {
			super(str);
		}

		public void setup() {
			Text.Foundry renderF = new Text.Foundry(getAttribFont());
			renderedPart = renderF.render(rawString, getColorFromAttrib());
		}

		public Text getText() {
			if (renderedPart == null) setup();
			return renderedPart;
		}

		public ArrayList<RawNode> splitWidth(double width) {
			ArrayList<RawNode> newNodes = new ArrayList<RawNode>();
			int start = 0;
			for (int i = 3; i < rawString.length(); i+=5) {
				if (getStringBounds(rawString.substring(start, i), getAttribFont()).getWidth() >= (width - 5)) {
					RawNode nn = new RawNode(rawString.substring(start, i - 5));
					nn.addAttributes(getAttributes());
					newNodes.add(nn);
					start = (i - 5);
				}
			}
			if (start < rawString.length() - 1) {
				RawNode nn = new RawNode(rawString.substring(start, rawString.length()));
				nn.addAttributes(getAttributes());
				newNodes.add(nn);
			}
			return newNodes;
		}

		@Override
		public void draw(GOut g, Coord c) {
			g.aimage(getText().tex(), c, 0, 0);
		}
	}

	public static class WhitespaceNode extends Node {
		public WhitespaceNode(String str) {
			super(" ");
		}

		@Override
		public void draw(GOut g, Coord c) {
			// Nothing
		}
	}

	protected static ArrayList<Triplet<Pattern, PatternType, Class<?>>> patterns = new ArrayList<Triplet<Pattern, PatternType, Class<?>>>();

	enum PatternType { Node, Attribute };

	static {
		patterns.add(new Triplet<Pattern, PatternType, Class<?>>(Pattern.compile("((([A-Za-z]{3,9}:(?:\\/\\/)?)(?:[-;:&=\\+\\$,\\w]+@)?[A-Za-z0-9.-]+|(?:www.|[-;:&=\\+\\$,\\w]+@)[A-Za-z0-9.-]+)((?:\\/[\\+~%\\/.\\w-_]*)?\\??(?:[-\\+=&,;%@.\\w_]*)#?(?:[.\\!\\/\\\\w]*))?)"), PatternType.Attribute, TextUrl.class));
		patterns.add(new Triplet<Pattern, PatternType, Class<?>>(Pattern.compile("\\[b]((.*?))\\[/b]"), PatternType.Attribute, TextBold.class));
		patterns.add(new Triplet<Pattern, PatternType, Class<?>>(Pattern.compile("\\[u]((.*?))\\[/u]"), PatternType.Attribute, TextUnderlined.class));
		patterns.add(new Triplet<Pattern, PatternType, Class<?>>(Pattern.compile("\\[s]((.*?))\\[/s]"), PatternType.Attribute, TextStrike.class));
		patterns.add(new Triplet<Pattern, PatternType, Class<?>>(Pattern.compile("\\[c(=\\d+,\\d+,\\d+|.*)]((.*?))\\[/c]"), PatternType.Attribute, TextColor.class));
		patterns.add(new Triplet<Pattern, PatternType, Class<?>>(Pattern.compile("\\s"), PatternType.Node, WhitespaceNode.class));
	}

	public static NodeAttribute getAttribute(Class<?> nodecl, Matcher match) {
		if (nodecl.equals(TextUrl.class)) {
			return new TextUrl(match);
		} else if (nodecl.equals(TextBold.class)) {
			return new TextBold(match);
		} else if (nodecl.equals(TextUnderlined.class)) {
			return new TextUnderlined(match);
		} else if (nodecl.equals(TextStrike.class)) {
			return new TextStrike(match);
		} else if (nodecl.equals(TextColor.class)) {
			return new TextColor(match);
		}
		return null;
	}

	public static Node createNode(Class<?> nodecl, Matcher match) {
		if (nodecl.equals(RawNode.class)) {
			return new RawNode(match.group());
		} else if (nodecl.equals(WhitespaceNode.class)) {
			return new WhitespaceNode(null);
		}
		return null;
	}

	public static class ProcessedText {
		protected LinkedList<Node> nodeList;
		protected ArrayList<Integer> lines;

		public ProcessedText(ArrayList<Node> noded) {
			nodeList = new LinkedList<STextProcessor.Node>();
			for (Node n : noded)
				nodeList.addLast(n);
			lines = new ArrayList<Integer>();
		}

		public ProcessedText append(ProcessedText text) {
			for (Node n : text.nodeList)
				nodeList.addLast(n);
			return this;
		}

		public void addAttribute(NodeAttribute attr) {
			for (Node n : nodeList) {
				if (n instanceof RawNode) {
					n.addAttributes(attr);
				}
			}
		}

		public void pack(Coord sz) {
			// split big nodes
			for (int i = 0; i < nodeList.size(); i++) {
				if (nodeList.get(i).getNodeWidth() >= (sz.x - 3)) {
					RawNode bigNode = (RawNode) nodeList.get(i);
					nodeList.remove(i); int z = i;
					for (RawNode n : bigNode.splitWidth(sz.x)) {
						nodeList.add(z, n); z++;
					}
				}
			}
			lines.clear();
			lines.add(0);
			double wbuf = 0;
			for (int i = 0; i < nodeList.size(); i++) {
				wbuf += nodeList.get(i).getNodeWidth();
				if (wbuf >= sz.x) {
					wbuf = nodeList.get(i).getNodeWidth();
					lines.add(i);
				}
			}
		}

		private int getLineEnd(int linesIndex) {
			if (linesIndex >= 0 && linesIndex < lines.size()) {
				int start = lines.get(linesIndex);
				for (int i = start + 1; i < nodeList.size(); i++) {
					if (lines.contains(i)) return i;
				}
				return nodeList.size();
			}
			return linesIndex;
		}

		public int getLinesCount() {
			return lines.size();
		}

		public double getLineHeight(int index) {
			if (index >= 0 && index < lines.size()) {
				return nodeList.get(lines.get(index)).getNodeHeight();
			} else return -1;
		}

		public List<Node> getLine(int index) {
			if (index >= 0 && index < lines.size()) {
				int start = lines.get(index);
				int end = getLineEnd(index);
				return nodeList.subList(start, end);
			}
			return null;
		}

		public double getHeight() {
			double hbuf = 0;
			for (int i = 0; i < lines.size(); i++) {
				hbuf += nodeList.get(lines.get(i)).getNodeHeight();
			}
			return hbuf;
		}

		public Node getNode(int index) {
			return nodeList.get(index);
		}

		public void printRaw() {
			for (int i = 0; i < nodeList.size(); i++) {
				if (lines.contains(i)) System.out.println();
				System.out.print(nodeList.get(i).getString());
			}
		}

		public void printNodes() {
			System.out.println("[NodeList]");
			for (int i = 0; i < nodeList.size(); i++) {
				System.out.println("\t" + nodeList.get(i).toString());
			}
			System.out.println("[/NodeList]");
		}
	}

	public static void printNodes(ArrayList<Node> nodeList) {
		System.out.println("[NodeList]");
		for (int i = 0; i < nodeList.size(); i++) {
			System.out.println("\t" + nodeList.get(i).toString());
		}
		System.out.println("[/NodeList]");
	}

	public static String getColoredText(String str, Color clr) {
		return String.format("[c=%d,%d,%d]%s[/c]", clr.getRed(), clr.getGreen(), clr.getBlue(), str);
	}

	public static ProcessedText fromString(String str) {
		Matcher matcher;

		ArrayList<Node> nodeList = new ArrayList<STextProcessor.Node>();
		ArrayList<Node> nodeBuffer = new ArrayList<STextProcessor.Node>();

		nodeList.add(new RawNode(str)); // put all string initially

		// Handle BB codes and regexes
		int j = 0;
		for (int i = 0; i < patterns.size(); i++) {
			j = 0;
			while (j < nodeList.size()) {
				Node current = nodeList.get(j);
				if (current instanceof RawNode) {
					Triplet<Pattern, PatternType, Class<?>> triplet = patterns.get(i);
					matcher = triplet.getFirst().matcher(current.getString());

					boolean matched = false; int lastMatchEnd = -1;
					while (matcher.find()) {
						matched = true;
						int start = matcher.start();
						int end = matcher.end();
						if (start != 0) {
							if (lastMatchEnd > -1) {
								nodeBuffer.add(new RawNode(current.getString().substring(lastMatchEnd, start)).addAttributes(current.getAttributes()));
							} else nodeBuffer.add(new RawNode(current.getString().substring(0, start)).addAttributes(current.getAttributes()));
						}

						if (triplet.getSecond() == PatternType.Node) {
							nodeBuffer.add(createNode(triplet.getThird(), matcher));
						} else {
							NodeAttribute attr = getAttribute(triplet.getThird(), matcher);
							String match = attr.getData();
							nodeBuffer.add(new RawNode(match).addAttributes(attr));
						}

						lastMatchEnd = end;
					}
					if (lastMatchEnd > -1) {
						if (lastMatchEnd < current.getString().length()) {
							nodeList.set(j, new RawNode(current.getString().substring(lastMatchEnd, current.getString().length())).addAttributes(current.getAttributes()));
							j--;
						}
					}
					if (!matched) {
						nodeBuffer.add(current);
					}
				} else {
					nodeBuffer.add(current);
				}
				j++;
			}

			nodeList = nodeBuffer;
			nodeBuffer = new ArrayList<STextProcessor.Node>();
		}
		return new ProcessedText(nodeList);
	}

	public static void main(String[] args) throws Exception {
		Pattern test = Pattern.compile("\\[([^]=]+)(?:=[^]]+)?]([^\\[]*)\\[/\1]");
		Matcher m = test.matcher("[c=val]wr[]rew][er]wr[/c]");
		while (m.find()) {
			ALS.alDebugPrint("match!");
			for (int i = 0; i < m.groupCount(); i++)
				System.out.print(m.group(i) + " ");
		}
	}

}
