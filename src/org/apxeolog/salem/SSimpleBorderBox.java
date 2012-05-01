package org.apxeolog.salem;

import haven.Coord;

public class SSimpleBorderBox {
	public int marginTop;
	public int marginLeft;
	public int marginRight;
	public int marginBottom;
	
	public int borderWidth;
	
	public int paddingTop;
	public int paddingLeft;
	public int paddingRight;
	public int paddingBottom;
	
	public Coord contentSize = Coord.z;
	
	public SSimpleBorderBox(Coord size, int margin, int padding, int border) {
		marginTop = marginLeft = marginRight = marginBottom = margin;
		paddingTop = paddingLeft = paddingRight = paddingBottom = padding;
		borderWidth = border;
		contentSize = size;
	}
	
	public Coord getBoxSize() {
		return new Coord(marginLeft + borderWidth + paddingLeft + contentSize.x + paddingRight + borderWidth + marginRight,
				marginTop + borderWidth + paddingTop + contentSize.y + paddingBottom + borderWidth + marginBottom);
	}
	
	public Coord getBorderPosition() {
		return new Coord(marginLeft, marginTop);
	}
	
	public Coord getBorderSize() {
		return new Coord(borderWidth + paddingLeft + contentSize.x + paddingRight + borderWidth,
				borderWidth + paddingTop + contentSize.y + paddingBottom + borderWidth);
	}
	
	public Coord getContentPosition() {
		return new Coord(marginLeft + borderWidth + paddingLeft, marginTop + borderWidth + paddingTop);
	}
	
	public Coord getContentSize() {
		return contentSize;
	}
}
