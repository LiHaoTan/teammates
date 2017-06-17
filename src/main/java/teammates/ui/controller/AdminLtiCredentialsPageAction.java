package teammates.ui.controller;

import teammates.common.util.Const;
import teammates.ui.pagedata.AdminLtiCredentialsPageData;

public class AdminLtiCredentialsPageAction extends Action {

    @Override
    protected ActionResult execute() {

        gateKeeper.verifyAdminPrivileges(account);

        AdminLtiCredentialsPageData data = new AdminLtiCredentialsPageData(account, sessionToken);

        data.instructorShortName = "";
        data.instructorName = "";
        data.instructorEmail = "";
        data.instructorInstitution = "";
        data.instructorDetailsSingleLine = "";

        statusToAdmin = "Admin Lti Credentials Page Load";

        return createShowPageResult(Const.ViewURIs.ADMIN_LTI_CREDENTIALS, data);
    }

}
