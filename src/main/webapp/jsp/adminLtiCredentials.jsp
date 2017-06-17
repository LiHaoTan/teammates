<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="t" %>
<%@ taglib tagdir="/WEB-INF/tags/admin" prefix="ta" %>

<c:set var="jsIncludes">
  <script type="text/javascript" src="/js/adminLtiCredentials.js"></script>
</c:set>

<ta:adminPage bodyTitle="Add New LTI Credentials" pageTitle="TEAMMATES - Administrator" jsIncludes="${jsIncludes}">
  <div class="well well-plain">
    <div>
      <label class="label-control" for="consumerKey">Consumer Key</label>
      <div class="text-muted">
        <span class="glyphicon glyphicon-exclamation-sign glyphicon-primary"></span>
        Used to uniquely identify the tool consumer (e.g. an LMS domain name)
      </div>
      <input class="form-control addCredentialsFormControl" type="text" id="consumerKey">
    </div>
    <br>

    <div>
      <button class="btn btn-primary addCredentialsFormControl" id="btnGenerateCredentials">
        Generate Credentials
      </button>
    </div>
  </div>

  <div class="panel panel-primary" hidden id="addCredentialsResultPanel">
    <div class="panel-heading">
      <strong>Result</strong>
    </div>
    <div class="table-responsive">
      <table class="table table-striped table-hover" id="addCredentialsResultTable">
        <thead>
        <tr>
          <th>Consumer Key</th>
          <th>Consumer Secret</th>
          <th>Status</th> <!-- to be removed? -->
          <th>Message</th>
        </tr>
        </thead>
        <tbody>
        </tbody>
      </table>
    </div>
  </div>
  <t:statusMessage statusMessagesToUser="${data.statusMessagesToUser}"/>
</ta:adminPage>
