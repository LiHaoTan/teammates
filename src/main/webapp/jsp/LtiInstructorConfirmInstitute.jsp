<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="t" %>
<%@ taglib tagdir="/WEB-INF/tags/lti" prefix="ltiTag" %>
<%@ page import="teammates.common.util.Const"%>

<c:set var="jsIncludes">
  <script type="text/javascript" src="/js/ltiInstructorCreate.js"></script>
</c:set>
<c:set var="institutionParamName" value="<%= Const.ParamsNames.INSTRUCTOR_INSTITUTION %>"/>
<c:set var="regkeyParamName" value="<%= Const.ParamsNames.REGKEY %>"/>

<jsp:useBean id="data" scope="request" type="teammates.ui.pagedata.AdminHomePageData"/>
<jsp:useBean id="joinLink" scope="request" type="java.lang.String"/>
<jsp:useBean id="regkey" scope="request" type="java.lang.String"/>

<ltiTag:ltiPage bodyTitle="Continue registration for instructor" pageTitle="TEAMMATES - Confirm your details">
  <div class="overflow-auto alert alert-info">
    <span class="glyphicon glyphicon-exclamation-sign glyphicon-primary"></span>
    You have a previous incomplete registration. Please confirm your details again to proceed.
  </div>
  <%--<t:statusMessage statusMessagesToUser="${data.statusMessagesToUser}"/>--%>

  <form action="${joinLink}" method="get" class="well well-plain">
    <div class="form-group">
      <label for="name" class="label-control">Name:</label>
      <input type="text" class="form-control" id="name" disabled value="${data.instructorName}">
    </div>
    <div class="form-group">
      <label for="email" class="label-control">Email: </label>
      <input type="text" class="form-control" id="email" disabled value="${data.instructorEmail}">
    </div>
    <div class="form-group">
      <label for="institution" class="label-control">Institution: </label>
      <input type="text" class="form-control" name="${institutionParamName}" id="institution"
             value="${data.instructorInstitution}">
    </div>
    <input type="hidden" name="${regkeyParamName}" value="${regkey}"/>
    <!--<input type="hidden" name="token" id="token">-->
    <button class="btn btn-primary addInstructorFormControl addInstructorBtn" id="btnAddInstructor">Confirm</button>
  </form>
</ltiTag:ltiPage>
