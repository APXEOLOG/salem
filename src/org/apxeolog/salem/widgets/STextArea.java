package org.apxeolog.salem.widgets;

import java.util.ArrayList;

import org.apxeolog.salem.ALS;
import org.apxeolog.salem.utils.STextProcessor;
import org.apxeolog.salem.utils.STextProcessor.*;
import haven.*;

public class STextArea extends Widget {
	protected ArrayList<ProcessedText> textBuffer;
	protected int scrollAmount = 0;
	protected boolean appendMode = true;
	
	protected boolean msPressed = false;
	protected int cButton;
	protected Coord cCoord;
	
	public STextArea(Coord c, Coord sz, Widget parent) {
		super(c, sz, parent);
		textBuffer = new ArrayList<ProcessedText>();
	}
	
	public void addString(String str) {
		ProcessedText txt = STextProcessor.fromString(str);
		txt.pack(sz);
		textBuffer.add(txt);
	}
	
	public void clear() {
		textBuffer.clear();
		scrollAmount = 0;
	}
	
	public void resizeFinish() {
		for (int i = 0; i < textBuffer.size(); i++) {
			textBuffer.get(i).pack(sz);
		}
	}

	@Override
	public void draw(GOut initialGL) {
		super.draw(initialGL);
		
		if (textBuffer.isEmpty()) return;
		
		double heightBuffer = 0; 
		int firstMessage = 0, lastMessage = 0;
		double skipFirst = 0, skipLast = 0;
		boolean checkTop = true, checkBottom = true;
		
		for (int i = 0; i < textBuffer.size(); i++) {
			heightBuffer += textBuffer.get(i).getHeight();
			if (checkTop && heightBuffer > scrollAmount) {
				firstMessage = i;
				skipFirst = (heightBuffer - textBuffer.get(i).getHeight()) + scrollAmount;
				checkTop = false;
			}
			if (checkBottom && heightBuffer > scrollAmount + sz.y) {
				lastMessage = i;
				skipLast = scrollAmount + sz.y - (heightBuffer - textBuffer.get(i).getHeight());
				checkBottom = false;
			}
			if (!checkBottom && !checkTop) break;
		}
		
		Coord messageCoords = Coord.z;
		if (firstMessage == lastMessage) {
			textBuffer.get(firstMessage).drawOffsetY(initialGL, messageCoords, skipFirst, -skipLast);
		} else {
			for (int i = firstMessage; i <= lastMessage; i++) {
				if (i == firstMessage)
					textBuffer.get(i).drawOffsetY(initialGL, messageCoords, skipFirst);
				else if (i == lastMessage)
					textBuffer.get(i).drawOffsetY(initialGL, messageCoords, -skipLast);
				else 
					textBuffer.get(i).drawOffsetY(initialGL, messageCoords);
				messageCoords = messageCoords.add(0, (int)textBuffer.get(i).getHeight());
			}
		}
		if (cCoord != null) {
			double hbuf = skipFirst;
			for (int i = firstMessage; i <= lastMessage; i++) {
				hbuf += textBuffer.get(i).getHeight();
				if (cCoord.y <= hbuf) {
					Node cn = textBuffer.get(i).click(cCoord.sub(0, (int)(hbuf - textBuffer.get(i).getHeight())));
					cn.act(cButton);
					ALS.alDebugPrint(cn);
					break;
				}
			}
			cCoord = null;
		}
	}
	
	@Override
	public boolean mousedown(Coord c, int button) {
		if (button != 1) return false;
		msPressed = true;
		ui.grabmouse(this);
		return true;
	}
	
	@Override
	public boolean mouseup(Coord c, int button) {
		if (msPressed) {
			msPressed = false;
			ui.grabmouse(null);
			if (c.isect(new Coord(0, 0), sz)) {
				cCoord = new Coord(c);
				cButton = button;
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean mousewheel(Coord c, int amount) {
		if (c.isect(Coord.z, sz)) {
			scrollAmount += amount * 10;
			if (scrollAmount < 0) scrollAmount = 0;
			
			return true;
		} return false;
	}
}
