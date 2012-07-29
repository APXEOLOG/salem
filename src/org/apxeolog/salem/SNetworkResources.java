package org.apxeolog.salem;

import haven.HackThread;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class SNetworkResources {
	public static String sendGetRequest(String endpoint, String requestParameters) {
		String result = null;
		if (endpoint.startsWith("http://")) {
			try {

				// Send data
				String urlStr = endpoint;
				if (requestParameters != null && requestParameters.length() > 0) {
					urlStr += "?" + requestParameters;
				}
				URL url = new URL(urlStr);
				URLConnection conn = url.openConnection();

				// Get the response
				BufferedReader rd = new BufferedReader(new InputStreamReader(
						conn.getInputStream()));
				StringBuffer sb = new StringBuffer();
				String line;
				while ((line = rd.readLine()) != null) {
					sb.append(line);
				}
				rd.close();
				result = sb.toString();
			} catch (Exception e) {
				//
			}
		}
		return result;
	}

	public static void onGameUILoad() {
		// Send some statistics when player enters game
		new Thread(HackThread.tg(), new Runnable() {
			@Override
			public void run() {
				// Send random generated GUID
				sendGetRequest("http://unionclient.ru/salem/stat/", "guid=" + HConfig.mp_guid);
			}
		}).start();

	}
}
