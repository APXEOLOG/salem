package org.apxeolog.salem;

import haven.Coord;
import haven.GOut;
import haven.Text;
import haven.Widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class SChat extends Widget {
	public static Font chatFont = new Font("Serif", Font.BOLD, 14);
	public static FontRenderContext chatFontContext = new FontRenderContext(null, true, true);
	public static Rectangle2D chatLineBound = chatFont.getStringBounds("TEST", chatFontContext);
	public static Text.Foundry textFoundry = new Text.Foundry(chatFont);
	
	protected static class ChatLine {
		public Text cachedLine = null;
		public Text cachedHeader = null;
		public boolean containsHeader = false;
		
		public ChatLine(String text, Color tColor, String hName, Color hColor) {
			containsHeader = (hName != null && hColor != null);
			cachedLine = textFoundry.render(text.substring(hName.length()), tColor);
			if (containsHeader) {
				cachedHeader = textFoundry.render(hName, hColor);
			}
		}
		
		public void render(GOut g, Coord c) {
			if (containsHeader) {
				g.image(cachedHeader.img, c);
				g.image(cachedLine.img, c.add(cachedHeader.sz().x, 0));
			} else g.image(cachedLine.img, c);
		}
	}
	
	protected ArrayList<ChatLine> chatLines;
	protected int firstLineIndex = 0;
	protected boolean appendMode = true;
	
	public SChat(Coord c, Coord sz, Widget parent) {
		super(c, sz, parent);
		chatLines = new ArrayList<ChatLine>();
	}
	
	public void addMessage(String msg, Color mColor, String hName, Color hColor) {
		// Add chat header
		String text = hName + msg;
		int headerLength = hName.length();
		boolean isHeader = true;
		// Create glyph vector
		GlyphVector gVector = chatFont.layoutGlyphVector(chatFontContext, text.toCharArray(), 0, text.length(), 0);
		int translateXIndex = 0; double translateXWidth = 0; int lastWhitespaceIndex = -1;
		for (int i = 0; i < gVector.getNumGlyphs(); i++) {
			// Detect line bound
			if (gVector.getGlyphPosition(i).getX() + gVector.getGlyphMetrics(i).getBounds2D().getWidth() - translateXWidth > sz.x) {
				// Find last word
				lastWhitespaceIndex = text.lastIndexOf(' ', i);
				if (lastWhitespaceIndex > headerLength && lastWhitespaceIndex > translateXIndex) {
					// Cut new line from next word
					chatLines.add(new ChatLine(text.substring(translateXIndex, lastWhitespaceIndex), mColor, isHeader ? hName : null, hColor));
					if (isHeader) isHeader = false;
					translateXIndex = lastWhitespaceIndex;
					translateXWidth = gVector.getGlyphPosition(translateXIndex).getX() + gVector.getGlyphMetrics(translateXIndex).getBounds2D().getWidth();
				} else {
					// One big word
					chatLines.add(new ChatLine(text.substring(translateXIndex, (i - 1)), mColor, isHeader ? hName : null, hColor));
					if (isHeader) isHeader = false;
					translateXIndex = (i - 1);
					translateXWidth = gVector.getGlyphPosition(translateXIndex).getX() + gVector.getGlyphMetrics(translateXIndex).getBounds2D().getWidth();
				}
			}
		}
		if (translateXIndex < (text.length() - 1)) {
			chatLines.add(new ChatLine(text.substring(translateXIndex, text.length()), mColor, isHeader ? hName : null, hColor));
			if (isHeader) isHeader = false;
		}
	}
	
	public int getLinesCount() {
		return Math.min((int)(sz.y / chatLineBound.getHeight()) + 1, chatLines.size());
	}
	
	@Override
	public void draw(GOut initialGL) {
		super.draw(initialGL);
		
		int first = appendMode ? chatLines.size() - getLinesCount() + 1 : firstLineIndex;
		int last = Math.min(first + getLinesCount(), chatLines.size() - 1);
		
		for (int i = firstLineIndex; i <= last; i++) {
			chatLines.get(i).render(initialGL, new Coord(0, (int)((i - first) * chatLineBound.getHeight())));
		}
	}
	
	@Override
	public boolean mousewheel(Coord c, int amount) {
		if (c.isect(Coord.z, sz)) {
			if (amount > 0) {
				// Scroll down
				if (firstLineIndex <= chatLines.size() - getLinesCount()) firstLineIndex++;
				else appendMode = true;
			} else {
				// Scroll up
				appendMode = false;
				if (firstLineIndex > 0) firstLineIndex--;
			}
			return true;
		} return false;
	}
}
