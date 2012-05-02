package org.apxeolog.salem;

import java.lang.reflect.Array;

public class ALS {
	public static void alDebugPrint(Object...objects) {
		for (Object obj : objects) {
			if (obj instanceof Array) {
				System.out.print("[ ");
				for (Object o : (Object[])obj) System.out.print(o == null ? "null" : o.toString() + " ");
				System.out.print("]");
			}
			else System.out.print(obj == null ? "null" : obj.toString() + " ");
		}
		System.out.println();
	}
}
