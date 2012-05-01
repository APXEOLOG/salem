package org.apxeolog.salem;

import java.lang.reflect.Array;
import java.util.Arrays;

public class ALS {
	public static void alDebugPrint(Object...objects) {
		for (Object obj : objects) {
			if (obj instanceof Array) System.out.print(Arrays.toString((Object[]) obj));
			else System.out.print(obj == null ? "null" : obj.toString() + " ");
		}
		System.out.println();
	}
}
