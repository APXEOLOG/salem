package org.apxeolog.salem.config;

import haven.Resource;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apxeolog.salem.ALS;
import org.apxeolog.salem.utils.FileUtils;
import org.apxeolog.salem.utils.HXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * I want to add some pretty nice things from OOP here ^_^
 * @author APXEOLOG
 *
 */
public class XMLConfigProvider {
	private static Class<?>[] initalizer = {
		MinimapHighlightConfig.class, XConfig.class, UIConfig.class,
		ChatConfig.class, ToolbarsConfig.class };

	static {
		registeredConfigTypes = new HashMap<String, IConfigFactory>();
		loadedConfigs = new HashMap<String, IConfigExport>();
		try {
			// Init all classes to handle them properly
			for (Class<?> cl : initalizer)
				Class.forName(cl.getName(), true, cl.getClassLoader());
		} catch (ClassNotFoundException e) {
			// Whoops O_o
		}
	}

	private static HashMap<String, IConfigFactory> registeredConfigTypes;
	private static HashMap<String, IConfigExport> loadedConfigs;

	public static void registerConfig(String name, IConfigFactory factory) {
		registeredConfigTypes.put(name, factory);
	}

	public static IConfigExport getConfig(String name) {
		IConfigExport config = loadedConfigs.get(name);
		return config;
	}

	private static final int SAVE_TIMEOUT = 5000;
	private static long firstTimeSaveWasRequested = Long.MAX_VALUE;

	public static void save() {
		long currentTime = System.currentTimeMillis();
		if (currentTime - firstTimeSaveWasRequested > SAVE_TIMEOUT) {
			save(true);
			firstTimeSaveWasRequested = Long.MAX_VALUE;
		} else {
			firstTimeSaveWasRequested = currentTime;
		}
	}

	public static void save(boolean ignoreTiming) {
		if (ignoreTiming) {
			saveConfigToFile(new File("salem.xml"));
		}
	}

	public static void load() {
		loadConfigFromFile(new File("salem.xml"));
	}

	public static void init() {
		for (Entry<String, IConfigExport> entry : loadedConfigs.entrySet()) {
			try {
				entry.getValue().init(null);
			} catch (Exception ex) {
				ALS.alError("XMLConfigProvider: [ERROR_INIT_CONFIG]", entry.getKey(), ex);
				// Try init from default
				Element rootElement = getDefaultSection(entry.getKey());
				if (rootElement != null) entry.getValue().init(rootElement);
			}
		}
	}

	private static void loadConfig(String name, Element root) {
		IConfigFactory factory = registeredConfigTypes.get(name);
		if (factory != null) {
			loadedConfigs.put(name, factory.create(root));
		}
	}

	private static Element getDefaultSection(String name) {
		try {
			Document loadedDocument = HXml.readXMLFile(Resource.class.getResourceAsStream("/res/default_config.xml"));
			NodeList list = loadedDocument.getElementsByTagName(name);
			if (list.getLength() > 0) return (Element) list.item(0);
			else {
				ALS.alError("XMLConfigProvider: [ERROR_NO_SECTION]", name);
				return null;
			}
		} catch (Exception e) {
			ALS.alError("XMLConfigProvider: [ERROR_DEFAULT_READ]", e);
			return null;
		}
	}

	private static void loadConfigFromFile(File file) {
		if (!file.exists()) {
			try {
				file.createNewFile();
				FileUtils.copyStream(Resource.class.getResourceAsStream("/res/default_config.xml"), file);
			} catch (IOException e) {
				ALS.alError("XMLConfigProvider: [ERROR_DEFAULT_WRITE]");
			}
		}
		Document loadedDocument = HXml.readXMLFile(file);
		if (loadedDocument == null) return;
		try {
			Element rootElement = (Element) loadedDocument.getElementsByTagName("config").item(0);
			for (Entry<String, IConfigFactory> exporter : registeredConfigTypes.entrySet()) {
				try {
					NodeList list = rootElement.getElementsByTagName(exporter.getKey());
					if (list.getLength() > 0) loadConfig(exporter.getKey(), (Element) list.item(0));
					else loadConfig(exporter.getKey(), null);
				} catch (Exception ex) {
					ALS.alError("XMLConfigProvider: [ERROR_LOAD_CONFIG]", exporter.getKey(), ex);
				}
			}
		} catch (Exception ex) {
			ALS.alError("XMLConfigProvider: [ERROR_LOAD_CONFIGS]", ex);
		}
	}

	private static void saveConfigToFile(File file) {
		Document saveDocument = HXml.newDoc();
		Element rootElement = (Element) saveDocument.appendChild(saveDocument.createElement("config"));
		Element bufElem = null;
		for (Entry<String, IConfigExport> exporter : loadedConfigs.entrySet()) {
			try {
				bufElem = (Element) rootElement.appendChild(saveDocument.createElement(exporter.getKey()));
				exporter.getValue().addElement(bufElem, saveDocument);
			} catch (Exception ex) {
				ALS.alError("XMLConfigProvider: [ERROR_SAVE_CONFIG]", ex);
			}
		}
		HXml.saveXML(saveDocument, file);
	}
}
