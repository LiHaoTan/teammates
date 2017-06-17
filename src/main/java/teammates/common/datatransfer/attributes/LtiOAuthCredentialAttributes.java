package teammates.common.datatransfer.attributes;

import java.util.ArrayList;
import java.util.List;

import teammates.common.util.FieldValidator;
import teammates.common.util.JsonUtils;
import teammates.storage.entity.LtiOAuthCredential;

/**
 * A data transfer object for LtiOAuthCredential entities.
 */
public class LtiOAuthCredentialAttributes extends EntityAttributes<LtiOAuthCredential> {

    //Note: be careful when changing these variables as their names are used in *.json files.

    private String consumerKey;

    private String consumerSecret;

    public LtiOAuthCredentialAttributes(LtiOAuthCredential ltiOAuthCredential) {
        consumerKey = ltiOAuthCredential.getConsumerKey();
        consumerSecret = ltiOAuthCredential.getConsumerSecret();
    }

    public LtiOAuthCredentialAttributes() {
        // attributes to be set after construction
    }

    public LtiOAuthCredentialAttributes(String consumerKey, String consumerSecret) {
        // maybe we should sanitize?
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
    }

    /**
     * Gets a deep copy of this object.
     */
    public LtiOAuthCredentialAttributes getCopy() {
        return new LtiOAuthCredentialAttributes(consumerKey, consumerSecret);
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    @Override
    public List<String> getInvalidityInfo() {
        FieldValidator validator = new FieldValidator();
        @SuppressWarnings("UnnecessaryLocalVariable")
        List<String> errors = new ArrayList<String>();

        // TODO: not sure if we should have validation
        //addNonEmptyError(validator.getInvalidityInfoForConsumerKey(consumerKey), errors);
        //addNonEmptyError(validator.getInvalidityInfoForSecret(consumerKey), errors);

        return errors;
    }

    @Override
    public LtiOAuthCredential toEntity() {
        return new LtiOAuthCredential(consumerKey, consumerSecret);
    }

    @Override
    public String toString() {
        return JsonUtils.toJson(this, LtiOAuthCredentialAttributes.class);
    }

    @Override
    public String getIdentificationString() {
        return consumerKey;
    }

    @Override
    public String getEntityTypeAsString() {
        return LtiOAuthCredential.class.getName();
    }

    @Override
    public String getBackupIdentifier() {
        return LtiOAuthCredential.class.getName();
    }

    @Override
    public String getJsonString() {
        return JsonUtils.toJson(this, LtiOAuthCredentialAttributes.class);
    }

    @Override
    public void sanitizeForSaving() {
        // TODO: not sure if we need to sanitize anything here
        // maybe sanitize consumerKey spaces
        // and sanitizeTextField, sanitizeHtml
    }

}
