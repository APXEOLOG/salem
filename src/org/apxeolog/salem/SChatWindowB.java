package org.apxeolog.salem;

import org.apxeolog.salem.widgets.STextArea;

import haven.Coord;
import haven.Text;
import haven.Widget;

public class SChatWindowB extends SWindow {
	static class SChatWindowHeader extends SWindowHeader {
		protected boolean pressed = false;
		protected Text gameChatPart;
		protected Text ircChatPart;
		
		public SChatWindowHeader(Coord c, Coord sz, Widget parent, String caption, boolean min, boolean clo) {
			super(c, sz, parent, null, min, clo);
		}
		
		public void click(Coord c) {
			ALS.alDebugPrint(c);
		}
		
		@Override
		public boolean mousedown(Coord c, int button) {
			if (button == 1 && ui.modctrl) click(c);
			return super.mousedown(c, button);
		}
	}
	
	protected STextArea gameTextArea;
	protected STextArea ircTextArea;
	
	public SChatWindowB(Coord c, Coord sz, Widget parent, String cap) {
		super(c, sz, parent, cap);
		windowHeader.unlink();
		windowHeader = new SChatWindowHeader(Coord.z, Coord.z, this, "Game Chat | IRC Chat", true, false);
		resize();
		
		gameTextArea = new STextArea(Coord.z, windowBox.getContentSize(), this);
		ircTextArea = new STextArea(Coord.z, windowBox.getContentSize(), this);
		ircTextArea.hide();
		/*STextPictButton pb = new STextPictButton(Coord.z, new Coord(50, 0), windowHeader);
		pb.setText("IRC Chat");
		windowHeader.addPictControl(pb);*/
		setResizable(true);
	}
	
	@Override
	public void resize(Coord newSize) {
		super.resize(newSize);
		if (gameTextArea != null)
			gameTextArea.resize(windowBox.getContentSize());
		if (ircTextArea != null)
			ircTextArea.resize(windowBox.getContentSize());
	}
	
	@Override
	public void resizeFinish() {
		if (gameTextArea != null)
			gameTextArea.resizeFinish();
		if (ircTextArea != null)
			ircTextArea.resizeFinish();
	}
	
	public void addString(String str) {
		gameTextArea.addString(str);
	}
}
