package org.apxeolog.salem;

import java.lang.reflect.Array;
import java.util.Collection;

public class ALS {
	public static void alDebugPrint(Object...objects) {
		for (Object obj : objects) {
			if (obj instanceof Iterable<?>) {
				System.out.print("[ ");
				for (Object o : (Iterable<?>)obj) System.out.print(o == null ? "null" : o.toString() + " ");
				System.out.print("]");
			} /*else if (obj instanceof Collection<?>) {
				System.out.print("[ ");
				for (Object o : (Collection<?>)obj) System.out.print(o == null ? "null" : o.toString() + " ");
				System.out.print("]");
			}	*/
			else System.out.print(obj == null ? "null" : obj.toString() + " ");
		}
		System.out.println();
	}
}
