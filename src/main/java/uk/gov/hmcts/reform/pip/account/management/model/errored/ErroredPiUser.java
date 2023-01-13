package uk.gov.hmcts.reform.pip.account.management.model.errored;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ErroredPiUser extends PiUser {

    /**
     * This is the error messages for why the user has failed to be created.
     */
    private List<String> errorMessages;

    /**
     * Constructor that takes in an existing user and converts it to an errored user.
     * @param user The user to be converted to an errored user.
     */
    public ErroredPiUser(PiUser user) {
        super(user.getUserId(),
              user.getUserProvenance(),
              user.getProvenanceUserId(),
              user.getEmail(),
              user.getRoles(),
              user.getForenames(),
              user.getSurname(),
              user.getCreatedDate(),
              user.getLastVerifiedDate(),
              user.getLastSignedInDate());
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == this.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
