<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="t" %>
<%@ taglib tagdir="/WEB-INF/tags/admin" prefix="ta" %>
<%@ taglib tagdir="/WEB-INF/tags/admin/home" prefix="adminHome" %>

<c:set var="jsIncludes">
    <script type="text/javascript" src="/js/adminHome.js"></script>
</c:set>

<ta:adminPage bodyTitle="Add New Instructor Test" pageTitle="TEAMMATES - Lti Test" jsIncludes="${jsIncludes}">
    <div class="well well-plain">
        <div>
            <label class="label-control">Adding a Single Instructor</label>
        </div>
        <br>
        <div>
            <label class="label-control">Short Name:</label>
            <input class="form-control addInstructorFormControl" type="text" id="instructorShortName" value="${param.name}">
        </div>
        <br>
        <div>
            <label class="label-control">Name:</label>
            <input class="form-control addInstructorFormControl" type="text" id="instructorName" value="${param.name}">
        </div>
        <br>
        <div>
            <label class="label-control">Email: </label>
            <input class="form-control addInstructorFormControl" type="text" id="instructorEmail" value="${param.email}">
        </div>
        <br>
        <div>
            <label class="label-control">Institution: </label>
            <input class="form-control addInstructorFormControl" type="text" id="instructorInstitution" value="${param.institution}">
        </div>
        <br>

        <div>
            <!-- to replace onclick -->
            <button class="btn btn-primary addInstructorFormControl addInstructorBtn" id="btnAddInstructor"
                    onclick="replacethisandaddsomethingreal">Confirm</button>
        </div>
    </div>

    <t:statusMessage statusMessagesToUser="${data.statusMessagesToUser}" />
</ta:adminPage>
