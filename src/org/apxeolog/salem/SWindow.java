package org.apxeolog.salem;

import haven.Coord;
import haven.GOut;
import haven.Resource;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.Widget;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;

public class SWindow extends Widget {
	protected static Text.Foundry captionFoundry = new Text.Foundry(new Font("Serif", Font.PLAIN, 12));
	
	protected static class SPictButtonClose extends SPictButton {
		protected Tex btnImage = new TexI(Resource.loadimg("apx/gfx/hud/close-button"));
		
		public SPictButtonClose(Coord c, Coord sz, Widget parent) {
			super(c, sz, parent);
		}
		
		@Override
		public void draw(GOut g) {
			g.chcolor(0, 0, 0, 255);
			g.frect(Coord.z, sz);
			g.chcolor();
			g.image(btnImage, new Coord(2, 2));
			g.chcolor(255, 255, 255, 255);
			g.rect(Coord.z, sz.add(1, 1));
			super.draw(g);
		}
		
		@Override
		public void click() {
			wdgmsg("swindow_close");
		}
	}
	
	protected static class SPictButtonMinimize extends SPictButton {
		protected Tex btnImageMin = new TexI(Resource.loadimg("apx/gfx/hud/minimize-button"));
		protected Tex btnImageMax = new TexI(Resource.loadimg("apx/gfx/hud/maximize-button"));
		protected boolean stateNormal = true;
		
		public SPictButtonMinimize(Coord c, Coord sz, Widget parent) {
			super(c, sz, parent);
		}
		
		@Override
		public void draw(GOut g) {
			g.chcolor(0, 0, 0, 255);
			g.frect(Coord.z, sz);
			g.chcolor();
			if (stateNormal) g.image(btnImageMin, new Coord(2, 2));
			else g.image(btnImageMax, new Coord(2, 2));
			g.chcolor(255, 255, 255, 255);
			g.rect(Coord.z, sz.add(1, 1));
			super.draw(g);
		}
		
		@Override
		public void click() {
			if (stateNormal) {
				wdgmsg("swindow_minimize");
			} else {
				wdgmsg("swindow_maximize");
			}
			stateNormal = !stateNormal;
		}
	}
	
	protected static class SWindowHeader extends Widget {
		private static final Coord minimalHeaderSize = new Coord(18, 18);
		
		protected SSimpleBorderBox headerBox = null;
		protected Text headerText = null;
		protected SPictButton btnClose = null;
		protected SPictButton btnMinimize = null;
		protected ArrayList<SPictButton> buttons = null;
		
		public SWindowHeader(Coord c, Coord sz, Widget parent, String caption, boolean min, boolean clo) {
			super(c, sz, parent);
			headerBox = new SSimpleBorderBox(Coord.z, 0, 0, 1);
			buttons = new ArrayList<SPictButton>();
			pSetText(caption);
			pSetClosable(clo);
			pSetMinimazable(min);
			resize();
		}
		
		public void setText(String text) {
			pSetText(text);
			resize();
		}
		
		public void setClosable(boolean closable) {
			pSetClosable(closable);
			resize();
		}
		
		public void setMinimazable(boolean minimazable) {
			pSetMinimazable(minimazable);
			resize();
		}
		
		private void pSetClosable(boolean closable) {
			if (closable) {
				if (btnClose == null) btnClose = new SPictButtonClose(Coord.z, Coord.z, this);
			} else {
				if (btnClose != null) btnClose.unlink();
				btnClose = null;
			}
		}
		
		private void pSetMinimazable(boolean minimazable) {
			if (minimazable) {
				if (btnMinimize == null) btnMinimize = new SPictButtonMinimize(Coord.z, Coord.z, this);
			} else {
				if (btnMinimize != null) btnMinimize.unlink();
				btnMinimize = null;
			}
		}

		private void pSetText(String text) {
			if (text != null) {
				headerText = captionFoundry.render(text, Color.WHITE);
			} else {
				headerText = null;
			}
		}
		
		protected Coord textSize() {
			if (headerText != null) return headerText.sz().add(10, 0);
			else return minimalHeaderSize;
		}
		
		public void addPictControl(SPictButton btn) {
			buttons.add(btn);
			resize();
			parent.resize(parent.sz);
		}
		
		public void removePictControl(SPictButton btn) {
			buttons.remove(btn);
			resize();
			parent.resize(parent.sz);
		}
		
		protected void resize() {
			Coord contSize = Coord.z;
			
			contSize = contSize.add(textSize());
			if (btnMinimize != null) {
				btnMinimize.sz = new Coord(contSize.y, contSize.y);
				btnMinimize.c = new Coord(contSize.x, 0);
				contSize = contSize.add(btnMinimize.sz.x, 0);
			}
			
			if (btnClose != null) {
				btnClose.sz = new Coord(contSize.y, contSize.y);
				btnClose.c = new Coord(contSize.x, 0);
				contSize = contSize.add(btnClose.sz.x, 0);
			}
			
			for (SPictButton btn : buttons) {
				btn.sz = new Coord(contSize.y, contSize.y);
				btn.c = new Coord(contSize.x, 0);
				contSize = contSize.add(btn.sz.x, 0);
			}
			
			headerBox.contentSize = contSize;
			sz = headerBox.getBoxSize();
		}
		
		@Override
		public void draw(GOut initialGL) {
			initialGL.chcolor(0, 0, 0, 255);
			initialGL.frect(headerBox.getBorderPosition(), textSize());
			super.draw(initialGL);
			if (headerBox.borderWidth != 0) {
				initialGL.chcolor(255, 255, 255, 255);
				initialGL.rect(headerBox.getBorderPosition(), textSize().add(1, 1));
			}
			if (headerText != null) {
				initialGL.image(headerText.img, headerBox.getContentPosition().add(4, -1));
			}
		}
		
		protected boolean dragMode = false;
		protected Coord doff = Coord.z;
		
		public boolean mousedown(Coord c, int button) {
			parent.setfocus(this);
			raise();

			if (super.mousedown(c, button))
				return true;

			if (button == 1) {
				ui.grabmouse(this);
				dragMode = true;
				doff = c;
			}
			return true;
		}

		public boolean mouseup(Coord c, int button) {
			if (dragMode) {
				ui.grabmouse(null);
				dragMode = false;
			} else {
				super.mouseup(c, button);
			}
			return true;
		}

		public void mousemove(Coord c) {
			if (dragMode) {
				parent.c = parent.c.add(c.add(doff.inv()));
			} else {
				super.mousemove(c);
			}
		}
	}
	
	protected SWindowHeader windowHeader = null;
	protected SSimpleBorderBox windowBox = null;
	protected boolean isMinimized = false;
	
	protected boolean dropTarget = false;
	protected boolean dragMode = false;
	protected Coord doff = Coord.z;

	public SWindow(Coord c, Coord sz, Widget parent, String cap, boolean closeable, boolean minimizable) {
		super(c, new Coord(0, 0), parent);
		windowHeader = new SWindowHeader(Coord.z, Coord.z, this, cap, minimizable, closeable);
		windowBox = new SSimpleBorderBox(sz, 0, 2, 1);
		windowBox.marginTop = windowHeader.sz.y;
		resize(sz);
		setfocustab(true);
		parent.setfocus(this);
	}

	public SWindow(Coord c, Coord sz, Widget parent, String cap) {
		this(c, sz, parent, cap, true, true);
	}
    
	public void setText(String text) {
		windowHeader.setText(text);
	}
	
	public void setClosable(boolean closable) {
		windowHeader.setClosable(closable);
	}
	
	public void setMinimazable(boolean minimazable) {
		windowHeader.setMinimazable(minimazable);
	}
	
	public void resize(Coord newSize) {
		windowBox.contentSize = newSize;
		sz = windowBox.getBoxSize();
		windowHeader.c = xlate(new Coord(sz.div(2).sub(windowHeader.sz.div(2)).x, windowBox.getBorderPosition().y - windowHeader.sz.y + 2), false);
		
		for (Widget ch = child; ch != null; ch = ch.next)
			ch.presize();
	}

	public void draw(GOut initialGL) {
		if (!isMinimized) {
			initialGL.chcolor(0, 0, 0, 128);
			initialGL.frect(windowBox.getBorderPosition(), windowBox.getBorderSize().add(3, 3));
			if (windowBox.borderWidth != 0) {
				initialGL.chcolor(255, 255, 255, 255);
				initialGL.rect(windowBox.getBorderPosition(), windowBox.getBorderSize().add(3, 3));
			}
		}
		super.draw(initialGL);
	}

	public void pack() {
		Coord max = new Coord(0, 0);
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			Coord br = wdg.c.add(wdg.sz);
			if (br.x > max.x)
				max.x = br.x;
			if (br.y > max.y)
				max.y = br.y;
		}
		resize(max.sub(1, 1));
	}

	protected void minimize() {
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg == windowHeader) continue;
			wdg.visible = false;
		}
		isMinimized = true;
	}

	protected void maximize() {
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg == windowHeader) continue;
			wdg.visible = true;
		}
		isMinimized = false;
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
		
		if (super.mousedown(c, button)) return true;
		
		if (isMinimized) return false;
		
		if (c.isect(windowBox.getBorderPosition(), windowBox.getBorderSize())) {
			if (button == 1) {
				ui.grabmouse(this);
				dragMode = true;
				doff = c;
			}
			return true;
		} else return false;
	}

	public boolean mouseup(Coord c, int button) {
		if (dragMode) {
			ui.grabmouse(null);
			dragMode = false;
		} else {
			super.mouseup(c, button);
		}
		return true;
	}

	public void mousemove(Coord c) {
		if (dragMode) {
			this.c = this.c.add(c.add(doff.inv()));
		} else {
			super.mousemove(c);
		}
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (msg.equals("swindow_close")) {
			if (ui.isRWidget(this)) wdgmsg("close");
			else unlink();
		} else if (msg.equals("swindow_minimize")) {
			minimize();
		} else if (msg.equals("swindow_maximize")) {
			maximize();
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
