package teammates.storage.api;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.QueryKeys;

import teammates.common.datatransfer.attributes.LtiOAuthCredentialAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.storage.entity.LtiOAuthCredential;

/**
 * Handles CRUD operations for LTI OAuth Credentials.
 *
 * @see LtiOAuthCredential
 * @see LtiOAuthCredentialAttributes
 */
public class LtiOAuthCredentialDb extends EntitiesDb<LtiOAuthCredential, LtiOAuthCredentialAttributes> {
    /**
     * Preconditions:
     * <br> * {@code ltiOAuthCredentialAttributes} is not null and has valid data.
     */
    public void createCredential(LtiOAuthCredentialAttributes ltiOAuthCredentialAttributes)
            throws InvalidParametersException {
        try {
            createEntity(ltiOAuthCredentialAttributes);
        } catch (EntityAlreadyExistsException e) {
            throw new InvalidParametersException("Credential already exists!");
        }
    }

    /**
     * Gets the data transfer version of the credential.
     * Preconditions:
     * <br> * consumerKey is non-null.
     * @return Null if not found.
     */
    public LtiOAuthCredentialAttributes getCredential(String consumerKey) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, consumerKey);
        return consumerKey.isEmpty() ? null : makeAttributesOrNull(getCredentialEntity(consumerKey));
    }

    // TODO: maybe we should inline this
    private LtiOAuthCredential getCredentialEntity(String consumerKey) {
        LtiOAuthCredential ltiOAuthCredential = load().id(consumerKey).now();
        if (ltiOAuthCredential == null) {
            return null;
        }

        return ltiOAuthCredential;
    }

    @Override
    protected LoadType<LtiOAuthCredential> load() {
        return ofy().load().type(LtiOAuthCredential.class);
    }

    @Override
    protected LtiOAuthCredential getEntity(LtiOAuthCredentialAttributes entity) {
        return getCredentialEntity(entity.getConsumerKey());
    }

    @Override
    protected QueryKeys<LtiOAuthCredential> getEntityQueryKeys(LtiOAuthCredentialAttributes attributes) {
        Key<LtiOAuthCredential> keyToFind = Key.create(LtiOAuthCredential.class, attributes.getConsumerKey());
        return load().filterKey(keyToFind).keys();
    }

    @Override
    protected LtiOAuthCredentialAttributes makeAttributes(LtiOAuthCredential entity) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, entity);

        return new LtiOAuthCredentialAttributes(entity);
    }
}
