package org.apxeolog.salem;

public class ALS {
	protected static int DEBUG_LEVEL = 1;
	protected static int ERROR_LEVEL = 1;

	public static void alDebugPrint(Object...objects) {
		if (DEBUG_LEVEL == 0) return;

		for (Object obj : objects) {
			if (obj instanceof Iterable<?>) {
				System.out.print("[ ");
				for (Object o : (Iterable<?>)obj) System.out.print(o == null ? "null" : "["+o.getClass().getSimpleName()+"]" + o.toString() + " ");
				System.out.print("]");
			}
			else System.out.print(obj == null ? "null" : obj.toString() + " ");
		}
		System.out.println();
	}

	public static void alError(Object...objects) {
		if (ERROR_LEVEL == 0) return;
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
