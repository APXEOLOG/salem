package org.apxeolog.salem;

import haven.Coord;
import haven.GOut;
import haven.RichText;
import haven.Text;
import haven.Widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class SChat extends Widget {
	public final static Font chatFont = new Font("Serif", Font.BOLD, 14);
	public final static FontRenderContext chatFontContext = new FontRenderContext(null, true, true);
	public final static Rectangle2D chatLineBound = chatFont.getStringBounds("TEST", chatFontContext);
	public final static Text.Foundry textFoundry = new Text.Foundry(chatFont);
	public final static RichText.Foundry richTextFoundry = new RichText.Foundry(TextAttribute.FONT, chatFont);
	
	public final static Text villageHeader = textFoundry.render("[Village]: ", Color.GREEN);
	public final static Text areaHeader = textFoundry.render("[Area]: ", Color.WHITE);
	public final static Text partyHeader = textFoundry.render("[Party]: ", Color.CYAN);
	
	protected static class Header {
		public static final String headerMask = "[%s]: ";
		public static final String headerRichMask = "[$col[%d,%d,%d]{%s}]: ";
		
		public String pureName;
		public Text cachedHeader;
		public WeakReference<Widget> linkedChat;
		public Color headerColor;
		
		public Header(String pure, Color hColor, WeakReference<Widget> wdgRef) {
			pureName = pure;
			headerColor = hColor;
			linkedChat = wdgRef;
			cachedHeader = richTextFoundry.render(String.format(headerRichMask, 
							headerColor.getRed(), headerColor.getGreen(), headerColor.getBlue(), pureName));
		}
		
		public int getSizeX() {
			return cachedHeader.sz().x;
		}
		
		public String getFullHeader() {
			return String.format(headerMask, pureName);
		}
	}
	
	protected static class ChatLine {
		public Text cachedLine = null;
		public Header lineHeader = null;
		
		public ChatLine(String text, Color tColor, Header header) {
			lineHeader = header;
			if (lineHeader != null) {
				cachedLine = textFoundry.render(text.substring(lineHeader.getFullHeader().length()), tColor);
			} else {
				cachedLine = textFoundry.render(text, tColor);
			}
		}
		
		public Header getHeader() {
			return lineHeader;
		}
		
		public void render(GOut g, Coord c) {
			if (lineHeader != null) {
				g.image(lineHeader.cachedHeader.img, c);
				g.image(cachedLine.img, c.add(lineHeader.cachedHeader.sz().x, 0));
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
	
	public void addMessage(String msg, Color mColor, String hName, Color hColor, Widget from) {
		// Header
		Header msgHeader = new Header(hName, hColor, new WeakReference<Widget>(from));
		// Add chat header
		String text = msgHeader.getFullHeader() + msg;
		int headerLength = msgHeader.getFullHeader().length();
		boolean isHeader = true;
		// Create glyph vector
		GlyphVector gVector = chatFont.layoutGlyphVector(chatFontContext, text.toCharArray(), 0, text.length(), 0);
		int translateXIndex = 0; double translateXWidth = 0; int lastWhitespaceIndex = -1;
		for (int i = 0; i < gVector.getNumGlyphs(); i++) {
			// Detect line bound
			if (gVector.getGlyphPosition(i).getX() + gVector.getGlyphMetrics(i).getBounds2D().getWidth() - translateXWidth > (sz.x - 5)) {
				// Find last word
				lastWhitespaceIndex = text.lastIndexOf(' ', i);
				if (lastWhitespaceIndex > headerLength && lastWhitespaceIndex > translateXIndex) {
					// Cut new line from next word
					chatLines.add(new ChatLine(text.substring(translateXIndex, lastWhitespaceIndex), mColor, isHeader ? msgHeader : null));
					if (isHeader) isHeader = false;
					translateXIndex = lastWhitespaceIndex;
					translateXWidth = gVector.getGlyphPosition(translateXIndex).getX() + gVector.getGlyphMetrics(translateXIndex).getBounds2D().getWidth();
				} else {
					// One big word
					chatLines.add(new ChatLine(text.substring(translateXIndex, (i - 1)), mColor, isHeader ? msgHeader : null));
					if (isHeader) isHeader = false;
					translateXIndex = (i - 1);
					translateXWidth = gVector.getGlyphPosition(translateXIndex).getX() + gVector.getGlyphMetrics(translateXIndex).getBounds2D().getWidth();
				}
			}
		}
		if (translateXIndex < (text.length() - 1)) {
			chatLines.add(new ChatLine(text.substring(translateXIndex, text.length()), mColor, isHeader ? msgHeader : null));
			if (isHeader) isHeader = false;
		}
	}
	
	public int getLinesCount() {
		return Math.min((int)(sz.y / chatLineBound.getHeight()) + 1, chatLines.size());
	}
	
	@Override
	public void draw(GOut initialGL) {
		super.draw(initialGL);
		
		int first = appendMode ? chatLines.size() - getLinesCount() : firstLineIndex;
		int last = Math.min(first + getLinesCount(), chatLines.size() - 1);
		
		for (int i = firstLineIndex; i <= last; i++) {
			chatLines.get(i).render(initialGL, new Coord(0, (int)((i - first) * chatLineBound.getHeight())));
		}
	}
	
	protected boolean pressed = false;
	
	@Override
	public boolean mousedown(Coord c, int button) {
		if (button != 1) return false;
		pressed = true;
		ui.grabmouse(this);
		return true;
	}
	
	@Override
	public boolean mouseup(Coord c, int button) {
		if (pressed && button == 1) {
			pressed = false;
			ui.grabmouse(null);
			if (c.isect(new Coord(0, 0), sz))
				return click(c);
		}
		return false;
	}
	
	public boolean click(Coord c) {
		int index = appendMode ? chatLines.size() - getLinesCount() + 1 : firstLineIndex + (int)((double)c.y / (double)chatLineBound.getHeight());
		if (index < chatLines.size()) {
			if (c.x <= chatLines.get(index).getHeader().getSizeX()) {
				Header selected = chatLines.get(index).getHeader();
				if (ui.modctrl)
					((SChatWindow)parent).setLinkedMode(selected.pureName, selected.cachedHeader, selected.linkedChat, true);
				else
					((SChatWindow)parent).setLinkedMode(selected.pureName, selected.cachedHeader, selected.linkedChat, false);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean mousewheel(Coord c, int amount) {
		if (c.isect(Coord.z, sz)) {
			if (amount > 0) {
				// Scroll down
				if (firstLineIndex <= chatLines.size() - getLinesCount() - 1) firstLineIndex++;
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
