<%@ tag description="Generic Admin Page" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="t" %>
<%@ taglib tagdir="/WEB-INF/tags/lti" prefix="ltiTag" %>

<%@ attribute name="pageTitle" required="true" %>
<%@ attribute name="cssIncludes" %>
<%@ attribute name="jsIncludes" %>
<%@ attribute name="bodyTitle" required="true" %>

<t:page pageTitle="${pageTitle}" bodyTitle="${bodyTitle}">
  <jsp:attribute name="cssIncludes">
    ${cssIncludes}
  </jsp:attribute>
  <jsp:attribute name="jsIncludes">
    ${jsIncludes}
  </jsp:attribute>
  <jsp:attribute name="navBar">
    <ltiTag:navBar />
  </jsp:attribute>
  <jsp:attribute name="bodyFooter">
    <t:bodyFooter />
  </jsp:attribute>
  <jsp:body>
    <jsp:doBody />
  </jsp:body>
</t:page>
