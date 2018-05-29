package org.psc.pom.version.manipulation;

import static org.junit.Assert.*;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;
import org.psc.pom.version.manipulation.engine.VersionManipulator;
import org.xml.sax.SAXException;

public class VersionManipulatorTest {
	
	private VersionManipulator versionManipulator = new VersionManipulator();
	@Test
	public void test() throws IOException, ParserConfigurationException, SAXException, TransformerException, XPathExpressionException {
		versionManipulator.readFile("pom.xml", "org.psc.maven", "basic-parent");
		fail("Not yet implemented");
	}

}
