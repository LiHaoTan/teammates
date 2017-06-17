package teammates.storage.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * Represents a LtiOAuthCredential in the system.
 */
@Entity
@Index
public class LtiOAuthCredential extends BaseEntity {

    @Id
    private String consumerKey;

    private String consumerSecret;

    @SuppressWarnings("unused") // required by Objectify
    private LtiOAuthCredential() {
    }

    /**
     * Instantiates a new LtiOAuthCredential.
     *
     * @param consumerKey
     *            the consumer key of the user. Can be randomly generated or DNS
     * @param consumerSecret
     *            The shared secret used for signing requests.
     */
    public LtiOAuthCredential(String consumerKey, String consumerSecret) {
        setConsumerKey(consumerKey);
        setConsumerSecret(consumerSecret);
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String oAuthConsumerKey) {
        this.consumerKey = oAuthConsumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

}
