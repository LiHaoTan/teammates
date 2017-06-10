package teammates.ui.controller;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.EmailSendingException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.exception.TeammatesException;
import teammates.common.util.Config;
import teammates.common.util.Const;
import teammates.common.util.EmailWrapper;
import teammates.common.util.FieldValidator;
import teammates.common.util.JsonUtils;
import teammates.common.util.Logger;
import teammates.common.util.SanitizationHelper;
import teammates.common.util.StatusMessage;
import teammates.common.util.StatusMessageColor;
import teammates.common.util.StringHelper;
import teammates.common.util.Templates;
import teammates.common.util.Url;
import teammates.logic.api.EmailGenerator;
import teammates.logic.backdoor.BackDoorLogic;
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

        // TODO START1 maybe create a whole method around this

        // If there is input from the form in adminCreateInstructorAccountWithOneBoxForm.tag,
        // it will be prioritized over the data from the adminCreateInstructorAccountForm.tag
        if (hasInstructorDetailsSingleLineStringFromSingleBox()) {
            // because it is sent line by line, not sure if this should be refactored
            data.instructorDetailsSingleLine = getRequestParamValue(Const.ParamsNames.INSTRUCTOR_DETAILS_SINGLE_LINE);
            try {
                createInstructorDataFromOneBox(data);
            } catch (InvalidParametersException e) {
                data.instructorShortName = "";
                data.instructorName = "";
                data.instructorEmail = "";
                data.instructorInstitution = "";
                return createInstructorCreationFailAjaxResult(data, e);
            }
        } else {
            createInstructorDataFromMultiParameterAccountForm(data);
        }

        trimInstructorData(data);

        // END1 end here

        try {
            // TODO just pass the whole data?
            logic.verifyInputForAdminHomePage(data.instructorShortName, data.instructorName,
                                              data.instructorInstitution, data.instructorEmail);
        } catch (InvalidParametersException e) {
            return createInstructorCreationFailAjaxResult(data, e);
        }

        String courseId;

        try {
            courseId = importDemoData(data);
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
                instructorAttributes.email, data.instructorShortName, joinLink);

        // TODO sending email takes up quite a lot of time, however this may not be of as much concern
        // since there is only one admin though it should be sent to a queue instead
        try {
            emailSender.sendEmail(email);
        } catch (EmailSendingException e) {
            log.severe("Instructor welcome email failed to send: " + TeammatesException.toStringWithStackTrace(e));
        }

        data.isInstructorAddingResultForAjax = true;
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
                             + " has been successfully created with join link:<br>" + joinLink;
    }

    /**
     * Creates an AJAX result with a link to retry import data failure
     */
    private ActionResult createInstructorImportRetryAjaxResult(AdminHomePageData data, Exception e) {
        String retryUrl = Const.ActionURIs.ADMIN_INSTRUCTORACCOUNT_ADD;
        retryUrl = Url.addParamToUrl(retryUrl, Const.ParamsNames.INSTRUCTOR_SHORT_NAME, data.instructorShortName);
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

    private void trimInstructorData(AdminHomePageData data) {
        data.instructorShortName = data.instructorShortName.trim();
        data.instructorName = data.instructorName.trim();
        data.instructorEmail = data.instructorEmail.trim();
        data.instructorInstitution = data.instructorInstitution.trim();
    }

    private ActionResult createInstructorCreationFailAjaxResult(AdminHomePageData data, InvalidParametersException e) {
        data.statusForAjax = e.getMessage().replace(Const.EOL, Const.HTML_BR_TAG);
        data.isInstructorAddingResultForAjax = false;
        statusToUser.add(new StatusMessage(data.statusForAjax, StatusMessageColor.DANGER));
        return createAjaxResult(data);
    }

    private void createInstructorDataFromMultiParameterAccountForm(AdminHomePageData data) {
        data.instructorShortName = getNonNullRequestParamValue(Const.ParamsNames.INSTRUCTOR_SHORT_NAME);
        data.instructorName = getNonNullRequestParamValue(Const.ParamsNames.INSTRUCTOR_NAME);
        data.instructorEmail = getNonNullRequestParamValue(Const.ParamsNames.INSTRUCTOR_EMAIL);
        data.instructorInstitution = getNonNullRequestParamValue(Const.ParamsNames.INSTRUCTOR_INSTITUTION);
    }

    private void createInstructorDataFromOneBox(AdminHomePageData data) throws InvalidParametersException {
        String[] instructorInfo = extractInstructorInfo(data.instructorDetailsSingleLine);

        data.instructorShortName = instructorInfo[0];
        data.instructorName = instructorInfo[0];
        data.instructorEmail = instructorInfo[1];
        data.instructorInstitution = instructorInfo[2];
    }

    /**
     * Returns if the request has the instructor details on a single line, this should be sent from adminHome.es6
     * recursively if there is more than one line.
     */
    private boolean hasInstructorDetailsSingleLineStringFromSingleBox() {
        // TODO this is not a good check as we actually have to retrieve the data
        // in order to know if the data exists, and would result in
        // the param value being retrieve twice, once here, and later again when we want the data
        // However, some significant refactoring might be needed to be done if we were to need to do it another way
        // of course we could also skip the boolean check and check it in another place, but it would not be clear that
        // a null check means that the data is missing
        return getRequestParamValue(Const.ParamsNames.INSTRUCTOR_DETAILS_SINGLE_LINE) != null;
    }

    /**
     * Extracts instructor's info from a string then store them in an array of string.
     * @param instructorDetails
     *         This string is in the format INSTRUCTOR_NAME | INSTRUCTOR_EMAIL | INSTRUCTOR_INSTITUTION
     *         or INSTRUCTOR_NAME \t INSTRUCTOR_EMAIL \t INSTRUCTOR_INSTITUTION
     * @return A String array of size 3
     */
    private String[] extractInstructorInfo(String instructorDetails) throws InvalidParametersException {
        String[] result = instructorDetails.trim().replace('|', '\t').split("\t");
        if (result.length != Const.LENGTH_FOR_NAME_EMAIL_INSTITUTION) {
            throw new InvalidParametersException(String.format(Const.StatusMessages.INSTRUCTOR_DETAILS_LENGTH_INVALID,
                                                               Const.LENGTH_FOR_NAME_EMAIL_INSTITUTION));
        }
        return result;
    }

    /**
     * Imports Demo course to new instructor.
     * @param pageData data from AdminHomePageData
     * @return the ID of Demo course
     */
    private String importDemoData(AdminHomePageData pageData)
            throws InvalidParametersException, EntityDoesNotExistException {
        String courseId = generateDemoCourseId(pageData.instructorEmail);

        DataBundle demoDataBundle = createDemoDataBundle(courseId, pageData);

        persistDataBundle(demoDataBundle);

        createAndPutDocuments(pageData, courseId);

        return courseId;
    }

    /**
     * Create and put documents for the demo data into the Index for use in App Engine's Search API
     */
    private void createAndPutDocuments(AdminHomePageData pageData, String courseId) {
        List<FeedbackResponseCommentAttributes> frComments =
                logic.getFeedbackResponseCommentForGiver(courseId, pageData.instructorEmail);
        List<StudentAttributes> students = logic.getStudentsForCourse(courseId);
        List<InstructorAttributes> instructors = logic.getInstructorsForCourse(courseId);

        logic.putFeedbackResponseCommentDocuments(frComments);
        logic.putStudentDocuments(students);
        logic.putInstructorDocuments(instructors);
    }

    private void persistDataBundle(DataBundle dataBundle) throws InvalidParametersException, EntityDoesNotExistException {
        // although BackDoorLogic is not for production use, it is more convenient to use it here, or should we not use
        // it?
        BackDoorLogic backDoorLogic = new BackDoorLogic();
        backDoorLogic.persistDataBundle(dataBundle);
    }

    private DataBundle createDemoDataBundle(String courseId, AdminHomePageData pageData) {
        DateTime demoFeedbackSessionEndDateTime = new DateTime(DateTimeZone.UTC)
                .withHourOfDay(23).withMinuteOfHour(59).plusYears(1);
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm a z");

        String demoDataBundleJsonString = Templates.populateTemplate(Templates.INSTRUCTOR_SAMPLE_DATA,
                // replace email
                "teammates.demo.instructor@demo.course", pageData.instructorEmail,
                // replace name
                "Demo_Instructor", pageData.instructorName,
                // replace course
                "demo.course", courseId,
                // update feedback session time
                "2013-04-01 11:59 PM UTC", demoFeedbackSessionEndDateTime.toString(dateTimeFormatter));

        return JsonUtils.fromJson(demoDataBundleJsonString, DataBundle.class);
    }

    // Strategy to Generate New Demo Course Id:
    // a. keep the part of email before "@"
    //    replace "@" with "."
    //    replace email host with their first 3 chars. eg, gmail.com -> gma
    //    append "-demo"
    //    to sum up: lebron@gmail.com -> lebron.gma-demo
    //
    // b. if the generated courseId already exists, create another one by appending a integer to the previous courseId.
    //    if the newly generate id still exists, increment the id, until we find a feasible one
    //    eg.
    //    lebron@gmail.com -> lebron.gma-demo  // already exists!
    //    lebron@gmail.com -> lebron.gma-demo0 // already exists!
    //    lebron@gmail.com -> lebron.gma-demo1 // already exists!
    //    ...
    //    lebron@gmail.com -> lebron.gma-demo99 // already exists!
    //    lebron@gmail.com -> lebron.gma-demo100 // found! a feasible id
    //
    // c. in any cases(a or b), if generated Id is longer than FieldValidator.COURSE_ID_MAX_LENGTH, shorten the part
    //    before "@" of the intial input email, by continuously remove its last character

    /**
     * Generate a course ID for demo course, and if the generated id already exists, try another one.
     *
     * @param instructorEmail is the instructor email.
     * @return generated course id
     */
    private String generateDemoCourseId(String instructorEmail) {
        String proposedCourseId = generateNextDemoCourseId(instructorEmail, FieldValidator.COURSE_ID_MAX_LENGTH);
        while (logic.getCourse(proposedCourseId) != null) {
            proposedCourseId = generateNextDemoCourseId(proposedCourseId, FieldValidator.COURSE_ID_MAX_LENGTH);
        }
        return proposedCourseId;
    }

    /**
     * Generate a course ID for demo course from a given email.
     *
     * @param instructorEmail is the instructor email.
     * @return the first proposed course id. eg.lebron@gmail.com -> lebron.gma-demo
     */
    private String getDemoCourseIdRoot(String instructorEmail) {
        String[] emailSplit = instructorEmail.split("@");

        String username = emailSplit[0];
        String host = emailSplit[1];

        String head = StringHelper.replaceIllegalChars(username, FieldValidator.REGEX_COURSE_ID, '_');
        String hostAbbreviation = host.substring(0, 3);

        return head + "." + hostAbbreviation + "-demo";
    }

    /**
     * Generate a course ID for demo course from a given email or a generated course Id.
     *
     * <p>Here we check the input string is an email or course Id and handle them accordingly;
     * check the resulting course id, and if bigger than maximumIdLength, cut it so that it equals maximumIdLength.
     *
     * @param instructorEmailOrProposedCourseId is the instructor email or a proposed course id that already exists.
     * @param maximumIdLength is the maximum resulting id length allowed, above which we will cut the part before "@"
     * @return the proposed course id, e.g.:
     *         <ul>
     *         <li>lebron@gmail.com -> lebron.gma-demo</li>
     *         <li>lebron.gma-demo -> lebron.gma-demo0</li>
     *         <li>lebron.gma-demo0 -> lebron.gma-demo1</li>
     *         <li>012345678901234567890123456789.gma-demo9 -> 01234567890123456789012345678.gma-demo10 (being cut)</li>
     *         </ul>
     */
    private String generateNextDemoCourseId(String instructorEmailOrProposedCourseId, int maximumIdLength) {
        final boolean isFirstCourseId = instructorEmailOrProposedCourseId.contains("@");
        if (isFirstCourseId) {
            return StringHelper.truncateHead(getDemoCourseIdRoot(instructorEmailOrProposedCourseId),
                                             maximumIdLength);
        }

        final boolean isFirstTimeDuplicate = instructorEmailOrProposedCourseId.endsWith("-demo");
        if (isFirstTimeDuplicate) {
            return StringHelper.truncateHead(instructorEmailOrProposedCourseId + "0",
                                             maximumIdLength);
        }

        final int lastIndexOfDemo = instructorEmailOrProposedCourseId.lastIndexOf("-demo");
        final String root = instructorEmailOrProposedCourseId.substring(0, lastIndexOfDemo);
        final int previousDedupSuffix = Integer.parseInt(instructorEmailOrProposedCourseId.substring(lastIndexOfDemo + 5));

        return StringHelper.truncateHead(root + "-demo" + (previousDedupSuffix + 1), maximumIdLength);
    }
}
