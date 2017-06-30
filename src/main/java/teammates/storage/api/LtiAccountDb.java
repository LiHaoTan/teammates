package teammates.storage.api;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.QueryKeys;

import teammates.common.datatransfer.attributes.LtiAccountAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.storage.entity.LtiAccount;

/**
 * Handles CRUD operations for LTI Account.
 *
 * @see LtiAccount
 * @see LtiAccountAttributes
 */
public class LtiAccountDb extends EntitiesDb<LtiAccount, LtiAccountAttributes> {
    /**
     * Preconditions:
     * <br> * {@code ltiAccountAttributes} is not null and has valid data.
     */
    public void createAccount(LtiAccountAttributes ltiAccountAttributes) throws InvalidParametersException {
        try {
            createEntity(ltiAccountAttributes);
        } catch (EntityAlreadyExistsException e) {
            throw new InvalidParametersException("Credential already exists!");
        }
    }

    /**
     * Gets the data transfer version of the account.
     * Preconditions:
     * <br> * userId is non-null.
     * @return Null if not found.
     */
    public LtiAccountAttributes getAccount(String userId) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, userId);
        return userId.isEmpty() ? null : makeAttributesOrNull(getAccountEntity(userId));
    }

    private LtiAccount getAccountEntity(String userId) {
        LtiAccount ltiAccount = load().id(userId).now();
        if (ltiAccount == null) {
            return null;
        }

        return ltiAccount;
    }

    @Override
    protected LoadType<LtiAccount> load() {
        return ofy().load().type(LtiAccount.class);
    }

    @Override
    protected LtiAccount getEntity(LtiAccountAttributes entity) {
        return getAccountEntity(entity.getUserId());
    }

    @Override
    protected QueryKeys<LtiAccount> getEntityQueryKeys(LtiAccountAttributes attributes) {
        Key<LtiAccount> keyToFind = Key.create(LtiAccount.class, attributes.getUserId());
        return load().filterKey(keyToFind).keys();
    }

    @Override
    protected LtiAccountAttributes makeAttributes(LtiAccount entity) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, entity);

        return LtiAccountAttributes.valueOf(entity);
    }

    public void updateAccountWithRegistrationKey(String userId, String regkey) {
        final LtiAccount updatedLtiAccount = load().id(userId).now();
        updatedLtiAccount.setRegkey(regkey);
        ofy().save().entity(updatedLtiAccount).now();
    }

    public void updateAccountWithGoogleId(String userId, String googleId) {
        final LtiAccount updatedLtiAccount = load().id(userId).now();
        updatedLtiAccount.setGoogleId(googleId);
        ofy().save().entity(updatedLtiAccount).now();
    }
}
