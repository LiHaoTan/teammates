package teammates.common.datatransfer.attributes;

import java.util.ArrayList;
import java.util.List;

import teammates.common.util.JsonUtils;
import teammates.storage.entity.LtiAccount;

/**
 * A data transfer object for {@link LtiAccount} entities.
 */
public class LtiAccountAttributes extends EntityAttributes<LtiAccount> {

    //Note: be careful when changing these variables as their names are used in *.json files.

    /** Corresponds to the entity property {@link LtiAccount#userId}. */
    private String userId;

    /** Corresponds to the entity property {@link LtiAccount#regkey}. */
    private String regkey;

    /** Corresponds to the entity property {@link LtiAccount#googleId}. */
    private String googleId;

    public LtiAccountAttributes(LtiAccount ltiAccount) {
        userId = ltiAccount.getUserId();
        regkey = ltiAccount.getRegkey();
        googleId = ltiAccount.getGoogleId();
    }

    public LtiAccountAttributes(String userId, String regkey, String googleId) {
        this.userId = userId;
        this.regkey = regkey;
        this.googleId = googleId;
    }

    public LtiAccountAttributes(String userId) {
        this.userId = userId;
    }

    /**
     * Gets a deep copy of this object.
     */
    public LtiAccountAttributes getCopy() {
        return new LtiAccountAttributes(userId, regkey, googleId);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getRegkey() {
        return regkey;
    }

    public void setRegkey(String regkey) {
        this.regkey = regkey;
    }

    @Override
    public List<String> getInvalidityInfo() {
        // No validation is required for the attributes
        return new ArrayList<>();
    }

    @Override
    public LtiAccount toEntity() {
        return new LtiAccount(userId, regkey, googleId);
    }

    @Override
    public String toString() {
        return JsonUtils.toJson(this, LtiAccountAttributes.class);
    }

    @Override
    public String getIdentificationString() {
        return userId;
    }

    @Override
    public String getEntityTypeAsString() {
        return LtiAccount.class.getSimpleName();
    }

    @Override
    public String getBackupIdentifier() {
        return LtiAccount.class.getSimpleName();
    }

    @Override
    public String getJsonString() {
        return JsonUtils.toJson(this, LtiAccountAttributes.class);
    }

    @Override
    public void sanitizeForSaving() {
        // TODO: sanitize for saving is not required, method can be removed after all sanitizeForSaving is removed
    }

}
