<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="t" %>
<%@ taglib tagdir="/WEB-INF/tags/lti" prefix="ltiTag" %>

<c:set var="jsIncludes">
  <script type="text/javascript" src="/js/ltiInstructorCreate.js"></script>
</c:set>

<ltiTag:ltiPage bodyTitle="" pageTitle="TEAMMATES - New Instructor">
  <jsp:useBean id="data" scope="request" type="teammates.ui.pagedata.PageData"/>
  <jsp:useBean id="errorMessage" scope="request" type="java.lang.String"/>

  <div class="panel panel-warning panel-narrow">
    <div class="panel-heading">
      <h3 class="panel-title">TEAMMATES cannot be launched</h3>
    </div>
    <div class="panel-body">
      ${errorMessage}
    </div>
  </div>
  <t:statusMessage statusMessagesToUser="${data.statusMessagesToUser}"/>
</ltiTag:ltiPage>
