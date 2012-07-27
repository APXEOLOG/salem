package org.apxeolog.salem.utils;

import haven.RichText;
import haven.Text;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apxeolog.salem.Pair;

public class STextProcessor {
	public static Font FONT = new Font("Serif", Font.BOLD, 14);
	public static RichText.Foundry FOUNDRY = new RichText.Foundry(TextAttribute.FONT, FONT);
	
	public void setFont(Font newfnt) {
		FONT = newfnt;
		FOUNDRY = new RichText.Foundry(TextAttribute.FONT, FONT);
	}
	
	public static class NodeAttribute {
		
	}
	
	public static class TextUrl extends NodeAttribute {
		protected String url;
		
		public TextUrl(String url) {
			this.url = url;
		}
	}
	
	public static class TextBold extends NodeAttribute {

	}

	public static class TextUnderlined extends NodeAttribute {

	}

	public static class TextStrike extends NodeAttribute {

	}
		
	public static class Node {
		protected String rawString;
		protected ArrayList<NodeAttribute> attributes;
		
		public Node(String str) {
			rawString = str;
			attributes = new ArrayList<STextProcessor.NodeAttribute>();
		}
		
		public String getString() {
			return rawString;
		}
		
		public void addAttribute(NodeAttribute attr) {
			attributes.add(attr);
		}
		
		@Override
		public String toString() {
			String className = this.getClass().getSimpleName();
			return String.format("[%s]%s[/%s]", className, rawString, className);
		}
	}
	
	public static class RawNode extends Node {
		public RawNode(String str) {
			super(str);
		}
	}
	
	public static class TextNode extends Node {
		public Text renderedPart;

		public TextNode(String str) {
			super(str);
		}
	}
	
	public static class BNode extends TextNode {
		public BNode(String str) {
			super(str);
		}
	}
	
	public static class UNode extends TextNode {
		public UNode(String str) {
			super(str);
		}
	}
	
	public static class SNode extends TextNode {
		public SNode(String str) {
			super(str);
		}
	}
	
	public static class UrlNode extends TextNode {
		public UrlNode(String str) {
			super(str);
		}
	}
	
	public static class WhitespaceNode extends Node {
		public WhitespaceNode(String str) {
			super(" ");
		}
	}
	
	public static class ProcessedText {
		
	}
	
	protected static ArrayList<Pair<Pattern, Class<?>>> patterns = new ArrayList<Pair<Pattern, Class<?>>>();
	
	static {
		//patterns.add(new Pair<Pattern, Class<?>>(Pattern.compile("\\[url\\](.+?)\\[/url\\]"), UrlNode.class));
		patterns.add(new Pair<Pattern, Class<?>>(Pattern.compile("(http|https|ftp)\\://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,3}(:[a-zA-Z0-9]*)?/?([a-zA-Z0-9\\-\\._\\?\\,\\'/\\\\+&amp;%\\$#\\=~])*[^\\.\\,\\)\\(\\s]"), TextUrl.class));
		patterns.add(new Pair<Pattern, Class<?>>(Pattern.compile("\\[b\\](.+?)\\[/b\\]"), TextBold.class));
		patterns.add(new Pair<Pattern, Class<?>>(Pattern.compile("\\[u\\](.+?)\\[/u\\]"), TextUnderlined.class));
		patterns.add(new Pair<Pattern, Class<?>>(Pattern.compile("\\[s\\](.+?)\\[/s\\]"), TextStrike.class));
		patterns.add(new Pair<Pattern, Class<?>>(Pattern.compile("\\s"), WhitespaceNode.class));
	}
	
	public static NodeAttribute getAttribute(Class<?> nodecl, String match) {
		if (nodecl.equals(TextUrl.class)) {
			return new TextUrl(match);
		} else if (nodecl.equals(TextBold.class)) {
			return new TextBold();
		} else if (nodecl.equals(TextUnderlined.class)) {
			return new TextUnderlined();
		} else if (nodecl.equals(TextStrike.class)) {
			return new TextStrike();
		} /*else if (nodecl.equals(WhitespaceNode.class)) {
			return new WhitespaceNode(null);
		}*/
		return null;
	}
	
	public static Node createNode(String str, Class<?> nodecl) {
		if (nodecl.equals(RawNode.class)) {
			return new RawNode(str);
		} else if (nodecl.equals(WhitespaceNode.class)) {
			return new WhitespaceNode(null);
		}
		return null;
	}
	
	public static void printNodes(ArrayList<Node> nodes) {
		System.out.println("[NodeList]");
		for (int i = 0; i < nodes.size(); i++) {
			System.out.println("\t" + nodes.get(i).toString());
		}
		System.out.println("[/NodeList]");
	}
	
	public static void fromString(String str) {
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
					Pair<Pattern, Class<?>> pair = patterns.get(i);
					matcher = pair.getKey().matcher(current.getString());
					
					boolean matched = false; int lastMatchEnd = -1;
					while (matcher.find()) {
						matched = true;
						int start = matcher.start();
						int end = matcher.end();
						
						if (start != 0) {
							if (lastMatchEnd > -1) {
								nodeBuffer.add(new RawNode(current.getString().substring(lastMatchEnd, start)));
							} else nodeBuffer.add(new RawNode(current.getString().substring(0, start)));
						}
						nodeBuffer.add(createNode(current.getString().substring(start, end), pair.getValue()));
						
						lastMatchEnd = end;
					}
					if (lastMatchEnd > -1) {
						if (lastMatchEnd < current.getString().length() - 1) {
							nodeList.set(j, new RawNode(current.getString().substring(lastMatchEnd, current.getString().length() - 1)));
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
		
		printNodes(nodeList);
		
		
		
	}
	
	public static void main(String[] args) throws Exception {
		fromString("Hey Joe! I [b]wanna[/b] give you this [s]focking cool link[/s] https://www.google.ru/search?sugexp=chrome,mod=5&sourceid=chrome&ie=UTF-8&q=java+BB+code+parser man:[b][url]http://ru20.voyna-plemyon.ru/game.php?village=12178&screen=overview[/url].[/b] How r u? [u]PEWPEWPE[/u]WPEW!");
	}
	
}
