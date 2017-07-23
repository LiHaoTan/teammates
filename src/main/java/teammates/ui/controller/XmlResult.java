package teammates.ui.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import teammates.common.datatransfer.attributes.AccountAttributes;
import teammates.common.util.StatusMessage;

/**
 * A result that represents a XML response.
 */
public class XmlResult extends ActionResult {

    private String xmlString;

    XmlResult(String destination,
                     AccountAttributes account,
                     List<StatusMessage> status, String xmlString) {
        super(destination, account, status);
        this.xmlString = xmlString;
    }

    @Override
    public void send(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/xml");
        resp.setCharacterEncoding("UTF-8");

        resp.getWriter().write(xmlString);
    }
}
