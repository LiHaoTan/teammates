package teammates.ui.controller;

import java.util.List;

import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.exception.EmailSendingException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.exception.TeammatesException;
import teammates.common.util.Config;
import teammates.common.util.Const;
import teammates.common.util.EmailWrapper;
import teammates.common.util.Logger;
import teammates.common.util.SanitizationHelper;
import teammates.common.util.StatusMessage;
import teammates.common.util.StatusMessageColor;
import teammates.common.util.StringHelper;
import teammates.common.util.Url;
import teammates.logic.api.EmailGenerator;
import teammates.logic.core.AdminAddInstructorLogic;
import teammates.ui.pagedata.AdminHomePageData;

public class AdminInstructorAccountAddAction extends Action {

    private static final Logger log = Logger.getLogger();

    // TODO Actions only use the Logic class which is a facade to every single *Logic class
    // not sure if it is such a great idea because it cause the Facade class to be so huge and not every class
    // needs everything in it, the upside is that the classes at least do not have direct coupling with the different
    // *Logic classes, perhaps it is probably better for each class to use the Logic classes they require though
    // like how different *Logic classes uses other *Logic classes
    private static final AdminAddInstructorLogic adminAddInstructorLogic = AdminAddInstructorLogic.inst();

    @Override
    protected ActionResult execute() {

        gateKeeper.verifyAdminPrivileges(account);

        AdminHomePageData data = new AdminHomePageData(account, sessionToken);

        data.instructorName = getNonNullRequestParamValue(Const.ParamsNames.INSTRUCTOR_NAME).trim();
        data.instructorEmail = getNonNullRequestParamValue(Const.ParamsNames.INSTRUCTOR_EMAIL).trim();
        data.instructorInstitution = getNonNullRequestParamValue(Const.ParamsNames.INSTRUCTOR_INSTITUTION).trim();
        data.isInstructorAddingResultForAjax = true;
        data.statusForAjax = "";

        data.instructorName = data.instructorName.trim();
        data.instructorEmail = data.instructorEmail.trim();
        data.instructorInstitution = data.instructorInstitution.trim();

        // END1 end here

        try {
            logic.verifyInputForAdminHomePage(data.instructorName,
                                              data.instructorInstitution, data.instructorEmail);
        } catch (InvalidParametersException e) {
            return createInstructorCreationFailAjaxResult(data, e);
        }

        String courseId;

        try {
            courseId = adminAddInstructorLogic.createCourseInstructorWithDemoData(data.instructorEmail, data.instructorName);
        } catch (Exception e) {
            return createInstructorImportRetryAjaxResult(data, e);
        }

        // TODO:
        // one instructor per course should only exist, but this should be designed in the schema properly
        // instead of getting a list
        // if we never get around to refactoring this, we should have a method that retrieves the instructor only
        // with the appropriate warnings
        List<InstructorAttributes> instructorAttributesList = logic.getInstructorsForCourse(courseId);
        final InstructorAttributes instructorAttributes = instructorAttributesList.get(0);

        String joinLink = createJoinLinkFromInstructorRegKey(instructorAttributes.key, data.instructorInstitution);

        EmailWrapper email = new EmailGenerator().generateNewInstructorAccountJoinEmail(
                instructorAttributes.email, data.instructorName, joinLink);

        // TODO sending email takes up quite a lot of time, however this may not be of as much concern
        // since there is only one admin though it should be sent to a queue instead
        try {
            emailSender.sendEmail(email);
        } catch (EmailSendingException e) {
            log.severe("Instructor welcome email failed to send: " + TeammatesException.toStringWithStackTrace(e));
        }

        data.statusForAjax = createInstructorCreationSuccessAjaxStatus(data, joinLink);

        createInstructorCreationSuccessExecutionStatus(data);

        return createAjaxResult(data);
    }

    private String createJoinLinkFromInstructorRegKey(String instructorRegKey, String instructorInstitution) {
        return Config.getAppUrl(Const.ActionURIs.INSTRUCTOR_COURSE_JOIN)
                .withRegistrationKey(StringHelper.encrypt(instructorRegKey))
                .withInstructorInstitution(instructorInstitution)
                .toAbsoluteString();
    }

    private void createInstructorCreationSuccessExecutionStatus(AdminHomePageData data) {
        statusToUser.add(new StatusMessage(data.statusForAjax, StatusMessageColor.SUCCESS));
        statusToAdmin = "A New Instructor <span class=\"bold\">"
                + SanitizationHelper.sanitizeForHtmlTag(data.instructorName) + "</span> has been created.<br>"
                + "<span class=\"bold\">Id: </span>"
                + "ID will be assigned when the verification link was clicked and confirmed"
                + "<br>"
                + "<span class=\"bold\">Email: </span>" + SanitizationHelper.sanitizeForHtmlTag(data.instructorEmail)
                + "<span class=\"bold\">Institution: </span>"
                + SanitizationHelper.sanitizeForHtmlTag(data.instructorInstitution);
    }

    private String createInstructorCreationSuccessAjaxStatus(AdminHomePageData data, String joinLink) {
        return "Instructor " + SanitizationHelper.sanitizeForHtml(data.instructorName)
                + " has been successfully created " + "<a href=" + joinLink + ">" + Const.JOIN_LINK + "</a>";
    }

    /**
     * Creates an AJAX result with a link to retry import data failure.
     */
    private ActionResult createInstructorImportRetryAjaxResult(AdminHomePageData data, Exception e) {
        String retryUrl = Const.ActionURIs.ADMIN_INSTRUCTORACCOUNT_ADD;
        retryUrl = Url.addParamToUrl(retryUrl, Const.ParamsNames.INSTRUCTOR_NAME, data.instructorName);
        retryUrl = Url.addParamToUrl(retryUrl, Const.ParamsNames.INSTRUCTOR_EMAIL, data.instructorEmail);
        retryUrl = Url.addParamToUrl(retryUrl, Const.ParamsNames.INSTRUCTOR_INSTITUTION, data.instructorInstitution);
        retryUrl = Url.addParamToUrl(retryUrl, Const.ParamsNames.SESSION_TOKEN, data.getSessionToken());

        StringBuilder errorMessage = new StringBuilder(100);
        String retryLink = "<a href=" + retryUrl + ">Exception in Importing Data, Retry</a>";
        errorMessage.append(retryLink);

        statusToUser.add(new StatusMessage(errorMessage.toString(), StatusMessageColor.DANGER));

        String message = "<span class=\"text-danger\">Servlet Action failure in AdminInstructorAccountAddAction" + "<br>"
                + e.getClass() + ": " + TeammatesException.toStringWithStackTrace(e) + "<br></span>";

        errorMessage.append("<br>").append(message);
        statusToUser.add(new StatusMessage("<br>" + message, StatusMessageColor.DANGER));
        statusToAdmin = message;

        data.isInstructorAddingResultForAjax = false;
        data.statusForAjax = errorMessage.toString();
        return createAjaxResult(data);
    }

    private ActionResult createInstructorCreationFailAjaxResult(AdminHomePageData data, InvalidParametersException e) {
        data.statusForAjax = e.getMessage().replace(System.lineSeparator(), Const.HTML_BR_TAG);
        data.isInstructorAddingResultForAjax = false;
        statusToUser.add(new StatusMessage(data.statusForAjax, StatusMessageColor.DANGER));
        return createAjaxResult(data);
    }
}
