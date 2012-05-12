package org.apxeolog.salem;

import haven.Coord;
import haven.Widget;

public class SInterfaces {
	public static abstract class ITempers extends Widget {
		public ITempers(Coord c, Coord sz, Widget parent) {
			super(c, sz, parent);
		}
		
		public abstract void upds(int[] n);
		public abstract void updh(int[] n);
	}	
	
	public static abstract class IGobble extends Widget {
		public IGobble(Coord c, Coord sz, Widget parent) {
			super(c, sz, parent);
		}
		
		public abstract void updt(int[] n);
		public abstract void trig(int a);
		public abstract void updv(int v);
	}	
}
