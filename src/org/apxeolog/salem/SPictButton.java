package org.apxeolog.salem;

import haven.Coord;
import haven.GOut;
import haven.Widget;

public class SPictButton extends Widget {

	public SPictButton(Coord c, Coord sz, Widget parent) {
		super(c, sz, parent);
	}

	protected boolean pressed = false;

	public void click() {
		wdgmsg("activate");
	}

	public boolean mousedown(Coord c, int button) {
		if (button != 1)
			return (false);
		pressed = true;
		ui.grabmouse(this);
		return (true);
	}

	public boolean mouseup(Coord c, int button) {
		if (pressed && button == 1) {
			pressed = false;
			ui.grabmouse(null);
			if (c.isect(new Coord(0, 0), sz))
				click();
			return (true);
		}
		return (false);
	}

	public void render(GOut g) {
		
	}
}
