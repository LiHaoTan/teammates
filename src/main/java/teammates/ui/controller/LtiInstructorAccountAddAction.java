package teammates.ui.controller;

import java.util.List;

import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.LtiAccountAttributes;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.exception.UnauthorizedAccessException;
import teammates.common.util.Config;
import teammates.common.util.Const;
import teammates.common.util.Logger;
import teammates.common.util.SanitizationHelper;
import teammates.common.util.StringHelper;
import teammates.logic.core.AdminAddInstructorLogic;
import teammates.storage.api.LtiAccountDb;

public class LtiInstructorAccountAddAction extends Action {

    private static final Logger log = Logger.getLogger();

    // TODO Actions only use the Logic class which is a facade to every single *Logic class
    // not sure if it is such a great idea because it cause the Facade class to be so huge and not every class
    // needs everything in it, the upside is that the classes at least do not have direct coupling with the different
    // *Logic classes, perhaps it is probably better for each class to use the Logic classes they require though
    // like how different *Logic classes uses other *Logic classes
    private static final AdminAddInstructorLogic adminAddInstructorLogic = AdminAddInstructorLogic.inst();

    @Override
    protected ActionResult execute() {
        // TODO maybe to remove
        gateKeeper.verifyLtiLaunchRequest(request);

        final String userId = (String) session.getAttribute("user_id");
        if (userId == null) {
            throw new UnauthorizedAccessException("No user_id found");
        }

        LtiAccountDb ltiAccountDb = new LtiAccountDb();
        final LtiAccountAttributes ltiAccountAttributes = ltiAccountDb.getAccount(userId);

        String instructorShortName = getRequestParamValue("name").trim();
        String instructorName = getRequestParamValue("name").trim();
        String instructorEmail = getRequestParamValue("email").trim();
        String instructorInstitution = getRequestParamValue("institution").trim();

        try {
            // TODO just pass the whole data? nah let's not do that
            logic.verifyInputForAdminHomePage(instructorShortName, instructorName,
                                              instructorInstitution, instructorEmail);
        } catch (InvalidParametersException e) {
            throw new UnauthorizedAccessException(e.getMessage());
        }

        String courseId;

        try {
            courseId = adminAddInstructorLogic.createCourseInstructorWithDemoData(instructorEmail, instructorName);
        } catch (InvalidParametersException | EntityDoesNotExistException e) {
            throw new UnauthorizedAccessException(e.getMessage());
        }

        // TODO:
        // one instructor per course should only exist, but this should be designed in the schema properly
        // instead of getting a list
        // if we never get around to refactoring this, we should have a method that retrieves the instructor only
        // with the appropriate warnings
        List<InstructorAttributes> instructorAttributesList = logic.getInstructorsForCourse(courseId);
        final InstructorAttributes instructorAttributes = instructorAttributesList.get(0);

        final String encryptedRegkey = StringHelper.encrypt(instructorAttributes.key);
        ltiAccountDb.updateAccountWithRegistrationKey(userId, encryptedRegkey);
        session.setAttribute("regkey", encryptedRegkey);
        // set attribute for institute?

        String joinLink = createJoinLinkFromInstructorRegKey(instructorAttributes.key, instructorInstitution);

        createInstructorCreationSuccessExecutionStatus(instructorName, instructorEmail, instructorInstitution);

        return createRedirectResult(joinLink);
    }

    private String createJoinLinkFromInstructorRegKey(String instructorRegKey, String instructorInstitution) {
        return Config.getAppUrl(Const.ActionURIs.INSTRUCTOR_COURSE_JOIN)
                .withRegistrationKey(StringHelper.encrypt(instructorRegKey))
                .withInstructorInstitution(instructorInstitution)
                .toAbsoluteString();
    }

    private void createInstructorCreationSuccessExecutionStatus(String instructorName, String instructorEmail,
                                                                String instructorInstitution) {
        //statusToUser.add(new StatusMessage(data.statusForAjax, StatusMessageColor.SUCCESS));
        statusToAdmin = "A New LTI Instructor <span class=\"bold\">"
                + SanitizationHelper.sanitizeForHtmlTag(instructorName) + "</span> has been created.<br>"
                + "<span class=\"bold\">Id: </span>"
                + "ID will be assigned when the verification link was clicked and confirmed"
                + "<br>"
                + "<span class=\"bold\">Email: </span>" + SanitizationHelper.sanitizeForHtmlTag(instructorEmail)
                + "<span class=\"bold\">Institution: </span>"
                + SanitizationHelper.sanitizeForHtmlTag(instructorInstitution);
    }
}
