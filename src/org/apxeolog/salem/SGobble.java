package org.apxeolog.salem;

import haven.Coord;
import haven.GOut;
import haven.RichText;
import haven.Tex;
import haven.Text;
import haven.Widget;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.apxeolog.salem.SInterfaces.IGobble;

public class SGobble extends IGobble {
	protected int[] currentVals = new int[4];
	protected int[] fepVals = new int[4];
	protected BufferedImage[] textVal = new BufferedImage[4];
	
	protected static final Tex[] caseTex = new Tex[4];
	protected static final Text.Foundry valFoundry = new Text.Foundry("Serif", 16);
	protected Text currentTimerVar = valFoundry.render("0");
	
	protected Tex currentCase = null;
	long lastTimeTriggered = 0;
	
	static {
		Text.Foundry bufFoundry = new Text.Foundry("Serif", 20);
		bufFoundry.aa = true;
		for (int i = 0; i < 4; i++)
			caseTex[i] = bufFoundry.render(Integer.toString(i + 1), Color.YELLOW).tex();
	}
	
	protected Object tooltip;
	
	protected Coord baseSize = Coord.z;
	
	public SGobble(Coord c, Coord sz, Widget parent) {
		super(c, sz.add(20, 0), parent);
		baseSize = sz;
	}
	
	public void updateTextCache() {
		tooltip = null;
		for (int i = 0; i < 4; i++) {
			textVal[i] = Text.render(String.format("%.1f / %.1f", fepVals[i] / 1000D, currentVals[i] / 1000D)).img;
		}
	}

	public int updateMeters() {
		int[] newVal = new int[4];
		boolean needUpdate = false;
		int max = 0;
		for (int i = 0; i < 4; i++) {
			newVal[i] = ui.sess.glob.cattr.get(STempers.stat_uniq[i]).comp;
			if (newVal[i] != currentVals[i])
				needUpdate = true;
			if (newVal[i] > max) max = newVal[i];
		}
		currentVals = newVal;
		if (needUpdate) updateTextCache();
		return max;
	}
	
	public void draw(GOut g) {
		// Main BG
		g.chcolor(0, 0, 0, 128);
		g.frect(Coord.z, baseSize);
		// Vals
		g.chcolor(0, 0, 0, 128);
		g.frect(baseSize.sub(0, (sz.y / 4) * 3), new Coord(sz.x - baseSize.x, sz.y / 2));
		
		g.chcolor(255, 255, 255, 255);
		g.rect(Coord.z, baseSize.add(1, 1));
		
		g.chcolor(255, 255, 255, 255);
		g.rect(baseSize.sub(0, (sz.y / 4) * 3), new Coord(sz.x - baseSize.x, sz.y / 2).add(1, 1));
		
//		// Food
//		FoodInfo food = null;
//		if (ui.lasttip instanceof WItem.ItemTip) {
//			GItem item = ((WItem.ItemTip) ui.lasttip).item();
//			food = GItem.find(FoodInfo.class, item.info());
//		}
		
		double max = updateMeters();
		
		Coord rectSize = new Coord(baseSize.x - 6, 15);
		for (int i = 0; i < 4; i++) {
			// Bg
			g.chcolor(Color.BLACK);
			g.frect(new Coord(3, i * 16 + 3), rectSize);
			// BaseFep
			g.chcolor(STempers.stat_color[i].getRed(), STempers.stat_color[i].getGreen(), STempers.stat_color[i].getBlue(), 128);
			g.frect(new Coord(3, i * 16 + 3), rectSize.mul((double)currentVals[i] / max, 1D));
			// Fep
			g.chcolor(STempers.stat_color[i].getRed(), STempers.stat_color[i].getGreen(), STempers.stat_color[i].getBlue(), 255);
			g.frect(new Coord(3, i * 16 + 3), rectSize.mul((double)fepVals[i] / max, 1D));
			// Text
			g.chcolor(Color.WHITE);
			g.image(textVal[i], new Coord((sz.x - textVal[i].getWidth()) / 2, i * 16 + 3));
		}
		
		// Vals
		// ul = baseSize.sub(0, (sz.y / 4) * 3);
		// mid = baseSize.add((sz.x - baseSize.x) / 2, - (sz.y / 2));
		Coord mid = baseSize.add((sz.x - baseSize.x) / 2, - (sz.y / 2));
		if (currentCase != null) {
			g.aimage(currentCase, mid.sub(0, 8), 0.5, 0.5);
		}
		g.aimage(currentTimerVar.tex(), mid.add(0, 8), 0.5, 0.5);
	}

	public Object tooltip(Coord c, boolean again) {
		if (true) {
			if (tooltip == null) {
				StringBuilder buf = new StringBuilder();
				for (int i = 0; i < 4; i++)
					buf.append(String.format("%s: %.1f / %.1f\n", STempers.stat_name[i], fepVals[i] / 1000D, currentVals[i] / 1000D));
				tooltip = RichText.render(buf.toString(), 0).tex();
			}
			return tooltip;
		}
		return null;
	}
	
	public void updt(int[] n) {
		fepVals = n;
		updateTextCache();
	}

	public void trig(int a) {
		// Random case
		currentCase = caseTex[a];
		lastTimeTriggered = System.currentTimeMillis();
	}

	public void updv(int v) {
		// Update val
		currentTimerVar = valFoundry.render(String.valueOf(v));
	}
}
