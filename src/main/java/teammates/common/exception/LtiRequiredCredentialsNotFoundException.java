package teammates.common.exception;

@SuppressWarnings("serial")
public class LtiRequiredCredentialsNotFoundException extends Exception {
    public LtiRequiredCredentialsNotFoundException(String message) {
        super(message);
    }
}
