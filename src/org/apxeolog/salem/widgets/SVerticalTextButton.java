package org.apxeolog.salem.widgets;

import java.awt.Color;
import java.awt.Font;

import haven.Coord;
import haven.GOut;
import haven.Text;
import haven.Text.Foundry;
import haven.Widget;

public class SVerticalTextButton extends Widget {
	private static final Text.Foundry foundry = new Foundry(new Font("Serif", Font.PLAIN, 14));

	protected boolean pressed = false;
	protected Text rendered = null;

	public SVerticalTextButton(Coord c, Coord sz, Widget parent, String title) {
		super(c, sz, parent);
		rendered = foundry.renderRotated(title, Color.WHITE);
		resize(rendered.sz().add(2, 8));
	}

	public void click() {

	}

	@Override
	public void draw(GOut initialGL) {
		initialGL.chcolor(0, 0, 0, 128);
		initialGL.frect(Coord.z, sz);
		initialGL.chcolor(255, 255, 255, 255);
		initialGL.rect(Coord.z, sz);
		initialGL.aimage(rendered.tex(), sz.div(2), 0.5, 0.5);
	}

	@Override
	public boolean mousedown(Coord c, int button) {
		if (button != 1)
			return (false);
		pressed = true;
		ui.grabmouse(this);
		return (true);
	}

	@Override
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
}
