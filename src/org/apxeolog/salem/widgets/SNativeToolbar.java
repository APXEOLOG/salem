package org.apxeolog.salem.widgets;

import org.apxeolog.salem.config.ToolbarsConfig.TBConfig;
import org.apxeolog.salem.config.ToolbarsConfig.TBSlot;

import haven.Coord;
import haven.DTarget;
import haven.DropTarget;
import haven.GOut;
import haven.Indir;
import haven.Resource;
import haven.Tex;
import haven.Widget;
import static haven.Inventory.invsq;

public class SNativeToolbar extends SWindow implements DTarget, DropTarget {
	private Indir<Resource>[] gameUIBelt;
	private TBConfig tbConfig;

	public SNativeToolbar(Coord c, Coord sz, Widget parent, Indir<Resource>[] belt, TBConfig config) {
		super(c, sz, parent, config.tbName);
		tbConfig = config;
		resizeToolbar();
	}

	public void resizeToolbar() {
		int width = tbConfig.tbSlots.size() * (invsq.sz().x + 1);
		Coord newSz = tbConfig.isVertical ? new Coord(invsq.sz().x, width) : new Coord(width, invsq.sz().y);
		resize(newSz);
	}

	private Resource getSlotContent(int index) {
		if (index < 0 || index >= tbConfig.tbSlots.size()) return null;
		TBSlot slotc = tbConfig.tbSlots.get(index);
		if (slotc.sGLobal < 0 || slotc.sGLobal >= gameUIBelt.length) return null;
		return gameUIBelt[slotc.sGLobal].get();
	}

	@Override
	public void draw(GOut initGL){
		super.draw(initGL);
		if (isMinimized) return;

		Resource slotr; Coord drawCoord;
		for (int i = 0; i < tbConfig.tbSlots.size(); i++) {
			drawCoord = tbConfig.isVertical ? new Coord(invsq.sz().x, (invsq.sz().x + 1) * i) : new Coord((invsq.sz().x + 1) * i, invsq.sz().y);
			initGL.image(invsq, drawCoord);

			slotr = getSlotContent(i);
			if (slotr != null) {
				Tex slotTex = slotr.layer(Resource.imgc).tex();
				if (slotTex != null) {
					initGL.image(slotTex, drawCoord.add(1, 1), invsq.sz().sub(2, 2));
				}
			}
		}
	}

	@Override
	public boolean dropthing(Coord cc, Object thing) {
		// TODO Auto-generated method stub
		return false;
	}
}
