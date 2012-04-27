package org.apxeolog.salem;

import haven.Coord;
import haven.GOut;
import haven.IButton;
import haven.Resource;
import haven.Text;
import haven.Widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;

public class SWindow extends Widget {
	protected SSimpleBorderBox captionBox = null;
	protected SSimpleBorderBox windowBox = null;
	protected IButton btnClose = null;
	
	
	protected static BufferedImage[] closeBtnImages = new BufferedImage[] {
			Resource.loadimg("apx/hud/cbtn"),
			Resource.loadimg("apx/hud/cbtn"),
			Resource.loadimg("apx/hud/cbtn") };
	
	static Text.Foundry cf = new Text.Foundry(new Font("Serif", Font.PLAIN, 12));
	
	protected boolean dropTarget = false;
	protected Text caption = null;
	protected boolean dragMode = false;
	protected Coord doff = Coord.z;

	public SWindow(Coord c, Coord sz, Widget parent, String cap, boolean closeable) {
		super(c, new Coord(0, 0), parent);
		if (cap != null) {
			caption = cf.render(cap, Color.WHITE);
			captionBox = new SSimpleBorderBox(caption.sz(), 0, 1, 1);
			captionBox.paddingTop = 0;
			captionBox.paddingLeft = captionBox.paddingRight = 6;
		}
		setClosable(closeable);
		windowBox = new SSimpleBorderBox(sz, 0, 2, 1);
		windowBox.marginTop = windowBox.marginRight = 10;
		resize(sz);
		setfocustab(true);
		parent.setfocus(this);
	}

	public SWindow(Coord c, Coord sz, Widget parent, String cap) {
		this(c, sz, parent, cap, true);
	}
    
    public void setClosable(boolean closeable) {
    	if (closeable) {
    		if (btnClose == null) btnClose = new IButton(Coord.z, this, closeBtnImages[0], closeBtnImages[1], closeBtnImages[2]);
    	} else {
    		if (btnClose != null) btnClose.unlink();
    	}
    }
    
	public void resize(Coord sz) {
		windowBox.contentSize = sz;
		this.sz = windowBox.getBoxSize();
		
		if (btnClose != null) {
			Coord calced = windowBox.getBorderPosition();
			calced.x += windowBox.getBorderSize().x;
			btnClose.c = xlate(calced.sub(btnClose.sz.div(2)).sub(3, 0), false);
		}

		for (Widget ch = child; ch != null; ch = ch.next)
			ch.presize();
	}
    
    public void setCaption(String text) {
    	if (text != null) {
			caption = cf.render(text, Color.WHITE);
			captionBox.contentSize = caption.sz();
		} else {
			caption = null;
			captionBox = null;
		}
    }
    
	public void draw(GOut initialGL) {
		initialGL.chcolor(0, 0, 0, 128);
		initialGL.frect(windowBox.getBorderPosition(), windowBox.getBorderSize());
		if (windowBox.borderWidth != 0) {
			initialGL.chcolor(255, 255, 255, 255);
			initialGL.rect(windowBox.getBorderPosition(), windowBox.getBorderSize());
		}

		if (caption != null) {
			Coord drawPoint = sz.div(2).sub(captionBox.getBoxSize().div(2)); drawPoint.y = -windowBox.getBorderPosition().y + (captionBox.getBoxSize().y / 2);
			initialGL.chcolor(0, 0, 0, 255);
			initialGL.frect(drawPoint.add(captionBox.getBorderPosition()), captionBox.getBorderSize());
			if (captionBox.borderWidth != 0) {
				initialGL.chcolor(255, 255, 255, 255);
				initialGL.rect(drawPoint.add(captionBox.getBorderPosition()), captionBox.getBorderSize());
			}
			initialGL.image(caption.img, drawPoint.add(captionBox.getContentPosition()));
		}
		super.draw(initialGL);
	}

	public void pack() {
		Coord max = new Coord(0, 0);
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg == btnClose)
				continue;
			Coord br = wdg.c.add(wdg.sz);
			if (br.x > max.x)
				max.x = br.x;
			if (br.y > max.y)
				max.y = br.y;
		}
		resize(max.sub(1, 1));
	}



	public void uimsg(String msg, Object... args) {
		if (msg == "pack") {
			pack();
		} else if (msg == "dt") {
			dropTarget = (Integer) args[0] != 0;
		} else {
			super.uimsg(msg, args);
		}
	}

	public Coord xlate(Coord c, boolean in) {
		if (in) return c.add(windowBox.getContentPosition());
		else return c.sub(windowBox.getContentPosition());
	}

	public boolean mousedown(Coord c, int button) {
		parent.setfocus(this);
		raise();
		if (super.mousedown(c, button))
			return (true);
		
		if (!c.isect(windowBox.getBorderPosition(), windowBox.getBorderSize()))
			return (false);
		
		if (button == 1) {
			ui.grabmouse(this);
			dragMode = true;
			doff = c;
		}
		return (true);
	}

	public boolean mouseup(Coord c, int button) {
		if (dragMode) {
			ui.grabmouse(null);
			dragMode = false;
		} else {
			super.mouseup(c, button);
		}
		return (true);
	}

	public void mousemove(Coord c) {
		if (dragMode) {
			this.c = this.c.add(c.add(doff.inv()));
		} else {
			super.mousemove(c);
		}
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (sender == btnClose) {
			if (ui.isRWidget(this)) wdgmsg("close");
			else unlink();
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	public boolean type(char key, java.awt.event.KeyEvent ev) {
		if (key == 27) {
			if (ui.isRWidget(this)) wdgmsg("close");
			else unlink();
			
			return (true);
		}
		return (super.type(key, ev));
	}

	public boolean drop(Coord cc, Coord ul) {
		if (dropTarget) {
			wdgmsg("drop", cc);
			return (true);
		}
		return (false);
	}

	public boolean iteminteract(Coord cc, Coord ul) {
		return (false);
	}

	public Object tooltip(Coord c, boolean again) {
		Object ret = super.tooltip(c, again);
		if (ret != null)
			return (ret);
		else
			return ("");
	}

}
