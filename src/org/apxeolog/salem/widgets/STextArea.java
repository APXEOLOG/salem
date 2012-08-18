package org.apxeolog.salem.widgets;

import java.util.ArrayList;
import java.util.List;

import org.apxeolog.salem.utils.STextProcessor;
import org.apxeolog.salem.utils.STextProcessor.*;
import haven.*;

public class STextArea extends Widget {
	protected ArrayList<ProcessedText> textBuffer;
	protected int scrollAmount = 0;
	protected boolean appendMode = true;

	protected boolean pressed = false;
	protected int clickButton;
	protected Coord clickCoord;

	public STextArea(Coord c, Coord sz, Widget parent) {
		super(c, sz, parent);
		textBuffer = new ArrayList<ProcessedText>();
	}

	public void addPText(ProcessedText str) {
		str.pack(sz);
		textBuffer.add(str);
		if (appendMode) scrollAmount = getMaxScrollAmount();
	}

	public void addString(String str) {
		ProcessedText txt = STextProcessor.fromString(str);
		txt.pack(sz);
		textBuffer.add(txt);
		if (appendMode) scrollAmount += txt.getHeight();
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

	// It was ~300 lines at first and it worked LOL
	@Override
	public void draw(GOut initialGL) {
		super.draw(initialGL);
		if (textBuffer.isEmpty()) return;

		double heightBuffer = 0;
		Coord nodeStart = Coord.z, nodeSize = Coord.z;
		ProcessedText buffer = null; List<Node> nodeBuffer = null; double nodeWidthBuffer = 0;
		for (int i = 0; i < textBuffer.size(); i++) {
			buffer = textBuffer.get(i);
			for (int j = 0; j < buffer.getLinesCount(); j++) {
				heightBuffer += buffer.getLineHeight(j);
				if (heightBuffer >= scrollAmount && heightBuffer <= scrollAmount + sz.y) {
					nodeWidthBuffer = 0;
					nodeBuffer = buffer.getLine(j);
					for (Node node : nodeBuffer) {
						nodeStart = new Coord((int)nodeWidthBuffer, (int)(heightBuffer - buffer.getLineHeight(j) - scrollAmount));
						node.draw(initialGL, nodeStart);
						if (clickCoord != null) {
							nodeSize = new Coord((int)node.getNodeWidth(), (int)node.getNodeHeight());
							if (clickCoord.isect(nodeStart, nodeSize)) {
								node.act(clickButton, ui.modctrl, ui.modshift);
								clickCoord = null;
							}
						}
						nodeWidthBuffer += node.getNodeWidth();
					}
				}
			}
		}
	}

	private int getMaxScrollAmount() {
		double amount = 0; double med = 1;
		for (int i = 0; i < textBuffer.size(); i++) {
			if (i == 0) med = textBuffer.get(i).getHeight();
			amount += textBuffer.get(i).getHeight();
		}
		return (int)Math.max(amount - ((int)(sz.y / med)) * med, 0);
	}

	@Override
	public boolean mousedown(Coord c, int button) {
		if (button != 1) return false;
		pressed = true;
		ui.grabmouse(this);
		return true;
	}

	@Override
	public boolean mouseup(Coord c, int button) {
		if (pressed) {
			pressed = false;
			ui.grabmouse(null);
			if (c.isect(new Coord(0, 0), sz)) {
				clickCoord = new Coord(c);
				clickButton = button;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mousewheel(Coord c, int amount) {
		if (c.isect(Coord.z, sz)) {
			scrollAmount += amount * 10;
			if (amount < 0) appendMode = false;
			if (scrollAmount < 0) scrollAmount = 0;
			if (scrollAmount > getMaxScrollAmount() + 15)  {
				scrollAmount = getMaxScrollAmount() + 15;
				appendMode = true;
			}
			return true;
		} return false;
	}
}
