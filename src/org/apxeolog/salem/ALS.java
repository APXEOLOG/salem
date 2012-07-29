package org.apxeolog.salem;

import java.lang.reflect.Array;
import java.util.Collection;

public class ALS {
	protected static int PRINT_LEVEL = 0;
	
	public static void alDebugPrint(Object...objects) {
		if (PRINT_LEVEL == 0) return;
		for (Object obj : objects) {
			if (obj instanceof Iterable<?>) {
				System.out.print("[ ");
				for (Object o : (Iterable<?>)obj) System.out.print(o == null ? "null" : "["+o.getClass().getCanonicalName()+"]" + o.toString() + " ");
				System.out.print("]");
			}
			else System.out.print(obj == null ? "null" : obj.toString() + " ");
		}
		System.out.println();
	}
}
