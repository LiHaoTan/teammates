package teammates.logic.core;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Config;
import teammates.common.util.FieldValidator;
import teammates.common.util.JsonUtils;
import teammates.common.util.Logger;
import teammates.common.util.StringHelper;
import teammates.common.util.Templates;
import teammates.common.util.ThreadHelper;
import teammates.logic.backdoor.BackDoorLogic;

/**
 * Handles operations related to adding instructors.
 * TODO further refining of below
 * @see teammates.ui.controller.AdminInstructorAccountAddAction
 * @see teammates.ui.controller.LtiServlet
 */
public final class AdminAddInstructorLogic {

    private static final Logger log = Logger.getLogger();

    private static AdminAddInstructorLogic instance = new AdminAddInstructorLogic();

    private static final CoursesLogic coursesLogic = CoursesLogic.inst();

    private static final FeedbackResponseCommentsLogic frcLogic = FeedbackResponseCommentsLogic.inst();
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
     * @return the ID of Demo course
     */
    public String createCourseInstructorWithDemoData(String instructorEmail, String instructorName)
            throws InvalidParametersException, EntityDoesNotExistException {
        String courseId = generateDemoCourseId(instructorEmail);

        DataBundle demoDataBundle = createDemoDataBundle(courseId, instructorEmail, instructorName);

        persistDataBundle(demoDataBundle);

        createAndPutDocuments(courseId, instructorEmail);

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
     *         TODO documentation seems to be wrong here, should be cut from front not in the center
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
        // TODO should not use the magic number 5 here
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

    private DataBundle createDemoDataBundle(String courseId, String instructorEmail, String instructorName) {
        DateTime demoFeedbackSessionEndDateTime = new DateTime(DateTimeZone.UTC)
                .withHourOfDay(23).withMinuteOfHour(59).plusYears(1);
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm a z");

        String demoDataBundleJsonString = Templates.populateTemplate(Templates.INSTRUCTOR_SAMPLE_DATA,
                // replace email
                "teammates.demo.instructor@demo.course", instructorEmail,
                // replace name
                "Demo_Instructor", instructorName,
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

        backDoorLogic.persistDataBundle(dataBundle);
    }

    /**
     * Create and put documents for the demo data into the Index for use in App Engine's Search API.
     */
    private void createAndPutDocuments(String courseId, String instructorEmail) {
        List<FeedbackResponseCommentAttributes> frComments =
                frcLogic.getFeedbackResponseCommentsForGiver(courseId, instructorEmail);
        List<StudentAttributes> students = studentsLogic.getStudentsForCourse(courseId);
        List<InstructorAttributes> instructors = instructorsLogic.getInstructorsForCourse(courseId);

        frcLogic.putDocuments(frComments);
        studentsLogic.putDocuments(students);
        instructorsLogic.putDocuments(instructors);
    }
}
