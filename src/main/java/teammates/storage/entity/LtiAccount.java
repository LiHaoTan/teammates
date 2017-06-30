package teammates.storage.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * Represents a tool consumer mappping of {@code user_id} to a user's {@code regkey}.
 */
@Entity
@Index
public class LtiAccount extends BaseEntity {

    /** The {@code user_id} parameter from the tool consumer that uniquely identifies the user. */
    @Id
    private String userId;

    /** The unique registration key used to identify the user. */
    private String regkey;

    /** The {@code googleId} associated associated to the user. */
    private String googleId;

    @SuppressWarnings("unused") // required by Objectify
    private LtiAccount() {
    }

    public LtiAccount(String userId, String regkey, String googleId) {
        this.userId = userId;
        this.googleId = googleId;
        this.regkey = regkey;
    }

    public String getUserId() {
        return userId;
    }

    public String getRegkey() {
        return regkey;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setRegkey(String regkey) {
        this.regkey = regkey;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }
}
