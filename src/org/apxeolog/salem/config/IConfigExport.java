package org.apxeolog.salem.config;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface IConfigExport {
	/**
	 * Attach all configurable elements to root element here
	 * @param rootElement
	 * @param document
	 */
	void addElement(Element rootElement, Document document);

	/**
	 * This method called after initializing of main Salem classes (Resources, Threads etc)
	 * @param rootElement if not null -> load from default section
	 */
	void init(Element rootElement);
}
