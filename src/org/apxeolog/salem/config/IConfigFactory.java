package org.apxeolog.salem.config;

import org.w3c.dom.Element;

public interface IConfigFactory {
	IConfigExport create(Element sectionRoot);
}
