package org.apxeolog.salem;

import haven.Coord;
import haven.GOut;
import haven.Text;
import haven.Widget;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class SLineEdit extends Widget implements ClipboardOwner {
	public static final int POINTER_RENDER_TIME = 800;
	protected Text.Foundry renderFoundry = null;
	protected StringBuilder textBuilder = null;

	protected int pointIndexAfter = 0;
	protected Coord textSelection = null;
	protected Text textCache = null;

	protected int renderStartPosition = 0;
	protected boolean needUpdate = true;
	protected long lastPointerRenderTime = 0;
	protected boolean renderPointer = true;

	protected Text defHeader = null;
	protected Color defColor = Color.WHITE;

	public SLineEdit(Coord c, Coord sz, Widget parent, String text, Text.Foundry foundry) {
		super(c, sz, parent);
		renderFoundry = foundry;
		textBuilder = new StringBuilder();
		textBuilder.append(text);
		pointIndexAfter = textBuilder.length();
	}

	protected int getStartRenderIndex() {
		if (textBuilder.length() == 0) return textBuilder.length();
		Coord shareX = new Coord(defHeader != null ? defHeader.sz().x : 0, 0);
		int wlen = 0;
		for (int i = (textBuilder.length() - 1); i >= 0; i--) {
			wlen += renderFoundry.getFontMetrics().charWidth(textBuilder.charAt(i));
			if (wlen >= (sz.x - 5 - shareX.x)) return i;
		}
		return 0;
	}

	protected int getPointerPosition(int startIndex, int pointerIndex) {
		Coord shareX = new Coord(defHeader != null ? defHeader.sz().x : 0, 0);
		if (pointerIndex == 0) return shareX.x;
		return shareX.x + renderFoundry.getFontMetrics().charsWidth(textBuilder.toString().toCharArray(), startIndex, (pointerIndex - startIndex));
	}

	@Override
	public boolean keydown(KeyEvent ev) {
		needUpdate = true;
		// Lets do some magic now!
		boolean ctrl = ev.isControlDown();
		//boolean alt = ev.isAltDown() || ev.isMetaDown();
		boolean shift = ev.isShiftDown();
		// Point movement
		if (ev.getKeyCode() == KeyEvent.VK_LEFT) {
			if (pointIndexAfter > 0) {
				pointIndexAfter--;
				if (shift) {
					// Text selection
					if (textSelection == null) {
						textSelection = new Coord(pointIndexAfter, pointIndexAfter + 1);
					} else {
						if (textSelection.x > 0)
							textSelection = textSelection.sub(1, 0);
					}
				} else textSelection = null;
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_RIGHT) {
			if (pointIndexAfter < (textBuilder.length())) {
				pointIndexAfter++;
				if (shift) {
					// Text selection
					if (textSelection == null) {
						textSelection = new Coord(pointIndexAfter - 1, pointIndexAfter);
					} else {
						if (textSelection.y < textBuilder.length())
							textSelection = textSelection.add(0, 1);
					}
				} else textSelection = null;
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
			if (pointIndexAfter > 0) {
				if (textBuilder.length() > 0) {
					pointIndexAfter--;
					textBuilder.deleteCharAt(pointIndexAfter);
					textSelection = null;
				}
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_DELETE) {
			if (pointIndexAfter > 0) {
				if (textBuilder.length() > pointIndexAfter) {
					textBuilder.deleteCharAt(pointIndexAfter);
					textSelection = null;
				}
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_C && ctrl) {
			if (textSelection != null) {
				if (textSelection.x > 0 && textSelection.y < textBuilder.length()) {
					String data = textBuilder.substring(textSelection.x, textSelection.y);
					setClipboardContents(data);
				}
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_V && ctrl) {
			String data = getClipboardContents();
			if (textBuilder.length() <= pointIndexAfter) textBuilder.append(data);
			else textBuilder.insert(pointIndexAfter, data);
			pointIndexAfter+= data.length();
			textSelection = null;
		} else if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
			wdgmsg("sle_activate", getText(), targetWdgId);
		} else if (Character.isDefined(ev.getKeyChar())) {
			if (textBuilder.length() <= pointIndexAfter) textBuilder.append(ev.getKeyChar());
			else textBuilder.insert(pointIndexAfter, ev.getKeyChar());
			pointIndexAfter++;
			textSelection = null;
		} else {
			needUpdate = false;
			return false;
		}
		return true;
	}

	public void clear() {
		textBuilder.delete(0, textBuilder.length());
		needUpdate = true;
		renderStartPosition = 0;
		pointIndexAfter = 0;
		textSelection = null;
	}

	public String getText() {
		return textBuilder.toString();
	}

	public void setupLine(Text header, Color lineCol) {
		defColor = lineCol;
		defHeader = header;
	}

	@Override
	public void draw(GOut g) {
		super.draw(g);

		Coord shareX = new Coord(defHeader != null ? defHeader.sz().x : 0, 0);
		int startPos = getStartRenderIndex();
		int lineHeight = renderFoundry.getFontMetrics().getHeight();
		if (needUpdate) {
			textCache = renderFoundry.render(textBuilder.substring(startPos), defColor);
			needUpdate = false;
		}
		if (textSelection != null) {
			int left = Math.max(textSelection.x - startPos, 0);
			int right = Math.min(textSelection.y - startPos, textBuilder.length() - startPos);
			g.chcolor(Color.GRAY);
			g.frect(new Coord(getPointerPosition(startPos, left), 0), new Coord(getPointerPosition(left, right) - shareX.x, lineHeight));
		}

		g.chcolor(0, 0, 0, 128);
		g.frect(Coord.z, sz);
		g.chcolor(255, 255, 255, 255);
		g.rect(Coord.z, sz.add(1, 1));
		g.chcolor(Color.WHITE);

		if (defHeader != null) g.image(defHeader.img, Coord.z);

		g.image(textCache.img, shareX);

		if (hasfocus && renderPointer) {
			int x = getPointerPosition(startPos, pointIndexAfter);
			g.line(new Coord(x, 0), new Coord(x, lineHeight), 1);
		}

		if (hasfocus && System.currentTimeMillis() - lastPointerRenderTime > POINTER_RENDER_TIME) {
			lastPointerRenderTime = System.currentTimeMillis();
			renderPointer = !renderPointer;
		}
	}

	@Override
	public boolean mousedown(Coord c, int button) {
		parent.setfocus(this);

		int startPos = getStartRenderIndex();
		if (textBuilder.length() > 0) {
			int wlen = 0;
			for (int i = startPos; i < textBuilder.length(); i++) {
				wlen += renderFoundry.getFontMetrics().charWidth(textBuilder.charAt(i));
				if (wlen >= c.x) {
					pointIndexAfter = i;
					return true;
				}
			}
			pointIndexAfter = textBuilder.length();
		}
		return true;
	}

	protected int targetWdgId = -1;

	public void show(int wdgId) {
		show();
		targetWdgId = wdgId;
	}

	/**
	 * Place a String on the clipboard, and make this class the owner of the
	 * Clipboard's contents.
	 */
	public void setClipboardContents(String aString) {
		StringSelection stringSelection = new StringSelection(aString);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, this);
	}

	/**
	 * Get the String residing on the clipboard.
	 * 
	 * @return any text found on the Clipboard; if none found, return an empty
	 *         String.
	 */
	public String getClipboardContents() {
		String result = "";
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		// odd: the Object param of getContents is not currently used
		Transferable contents = clipboard.getContents(null);
		boolean hasTransferableText = (contents != null)
				&& contents.isDataFlavorSupported(DataFlavor.stringFlavor);
		if (hasTransferableText) {
			try {
				result = (String) contents.getTransferData(DataFlavor.stringFlavor);
			} catch (UnsupportedFlavorException ex) {
				// highly unlikely since we are using a standard DataFlavor
			} catch (IOException ex) {
			}
		}
		return result;
	}

	/**
	 * Empty implementation of the ClipboardOwner interface.
	 */
	@Override
	public void lostOwnership(Clipboard aClipboard, Transferable aContents) {
		// do nothing
	}
}
