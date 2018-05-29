package org.psc.pom.version.manipulation.engine;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class VersionManipulator {

	public void readFile(String pathUri, String groupId, String artifactId) throws IOException,
			ParserConfigurationException, SAXException, TransformerException, XPathExpressionException {
		Path path = Paths.get(pathUri);

		FileInputStream fis = new FileInputStream(path.toFile());
		DocumentBuilder domBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = domBuilder.parse(fis);
		fis.close();

		List<Node> versions = new ArrayList<>();
		NodeList groupIds = document.getElementsByTagName("groupId");
		XPath xp = XPathFactory.newInstance().newXPath();
		
		for (int i = 0; i < groupIds.getLength(); i++) {
			Node current = groupIds.item(i);
			if (current.getTextContent().equals(groupId)) {
				Node version = null;
				boolean addVersion = false;
				NodeList siblings = (NodeList) xp.evaluate("following-sibling::*", current, XPathConstants.NODESET);

				for (int j = 0; j < siblings.getLength(); j++) {
					Node currentSibling = siblings.item(j);
					System.out.println(currentSibling);
					if (currentSibling.getNodeName().equals("version")) {
						version = currentSibling;
					} else if (currentSibling.getNodeName().equals("artifactId")
							&& currentSibling.getTextContent().equals(artifactId)) {
						addVersion = true;
					}
				}
				if (addVersion && version != null) {
					versions.add(version);
				}
			}
		}

		versions.stream().filter(e -> !StringUtils.startsWith(e.getTextContent(), "${")).forEach(e -> e.setTextContent("9.9.9"));
		versions.forEach(e -> System.out.println(e.getNodeName() + " = " + e.getTextContent()));

		DOMSource domSource = new DOMSource(document);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.transform(domSource, result);
		
		String manipulatedXml = writer.toString();
		
		Files.write(path, manipulatedXml.getBytes());
	}
}
