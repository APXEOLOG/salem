package org.apxeolog.salem;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;

import haven.Coord;
import haven.GOut;
import haven.Text;
import haven.Widget;

public class SLineEdit extends Widget {
	protected Text.Foundry renderFoundry = null;
	protected FontRenderContext renderContext = null;
	protected StringBuilder textBuilder = null;
	
	protected int pointIndexAfter = 0;
	protected Coord textSelection = null;
	protected Text textCache = null;
	
	protected int renderStartPosition = 0;
	protected boolean needUpdate = true;
	
	public SLineEdit(Coord c, Coord sz, Widget parent, String text, Text.Foundry foundry, FontRenderContext context) {
		super(c, sz, parent);
		renderFoundry = foundry;
		renderContext = context;
		textBuilder = new StringBuilder();
		textBuilder.append(text);
		pointIndexAfter = textBuilder.length();
	}
	
	protected int getLastVisibleCharIndex(int startIndex) {
		if (textBuilder.length() == 0) return textBuilder.length();
		GlyphVector gVector = renderFoundry.getFont().layoutGlyphVector(renderContext, textBuilder.toString().toCharArray(), 0, textBuilder.length(), 0);
		//if (startIndex >= gVector.getNumGlyphs()) return (gVector.getNumGlyphs());
		double translateX = gVector.getGlyphPosition(startIndex).getX();
		for (int i = startIndex; i < gVector.getNumGlyphs(); i++) {
			if (gVector.getGlyphPosition(i).getX() - translateX > sz.x) return (i - 1);
		}
		return gVector.getNumGlyphs();
	}
	
	protected int getPointerPosition(int startIndex, int pointerIndex) {
		if (pointerIndex == 0) return pointerIndex;
		GlyphVector gVector = renderFoundry.getFont().layoutGlyphVector(renderContext, textBuilder.toString().toCharArray(), 0, textBuilder.length(), 0);
		double translateX = gVector.getGlyphPosition(startIndex).getX();
		return (int)(gVector.getGlyphPosition(pointerIndex - 1).getX() + gVector.getGlyphMetrics(pointerIndex - 1).getBounds2D().getWidth() - translateX);
	}
	
	@Override
	public boolean keydown(KeyEvent ev) {
		// Lets do some magic now!
		boolean ctrl = ev.isControlDown();
		boolean alt = ev.isAltDown() || ev.isMetaDown();
		boolean shift = ev.isShiftDown();
		// Point movement
		if (ev.getKeyCode() == KeyEvent.VK_LEFT) {
			if (pointIndexAfter > 0) {
				pointIndexAfter--;
				if (pointIndexAfter < renderStartPosition) {
					renderStartPosition--;
					needUpdate = true;
				}
				if (shift) {
					// Text selection
					if (textSelection == null) {
						textSelection = new Coord(pointIndexAfter, pointIndexAfter + 1);
					} else {
						textSelection = textSelection.sub(1, 0);
					}
				} else textSelection = null;
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_RIGHT) {
			if (pointIndexAfter < (textBuilder.length() - 1)) {
				pointIndexAfter++;
				if (pointIndexAfter > getLastVisibleCharIndex(renderStartPosition)) {
					renderStartPosition++;
					needUpdate = true;
				}
				if (shift) {
					// Text selection
					if (textSelection == null) {
						textSelection = new Coord(pointIndexAfter - 1, pointIndexAfter);
					} else {
						textSelection = textSelection.sub(0, 1);
					}
				} else textSelection = null;
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
			if (pointIndexAfter > 0) {
				if (textBuilder.length() > 0) {
					pointIndexAfter--;
					textBuilder.deleteCharAt(pointIndexAfter);
					needUpdate = true;
				}
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_DELETE) {
			if (pointIndexAfter > 0) {
				if (textBuilder.length() > pointIndexAfter) {
					textBuilder.deleteCharAt(pointIndexAfter);
					needUpdate = true;
				}
			}
		} else if (Character.isLetter(ev.getKeyChar()) || Character.isDigit(ev.getKeyChar())) {
			if (textBuilder.length() <= pointIndexAfter) textBuilder.append(ev.getKeyChar());
			else textBuilder.insert(pointIndexAfter, ev.getKeyChar());
			pointIndexAfter++;
			if (pointIndexAfter > getLastVisibleCharIndex(renderStartPosition)) {
				renderStartPosition++;
			}
			needUpdate = true;
		} else {
			return false;
		}
		return true;
	}
	
	@Override
	public void draw(GOut g) {
		super.draw(g);
		if (needUpdate) {
			ALS.alDebugPrint("render", textBuilder.substring(renderStartPosition, getLastVisibleCharIndex(renderStartPosition)));
			textCache = renderFoundry.render(textBuilder.substring(renderStartPosition, getLastVisibleCharIndex(renderStartPosition)), Color.WHITE);
			needUpdate = false;
		}
		g.chcolor(0, 0, 0, 128);
		g.frect(Coord.z, sz);
		g.chcolor(Color.WHITE);
		g.image(textCache.img, Coord.z);
		int x = getPointerPosition(renderStartPosition, pointIndexAfter);
		g.line(new Coord(x, 0), new Coord(x, 16), 1);
	}
	
	public boolean mousedown(Coord c, int button) {
		parent.setfocus(this);
		// if(tcache != null) {
		// buf.point = tcache.charat(c.x + sx);
		// }
		return (true);
	}
	
//	public boolean key(KeyEvent ev) {
//		int mod = 0;
//		if ((ev.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)
//			mod |= C;
//		if ((ev.getModifiersEx() & (InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0)
//			mod |= M;
//		if (ev.getID() == KeyEvent.KEY_TYPED) {
//			char c = ev.getKeyChar();
//			if (((mod & C) != 0) && (c < 32)) {
//				/* Undo Java's TTY Control-code mangling */
//				if (ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
//				} else if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
//				} else if (ev.getKeyCode() == KeyEvent.VK_TAB) {
//				} else if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
//				} else {
//					if ((ev.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0)
//						c = (char) (c + 'A' - 1);
//					else
//						c = (char) (c + 'a' - 1);
//				}
//			}
//			return key(c, ev.getKeyCode(), mod);
//		} else if (ev.getID() == KeyEvent.KEY_PRESSED) {
//			if (ev.getKeyChar() == KeyEvent.CHAR_UNDEFINED)
//				return (key('\0', ev.getKeyCode(), mod));
//		}
//		return (false);
//	}
//	
//	public boolean key(char c, int code, int mod) {
//		if ((c == 8) && (mod == 0)) {
//			if (point > 0) {
//				line = line.substring(0, point - 1) + line.substring(point);
//				point--;
//			}
//		} else if ((c == 8) && (mod == C)) {
//			int b = wordstart(point);
//			line = line.substring(0, b) + line.substring(point);
//			point = b;
//		} else if (c == 10) {
//			done(line);
//		} else if ((c == 127) && (mod == 0)) {
//			if (point < line.length())
//				line = line.substring(0, point) + line.substring(point + 1);
//		} else if ((c == 127) && (mod == C)) {
//			int b = wordend(point);
//			line = line.substring(0, point) + line.substring(b);
//		} else if ((c >= 32) && (mod == 0)) {
//			line = line.substring(0, point) + c + line.substring(point);
//			point++;
//		} else if ((code == KeyEvent.VK_LEFT) && (mod == 0)) {
//			if (point > 0)
//				point--;
//		} else if ((code == KeyEvent.VK_LEFT) && (mod == C)) {
//			point = wordstart(point);
//		} else if ((code == KeyEvent.VK_RIGHT) && (mod == 0)) {
//			if (point < line.length())
//				point++;
//		} else if ((code == KeyEvent.VK_RIGHT) && (mod == C)) {
//			point = wordend(point);
//		} else if ((code == KeyEvent.VK_HOME) && (mod == 0)) {
//			point = 0;
//		} else if ((code == KeyEvent.VK_END) && (mod == 0)) {
//			point = line.length();
//		} else {
//			return (false);
//		}
//		return (true);
//	}
}
