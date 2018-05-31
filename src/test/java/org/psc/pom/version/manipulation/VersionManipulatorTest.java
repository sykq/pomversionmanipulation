package org.psc.pom.version.manipulation;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.psc.pom.version.manipulation.engine.VersionManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class VersionManipulatorTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(VersionManipulatorTest.class);

	private VersionManipulator versionManipulator = new VersionManipulator();

	@Test
	public void test() throws IOException, ParserConfigurationException, SAXException, TransformerException,
			XPathExpressionException {
		versionManipulator.readFile("pom.xml", "org.psc.maven", "basic-parent");
		fail("Not yet implemented");
	}

	@Test
	public void test2() throws IOException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
		List<Path> filesToProcess = new ArrayList<>();

		Files.walkFileTree(Paths.get(".\\src\\test\\resources\\"), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				if (StringUtils.endsWith(file.getFileName().toString(), ".xml")) {
					filesToProcess.add(file);
					LOGGER.info("{} added to filesToProcess", file.getFileName().toString());
				}
				return FileVisitResult.CONTINUE;
			}
		});

		versionManipulator.replaceVersionOccurrences(filesToProcess, s -> s.equals("ch.qos.logback"),
				s -> s.startsWith("logback"), s -> true, "1.99.0");
	}
}
