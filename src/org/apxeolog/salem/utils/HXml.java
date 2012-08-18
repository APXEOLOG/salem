package org.apxeolog.salem.utils;

import java.io.File;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HXml {
	public static Document readXMLFile(File file) {
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		builderFactory.setIgnoringElementContentWhitespace(true);
		builderFactory.setIgnoringComments(true);
		DocumentBuilder builder = null;
		try {
			builder = builderFactory.newDocumentBuilder();
			Document document = builder.parse(file);
			doRecursive(document);
			return document;
		} catch (Exception e) {
			return null;
		}
	}

	public static Document readXMLFile(InputStream stream) {
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		builderFactory.setIgnoringElementContentWhitespace(true);
		builderFactory.setIgnoringComments(true);
		DocumentBuilder builder = null;
		try {
			builder = builderFactory.newDocumentBuilder();
			Document document = builder.parse(stream);
			doRecursive(document);
			return document;
		} catch (Exception e) {
			return null;
		}
	}

	private static void doRecursive(Node node) {
		if (node == null) return;
		NodeList nodes = node.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node buf = nodes.item(i);
			if (buf.getNodeType() == Node.TEXT_NODE) {
				if (buf.getNodeValue().matches("\\s+")) {
					node.removeChild(buf);
					i--;
				} else {
					doRecursive(buf);
				}
			} else {
				doRecursive(buf);
			}
		}
	}

	public static Document newDoc() {
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		builderFactory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder builder = null;
		try {
			builder = builderFactory.newDocumentBuilder();
			Document document = builder.newDocument();
			return document;
		} catch (Exception e) {
			return null;
		}
	}

	public static void saveXML(Document doc, File file) {
		try {
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(file);
			transformer.transform(source, result);
		} catch (Exception ex) {
		}
	}
}
