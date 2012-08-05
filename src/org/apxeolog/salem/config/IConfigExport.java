package org.apxeolog.salem.config;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface IConfigExport {
	void addElement(Element rootElement, Document document);
	void init();
}
