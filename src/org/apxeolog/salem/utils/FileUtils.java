package org.apxeolog.salem.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
	public static boolean copyStream(final InputStream is, final File f) {
		try {
			return FileUtils.copyStream(is, new FileOutputStream(f));
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean copyStream(final InputStream is, final OutputStream os) {
		try {
			final byte[] buf = new byte[1024];

			int len = 0;
			while ((len = is.read(buf)) > 0) {
				os.write(buf, 0, len);
			}
			is.close();
			os.close();
			return true;
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
