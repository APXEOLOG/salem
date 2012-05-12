package org.apxeolog.salem;

import haven.Coord;
import haven.FoodInfo;
import haven.GItem;
import haven.GOut;
import haven.GameUI;
import haven.RichText;
import haven.Text;
import haven.Utils;
import haven.WItem;
import haven.Widget;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.apxeolog.salem.SInterfaces.ITempers;

public class STempers extends ITempers {
	public static final String[] stat_uniq = { "blood", "phlegm", "ybile", "bbile" };
	public static final String[] stat_name = { "Blood", "Phlegm", "Yellow Bile", "Black Bile" };
	public static final Color[] stat_color = { new Color(178, 0, 0, 255), new Color(0, 102, 179, 255), new Color(255, 151, 0, 255), new Color(128, 128, 128, 255) };
	
	protected int[] softVal = new int[4];
	protected int[] hardVal = new int[4];
	protected int[] maxVal = new int[4];
	protected BufferedImage[] textVal = new BufferedImage[4];
	
	
	protected boolean fullTempers = false;
	protected Object tooltip;
	
	public STempers(Coord c, Widget parent) {
		super(c, new Coord(120, 71), parent);
	}
	
	public void updateTextCache() {
		tooltip = null;
		for (int i = 0; i < 4; i++) {
			textVal[i] = Text.renderOutlined(String.format("%.1f / %.1f / %.1f", hardVal[i] / 1000D, softVal[i] / 1000D, maxVal[i] / 1000D), Color.WHITE, Color.BLACK, 1).img;
		}
	}

	public void updateMeters() {
		int[] max = new int[4];
		fullTempers = true;
		boolean needUpdate = false;
		for (int i = 0; i < 4; i++) {
			max[i] = ui.sess.glob.cattr.get(stat_uniq[i]).comp;
			if (max[i] == 0)
				return;
			if (hardVal[i] < max[i])
				fullTempers = false;
			if (max[i] != maxVal[i])
				needUpdate = true;
		}
		maxVal = max;
		if (needUpdate) updateTextCache();
	}
	
	public void draw(GOut g) {
		updateMeters();
		
		if (fullTempers)
			g.chcolor(0, 96, 0, 128);
		else
			g.chcolor(0, 0, 0, 128);
		g.frect(Coord.z, sz);
		
		if (fullTempers)
			g.chcolor(0, 255, 0, 128);
		else
			g.chcolor(255, 255, 255, 255);
		g.rect(Coord.z, sz.add(1, 1));
		
		// Food
		FoodInfo food = null;
		if (ui.lasttip instanceof WItem.ItemTip) {
			GItem item = ((WItem.ItemTip) ui.lasttip).item();
			food = GItem.find(FoodInfo.class, item.info());
		}
		
		Coord rectSize = new Coord(sz.x - 6, 15);
		for (int i = 0; i < 4; i++) {
			double hardP = Math.min((double)hardVal[i] / (double)maxVal[i], 1D);
			double softP = Math.min(((double)softVal[i] + (food != null ? food.tempers[i] : 0)) / (double)maxVal[i], 1D);
			// Bg
			g.chcolor(Color.BLACK);
			g.frect(new Coord(3, i * 16 + 3), rectSize);
			// Soft
			g.chcolor(stat_color[i].getRed(), stat_color[i].getGreen(), stat_color[i].getBlue(), 128);
			g.frect(new Coord(3, i * 16 + 3), rectSize.mul(softP, 1D));
			// Hard
			g.chcolor(stat_color[i].getRed(), stat_color[i].getGreen(), stat_color[i].getBlue(), 255);
			g.frect(new Coord(3, i * 16 + 3), rectSize.mul(hardP, 1D));
			// Text
			g.chcolor(Color.WHITE);
			g.image(textVal[i], new Coord((sz.x - textVal[i].getWidth()) / 2, i * 16 + 3));
		}
	}

	public Object tooltip(Coord c, boolean again) {
		if (true) {
			if (tooltip == null) {
				StringBuilder buf = new StringBuilder();
				for (int i = 0; i < 4; i++)
					buf.append(String.format("%s: %.1f / %.1f / %.1f\n", stat_name[i], hardVal[i] / 1000D, softVal[i] / 1000D, maxVal[i] / 1000D));
				tooltip = RichText.render(buf.toString(), 0).tex();
			}
			return tooltip;
		}
		return null;
	}
	
	public void upds(int[] n) {
		softVal = n;
		updateTextCache();
	}

	public void updh(int[] n) {
		hardVal = n;
		updateTextCache();
	}
	
	public boolean mousedown(Coord c, int button) {
		getparent(GameUI.class).act("gobble");
		return true;
	}
}
