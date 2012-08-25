package org.apxeolog.salem.config;

import haven.MapView;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;

import org.apxeolog.salem.widgets.SWindow;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *  == Haven and Hearth Extended ==
 * Class for save/load settings. This class use reflection.
 * Just add public static field and it will be saved into haven.cfg
 * Hardcode default values, this mechanism do not provide them.
 * @author APXEOLOG
 *
 */
public class XConfig implements IConfigExport {
	public static Boolean 	cl_no_minimap_chache_limit = true;
	public static Boolean 	cl_dump_minimaps = true;
	public static Boolean 	cl_minimap_show_grid = false;
	public static Boolean 	cl_render_on = true;
	public static Boolean 	cl_tilify = true;
	public static Boolean 	cl_use_new_tempers = true;
	public static Integer	cl_sfx_volume = 0;
	public static Integer 	cl_swindow_header_align = SWindow.HEADER_ALIGN_CENTER;
	public static Boolean	cl_use_new_chat = true;
	public static Boolean	cl_use_free_cam = false;
	public static Boolean	cl_use_new_toolbars = true;
	public static String 	mp_error_url = "http://unionclient.ru/salem/error/";
	public static Integer	cl_grid_mode = MapView.GRID_MODE_NONE;
	public static String	mp_guid = UUID.randomUUID().toString();
	public static Boolean 	cl_debug_mode = false;
	public static String	mp_irc_server = "irc.synirc.net";
	public static Integer	mp_irc_port = 6667;
	public static String	mp_irc_username = "";
	public static String	mp_irc_password = "";
	public static Boolean	mp_irc_autoconnect = false;

	@SuppressWarnings("unchecked")
	private static <T, V> T castToType(V classType, Object value) {
		if (classType.equals(Integer.class)) {
			return (T) Integer.valueOf((String)value);
		} else if (classType.equals(Long.class)) {
			return (T) Long.valueOf((String)value);
		} else if (classType.equals(String.class)) {
			return (T) (String)value;
		} else if (classType.equals(Boolean.class)) {
			return (T) Boolean.valueOf((String)value);
		} else if (classType.equals(Float.class)) {
			return (T) Float.valueOf((String)value);
		} else return null;
	}

	static {
		XMLConfigProvider.registerConfig("code", new IConfigFactory() {
			@Override
			public IConfigExport create(Element sectionRoot) {
				if (sectionRoot == null) return new XConfig();
				else return new XConfig(sectionRoot);
			}
		});
	}

	public XConfig() {

	}

	public XConfig(Element sectionRoot) {
		NodeList list = sectionRoot.getElementsByTagName("property");
		Element currentNode = null;
		for (int i = 0; i < list.getLength(); i++) {
			try {
				currentNode = (Element) list.item(i);
				String fieldName = currentNode.getAttribute("name");
				String fieldValue = currentNode.getChildNodes().item(0).getNodeValue();
				Field optionField = XConfig.class.getField(fieldName);
				if (Modifier.isStatic(optionField.getModifiers()) && Modifier.isPublic(optionField.getModifiers())) {
					optionField.set(null, castToType(optionField.getType(), fieldValue));
				}
			} catch (NoSuchFieldException e) {
			} catch (SecurityException e) {
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (NullPointerException e) {
			}
		}
	}

	@Override
	public void addElement(Element rootElement, Document document) {
		for (Field field : XConfig.class.getFields()) {
			if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
				try {
					Element bufElem = (Element) rootElement.appendChild(document.createElement("property"));
					bufElem.setAttribute("name", field.getName());
					bufElem.appendChild(document.createTextNode(String.valueOf(field.get(null))));
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				}
			}
		}
	}

	@Override
	public void init(Element rootElement) {

	}
}
