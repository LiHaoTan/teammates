<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="t" %>
<%@ taglib tagdir="/WEB-INF/tags/lti" prefix="ltiTag" %>

<c:set var="jsIncludes">
  <script type="text/javascript" src="/js/ltiInstructorCreate.js"></script>
</c:set>

<style>
  .box {
    max-width: 500px;
    margin: 0 auto;
  }

  body {
    background-color: #eaeff5 !important;
  }
</style>

<ltiTag:ltiPage bodyTitle="" pageTitle="TEAMMATES - New Instructor">
  <jsp:useBean id="data" scope="request" type="teammates.ui.pagedata.AdminHomePageData"/>
  <jsp:useBean id="signedParameters" scope="request" type="java.util.Map"/>
  <div class="box">
    <form action="/lti/ltiInstructorAccountAdd" method="post">
      <div class="overflow-auto alert alert-info">
        <span class="glyphicon glyphicon-exclamation-sign glyphicon-primary"></span>
        No account found linked to the LMS. You can register for an account or login if you already have a TEAMMATES account.
      </div>
      <%--<t:statusMessage statusMessagesToUser="${data.statusMessagesToUser}"/>--%>
      <div class="form-group">
        <label for="name" class="label-control">Name:</label>
        <input type="text" class="form-control" name="name" id="name" value="${data.instructorName}">
      </div>
      <div class="form-group">
        <label for="email" class="label-control">Email: </label>
        <input type="text" class="form-control" name="email" id="email" value="${data.instructorEmail}">
      </div>
      <div class="form-group">
        <label for="institution" class="label-control">Institution: </label>
        <input type="text" class="form-control" name="institution" id="institution"
               value="${data.instructorInstitution}">
      </div>
      <c:forEach var="signedParameter" items="${signedParameters}">
        <input type="hidden" name=
          <c:out value="${signedParameter.key}"/> value=<c:out value="${signedParameter.value}"/>>
      </c:forEach>

      <div class="form-group" style="margin:0 auto">
        <button class="btn btn-primary btn-block" id="btnAddInstructor">Register</button>
      </div>
      <div class="text-center form-group">
        Already have an account? <a href="/login?instructor=Instructor+Login">Login</a>
      </div>
    </form>
  </div>
</ltiTag:ltiPage>
