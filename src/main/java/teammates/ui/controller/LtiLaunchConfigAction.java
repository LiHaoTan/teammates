package teammates.ui.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

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
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import teammates.common.util.Config;
import teammates.common.util.Const;

/**
 * Generates a LTI launch configuration represented in an IMS common cartridge. The launch configuration can be given to a
 * user to create a LTI launch to TEAMMATES.
 * @see <a href="https://www.imsglobal.org/specs/ltiv1p2/implementation-guide#toc-5">
 *     Representing Basic Launch Links in a Cartridge</a>
 */
public class LtiLaunchConfigAction extends Action {

    @Override
    protected ActionResult execute() {
        InputStream configXmlStream = session.getServletContext().getResourceAsStream("/lti/basicLtiLinkDescriptor.xml");
        Document xmlDocument = createXmlDocument(configXmlStream);

        configureDynamicContentForXmlElements(xmlDocument);

        return createXmlResult(asXmlString(xmlDocument));
    }

    private Document createXmlDocument(InputStream inputStream) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(inputStream);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Configures the content of XML elements that needs to be dynamically determined.
     * @param xmlDocument the XML document with elements to be dynamically determined
     */
    private void configureDynamicContentForXmlElements(Document xmlDocument) {
        String launchUrl = Config.getAppUrl(Const.ActionURIs.LTI_LAUNCH).toAbsoluteString();

        Node launchUrlNode = xmlDocument.getElementsByTagName("blti:launch_url").item(0);
        launchUrlNode.setTextContent(launchUrl);

        Node iconNode = xmlDocument.getElementsByTagName("blti:icon").item(0);
        iconNode.setTextContent(Config.getAppUrl("/apple-touch-icon.png").toAbsoluteString());

        Node courseNavigationUrlNode = evaluateXPath(xmlDocument,
                "/cartridge_basiclti_link"
                        + "/extensions[@platform='canvas.instructure.com']"
                        + "/options[@name='course_navigation']"
                        + "/property[@name='url']");
        courseNavigationUrlNode.setTextContent(launchUrl);
    }

    private Node evaluateXPath(Document document, String expression) {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        try {
            XPathExpression expr = xpath.compile(expression);
            return (Node) expr.evaluate(document, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    private String asXmlString(Document document) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}
