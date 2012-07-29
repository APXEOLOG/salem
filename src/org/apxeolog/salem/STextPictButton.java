package org.apxeolog.salem;

import java.awt.Color;

import haven.Coord;
import haven.GOut;
import haven.Text;
import haven.Widget;

public class STextPictButton extends SPictButton {
	protected Text text;
	
	public STextPictButton(Coord c, Coord sz, Widget parent) {
		super(c, sz, parent);
	}
	
	public void setText(String str) {
		text = SWindow.captionFoundry.render(str, Color.WHITE);
	}
	
	@Override
	public void draw(GOut g) {
		g.aimage(text.tex(), Coord.z, 0, 0);
	}
}
