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
	protected boolean selected = false;
	protected boolean notify = false;
	protected Object data;

	public SVerticalTextButton(Coord c, Coord sz, Widget parent, String title) {
		super(c, sz, parent);
		rendered = foundry.renderRotated(title, Color.WHITE);
		resize(rendered.sz().add(2, 8));
	}

	public void setData(Object dat) {
		data = dat;
	}

	public Object getData() {
		return data;
	}

	public void click() {

	}

	public void select() {
		selected = true;
		notify = false;
	}

	public void unselect() {
		selected = false;
	}

	public void setnotify() {
		if (!selected)
			notify = true;
	}

	@Override
	public void draw(GOut initialGL) {
		if (selected)
			initialGL.chcolor(100, 100, 100, 128);
		else
			initialGL.chcolor(0, 0, 0, 128);
		if (notify)
			initialGL.chcolor(255, 255, 100, 128);
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
			if (c.isect(new Coord(0, 0), sz)) {
				select();
				click();
			}
			return (true);
		}
		return (false);
	}
}
