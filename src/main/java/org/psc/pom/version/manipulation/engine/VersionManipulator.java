package org.psc.pom.version.manipulation.engine;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class VersionManipulator {
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionManipulator.class);

    private static final String VARIABLE_IDENTIFIER = "${";

    @Deprecated
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
                    final String siblingNodeName = currentSibling.getNodeName();
                    final String siblingContent = currentSibling.getTextContent();
                    LOGGER.info("current sibling: {} - {}", siblingNodeName, siblingContent);
                    if (siblingNodeName.equals("version")) {
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

        versions.stream().filter(e -> !StringUtils.startsWith(e.getTextContent(), "${"))
                .forEach(e -> e.setTextContent("9.9.9"));
        versions.forEach(e -> LOGGER.info("{} = {}", e.getNodeName(), e.getTextContent()));

        DOMSource domSource = new DOMSource(document);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);

        String manipulatedXml = writer.toString();

        Files.write(path, manipulatedXml.getBytes());
    }

    public void replaceVersionOccurrences(List<Path> paths, Predicate<String> groupIdFilter,
                                          Predicate<String> artifactIdFilter, Predicate<String> versionFilter, String newVersion)
            throws XPathExpressionException, IOException, ParserConfigurationException, SAXException,
            TransformerException {
        for (Path path : paths) {
            replaceVersionOccurrences(path, groupIdFilter, artifactIdFilter, versionFilter, newVersion);
        }
    }

    public void replaceVersionOccurrences(Path path, Predicate<String> groupIdFilter,
                                          Predicate<String> artifactIdFilter, Predicate<String> versionFilter, String newVersion) throws IOException,
            ParserConfigurationException, SAXException, TransformerException, XPathExpressionException {

        Document document = buildDocumentFromPom(path);

        List<String> variableNames = new ArrayList<>();
        List<Node> versions = new ArrayList<>();
        NodeList groupIds = document.getElementsByTagName("groupId");
        XPath xp = XPathFactory.newInstance().newXPath();

        for (int i = 0; i < groupIds.getLength(); i++) {
            final Node current = groupIds.item(i);
            if (groupIdFilter.test(current.getTextContent())) {
                Node version = null;
                String potentialVariable = null;
                boolean addVersion = false;
                final NodeList siblings = (NodeList) xp.evaluate("following-sibling::*", current,
                        XPathConstants.NODESET);

                for (int j = 0; j < siblings.getLength(); j++) {
                    final Node currentSibling = siblings.item(j);

                    final String siblingNodeName = currentSibling.getNodeName();
                    final String siblingContent = currentSibling.getTextContent();

                    LOGGER.info("current sibling: {} - {}", siblingNodeName, siblingContent);

                    if (siblingNodeName.equals("version")) {

                        // the order of the condition-checks is important here:
                        // if the test for variable_identifier-content is positioned after the
                        // versionFilter-test, then if e.g. the versionFilter is set to always return
                        // true, the variable-resolution-logic will not be applied.
                        // an alternative solution without an else-path (just ifs), doesn't yield
                        // satisfying results either (the ${...}-variable reference wihtin a
                        // version-element would then be just overriden with the new version
                        // TODO: reevaluate
                        if (siblingContent.startsWith(VARIABLE_IDENTIFIER)) {
                            potentialVariable = siblingContent;
                        } else if (versionFilter.test(siblingContent)) {
                            version = currentSibling;
                        }

                    } else if (currentSibling.getNodeName().equals("artifactId")
                            && artifactIdFilter.test(currentSibling.getTextContent())) {
                        addVersion = true;
                        LOGGER.info("sibling version will be replaced");
                    }
                }
                if (addVersion && version != null) {
                    versions.add(version);
                } else if (addVersion && potentialVariable != null) {
                    variableNames.add(extractVariableName(potentialVariable));
                }
            }
        }

        versions.stream().filter(e -> !StringUtils.startsWith(e.getTextContent(), VARIABLE_IDENTIFIER))
                .peek(e -> LOGGER.info("version replaced")).forEach(e -> e.setTextContent(newVersion));

        replaceVariableVersions(document, variableNames, versionFilter, newVersion);

        writeFile(path, document);
    }

    private void replaceVariableVersions(Document document, List<String> variableNames, Predicate<String> versionFilter,
                                         String newVersion) {

        for (String variable : variableNames) {
            NodeList variablesNodes = document.getElementsByTagName(variable);

            // only one occurrence of a specific variable is possible within a document,
            // therefore check for getLength() == 1
            // TODO: maybe throw an exception if that condition does'nt hold true
            if (variablesNodes != null && variablesNodes.getLength() == 1) {
                final Node variableNode = variablesNodes.item(0);
                final String variableContent = variableNode.getTextContent();
                if (versionFilter.test(variableContent)) {
                    // TODO: variables which use the value of another variable (although referencing
                    // a variable for a variable-value with a variable which is defined within the
                    // same pom, should not occur anyways)
                    variableNode.setTextContent(newVersion);
                    LOGGER.info("version of variable {} replaced: {} -> {}", variable, variableContent, newVersion);
                }
            }
        }
    }

    public static Document buildDocumentFromPom(Path pomPath)
            throws IOException, ParserConfigurationException, SAXException {
        FileInputStream fis = new FileInputStream(pomPath.toFile());
        DocumentBuilder domBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = domBuilder.parse(fis);
        fis.close();
        return document;
    }

    private void writeFile(Path target, Document document) throws IOException, TransformerException {
        DOMSource domSource = new DOMSource(document);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);

        String manipulatedXml = writer.toString();

        Files.write(target, manipulatedXml.getBytes());
    }

    private String extractVariableName(String variableReference) {
        /*
         * I'm too dumb Pattern p = Pattern.compile("[^${]+[^}]"); Matcher m =
         * p.matcher(variableReference); String extractedVariableName = m.group();
         */
        String trimmedVarRef = variableReference.trim();
        return StringUtils.mid(trimmedVarRef, 2, trimmedVarRef.length() - 3);
    }

}
