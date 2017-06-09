package teammates.logic.core;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.InstructorSearchResultBundle;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Assumption;
import teammates.common.util.Config;
import teammates.common.util.FieldValidator;
import teammates.common.util.JsonUtils;
import teammates.common.util.Logger;
import teammates.common.util.StringHelper;
import teammates.common.util.Templates;
import teammates.common.util.ThreadHelper;
import teammates.logic.api.Logic;
import teammates.logic.backdoor.BackDoorLogic;
import teammates.storage.api.InstructorsDb;
import teammates.ui.pagedata.AdminHomePageData;

/**
 * Handles operations related to instructors.
 *
 * @see InstructorAttributes
 * @see InstructorsDb
 */
public final class AdminAddInstructorLogic {

    private static final Logger log = Logger.getLogger();

    private static AdminAddInstructorLogic instance = new AdminAddInstructorLogic();

    private static final InstructorsDb instructorsDb = new InstructorsDb();

    private static final AccountsLogic accountsLogic = AccountsLogic.inst();
    private static final CoursesLogic coursesLogic = CoursesLogic.inst();
    private static final FeedbackResponseCommentsLogic frcLogic = FeedbackResponseCommentsLogic.inst();
    private static final FeedbackSessionsLogic fsLogic = FeedbackSessionsLogic.inst();

    private static final StudentsLogic studentsLogic = StudentsLogic.inst();
    private static final InstructorsLogic instructorsLogic = InstructorsLogic.inst();

    private AdminAddInstructorLogic() {
        // prevent initialization
    }

    public static AdminAddInstructorLogic inst() {
        return instance;
    }

    /**
     * Imports Demo course to new instructor.
     * @param pageData data from AdminHomePageData
     * @return the ID of Demo course
     */
    public String createDemoDataForCourseInstructor(AdminHomePageData pageData)
            throws InvalidParametersException, EntityDoesNotExistException {
        String courseId = generateDemoCourseId(pageData.instructorEmail);

        DataBundle demoDataBundle = createDemoDataBundle(courseId, pageData);

        persistDataBundle(demoDataBundle);

        createAndPutDocuments(pageData, courseId);

        return courseId;
    }

    /**
     * Generate a course ID for demo course, and if the generated id already exists, try another one.
     *
     * @param instructorEmail is the instructor email.
     * @return generated course id
     */
    private String generateDemoCourseId(String instructorEmail) {
        String proposedCourseId = generateNextDemoCourseId(instructorEmail, FieldValidator.COURSE_ID_MAX_LENGTH);
        // coursesLogic
        while (coursesLogic.getCourse(proposedCourseId) != null) {
            proposedCourseId = generateNextDemoCourseId(proposedCourseId, FieldValidator.COURSE_ID_MAX_LENGTH);
        }
        return proposedCourseId;
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

    private void persistDataBundle(DataBundle dataBundle) throws InvalidParametersException, EntityDoesNotExistException {
        // although BackDoorLogic is not for production use, it is more convenient to use it here, or should we not use
        // it?
        BackDoorLogic backDoorLogic = new BackDoorLogic();

        try {
            backDoorLogic.persistDataBundle(dataBundle);
        } catch (EntityDoesNotExistException e) {
            ThreadHelper.waitFor(Config.PERSISTENCE_CHECK_DURATION);
            backDoorLogic.persistDataBundle(dataBundle);
            log.warning("Data Persistence was Checked Twice in This Request");
        }
    }

    /**
     * Create and put documents for the demo data into the Index for use in App Engine's Search API
     */
    private void createAndPutDocuments(AdminHomePageData pageData, String courseId) {
        List<FeedbackResponseCommentAttributes> frComments =
                frcLogic.getFeedbackResponseCommentsForGiver(courseId, pageData.instructorEmail);
        List<StudentAttributes> students = studentsLogic.getStudentsForCourse(courseId);
        List<InstructorAttributes> instructors = instructorsLogic.getInstructorsForCourse(courseId);

        frcLogic.putDocuments(frComments);
        studentsLogic.putDocuments(students);
        instructorsLogic.putDocuments(instructors);
    }







    /* ====================================
     * methods related to google search API
     * ====================================
     */

    /**
     * Creates or updates document for the given Instructor.
     * @param instructor to be put into documents
     */
    public void putDocument(InstructorAttributes instructor) {
        instructorsDb.putDocument(instructor);
    }

    /**
     * Batch creates or updates documents for the given Instructors.
     * @param instructors a list of instructors to be put into documents
     */
    public void putDocuments(List<InstructorAttributes> instructors) {
        instructorsDb.putDocuments(instructors);
    }

    /**
     * Removes document for the given Instructor.
     * @param instructor to be removed from documents
     */
    public void deleteDocument(InstructorAttributes instructor) {
        instructorsDb.deleteDocument(instructor);
    }

    /**
     * This method should be used by admin only since the searching does not restrict the
     * visibility according to the logged-in user's google ID. This is used by admin to
     * search instructors in the whole system.
     * @return null if no result found
     */
    public InstructorSearchResultBundle searchInstructorsInWholeSystem(String queryString) {
        return instructorsDb.searchInstructorsInWholeSystem(queryString);
    }

    /* ====================================
     * ====================================
     */

    public InstructorAttributes createInstructor(InstructorAttributes instructorToAdd)
            throws InvalidParametersException, EntityAlreadyExistsException {

        Assumption.assertNotNull("Supplied parameter was null", instructorToAdd);

        log.info("going to create instructor :\n" + instructorToAdd.toString());

        return instructorsDb.createInstructor(instructorToAdd);
    }

    public void setArchiveStatusOfInstructor(String googleId, String courseId, boolean archiveStatus)
           throws InvalidParametersException, EntityDoesNotExistException {

        InstructorAttributes instructor = instructorsDb.getInstructorForGoogleId(courseId, googleId);
        instructor.isArchived = archiveStatus;
        instructorsDb.updateInstructorByGoogleId(instructor);
    }

    public InstructorAttributes getInstructorForEmail(String courseId, String email) {

        return instructorsDb.getInstructorForEmail(courseId, email);
    }

    public InstructorAttributes getInstructorForGoogleId(String courseId, String googleId) {

        return instructorsDb.getInstructorForGoogleId(courseId, googleId);
    }

    public InstructorAttributes getInstructorForRegistrationKey(String encryptedKey) {

        return instructorsDb.getInstructorForRegistrationKey(encryptedKey);
    }

    public List<InstructorAttributes> getInstructorsForCourse(String courseId) {

        return instructorsDb.getInstructorsForCourse(courseId);
    }

    public List<InstructorAttributes> getInstructorsForGoogleId(String googleId) {

        return getInstructorsForGoogleId(googleId, false);
    }

    public List<InstructorAttributes> getInstructorsForGoogleId(String googleId, boolean omitArchived) {

        return instructorsDb.getInstructorsForGoogleId(googleId, omitArchived);
    }

    public String getEncryptedKeyForInstructor(String courseId, String email)
            throws EntityDoesNotExistException {

        verifyIsEmailOfInstructorOfCourse(email, courseId);

        InstructorAttributes instructor = getInstructorForEmail(courseId, email);

        return StringHelper.encrypt(instructor.key);
    }

    public List<InstructorAttributes> getInstructorsForEmail(String email) {

        return instructorsDb.getInstructorsForEmail(email);
    }

    /**
     * Gets all instructors in the Datastore.
     *
     * @deprecated Not scalable. Use only for admin features.
     */
    @Deprecated
    public List<InstructorAttributes> getAllInstructors() {

        return instructorsDb.getAllInstructors();
    }

    public boolean isGoogleIdOfInstructorOfCourse(String instructorId, String courseId) {

        return instructorsDb.getInstructorForGoogleId(courseId, instructorId) != null;
    }

    public boolean isEmailOfInstructorOfCourse(String instructorEmail, String courseId) {

        return instructorsDb.getInstructorForEmail(courseId, instructorEmail) != null;
    }

    /**
     * Returns whether the instructor is a new user, according to one of the following criteria:
     * <ul>
     * <li>There is only a sample course (created by system) for the instructor.</li>
     * <li>There is no any course for the instructor.</li>
     * </ul>
     */
    public boolean isNewInstructor(String googleId) {
        List<InstructorAttributes> instructorList = getInstructorsForGoogleId(googleId);
        return instructorList.isEmpty()
               || instructorList.size() == 1 && coursesLogic.isSampleCourse(instructorList.get(0).courseId);
    }

    public void verifyInstructorExists(String instructorId)
            throws EntityDoesNotExistException {

        if (!accountsLogic.isAccountAnInstructor(instructorId)) {
            throw new EntityDoesNotExistException("Instructor does not exist :"
                    + instructorId);
        }
    }

    public void verifyIsEmailOfInstructorOfCourse(String instructorEmail, String courseId)
            throws EntityDoesNotExistException {

        if (!isEmailOfInstructorOfCourse(instructorEmail, courseId)) {
            throw new EntityDoesNotExistException("Instructor " + instructorEmail
                    + " does not belong to course " + courseId);
        }
    }

    /**
     * Update the name and email address of an instructor with the specific Google ID.
     * @param instructor InstructorAttributes object containing the details to be updated
     */
    public void updateInstructorByGoogleId(String googleId, InstructorAttributes instructor)
            throws InvalidParametersException, EntityDoesNotExistException {

        // TODO: either refactor this to constant or just remove it. check not null should be in db
        Assumption.assertNotNull("Supplied parameter was null", instructor);

        coursesLogic.verifyCourseIsPresent(instructor.courseId);
        verifyInstructorInDbAndCascadeEmailChange(googleId, instructor);
        checkForUpdatingRespondents(instructor);

        instructorsDb.updateInstructorByGoogleId(instructor);
    }

    private void checkForUpdatingRespondents(InstructorAttributes instructor)
            throws InvalidParametersException, EntityDoesNotExistException {

        InstructorAttributes currentInstructor = getInstructorForGoogleId(instructor.courseId, instructor.googleId);
        if (!currentInstructor.email.equals(instructor.email)) {
            fsLogic.updateRespondentsForInstructor(currentInstructor.email, instructor.email, instructor.courseId);
        }
    }

    private void verifyInstructorInDbAndCascadeEmailChange(String googleId,
            InstructorAttributes instructor) throws EntityDoesNotExistException {
        InstructorAttributes instructorInDb = instructorsDb.getInstructorForGoogleId(instructor.courseId, googleId);
        if (instructorInDb == null) {
            throw new EntityDoesNotExistException("Instructor " + googleId
                    + " does not belong to course " + instructor.courseId);
        }
        // cascade comments
        if (!instructorInDb.email.equals(instructor.email)) {
            frcLogic.updateFeedbackResponseCommentsEmails(
                    instructor.courseId, instructorInDb.email, instructor.email);
        }
    }

    /**
     * Update the Google ID and name of an instructor with the specific email.
     * @param instructor InstructorAttributes object containing the details to be updated
     */
    public void updateInstructorByEmail(String email, InstructorAttributes instructor)
            throws InvalidParametersException, EntityDoesNotExistException {

        Assumption.assertNotNull("Supplied parameter was null", instructor);

        coursesLogic.verifyCourseIsPresent(instructor.courseId);
        verifyIsEmailOfInstructorOfCourse(email, instructor.courseId);

        instructorsDb.updateInstructorByEmail(instructor);
    }

    public List<String> getInvalidityInfoForNewInstructorData(String shortName, String name,
                                                              String institute, String email) {

        FieldValidator validator = new FieldValidator();
        List<String> errors = new ArrayList<String>();
        String error;

        error = validator.getInvalidityInfoForPersonName(shortName);
        if (!error.isEmpty()) {
            errors.add(error);
        }

        error = validator.getInvalidityInfoForPersonName(name);
        if (!error.isEmpty()) {
            errors.add(error);
        }

        error = validator.getInvalidityInfoForEmail(email);
        if (!error.isEmpty()) {
            errors.add(error);
        }

        error = validator.getInvalidityInfoForInstituteName(institute);
        if (!error.isEmpty()) {
            errors.add(error);
        }

        //No validation for isInstructor and createdAt fields.
        return errors;
    }

    public void deleteInstructorCascade(String courseId, String email) {
        fsLogic.deleteInstructorFromRespondentsList(getInstructorForEmail(courseId, email));
        instructorsDb.deleteInstructor(courseId, email);
    }

    public void deleteInstructorsForGoogleIdAndCascade(String googleId) {
        List<InstructorAttributes> instructors = instructorsDb.getInstructorsForGoogleId(googleId, false);

        //Cascade delete instructors
        for (InstructorAttributes instructor : instructors) {
            deleteInstructorCascade(instructor.courseId, instructor.email);
        }
    }

    // this method is only being used in course logic. cascade to comments is therefore not necessary
    // as it it taken care of when deleting course
    public void deleteInstructorsForCourse(String courseId) {

        instructorsDb.deleteInstructorsForCourse(courseId);
    }

}
