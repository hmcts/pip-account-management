package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValidProvenanceUserId;

import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ProvenanceUserIdValidator implements ConstraintValidator<ValidProvenanceUserId, PiUser> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean isValid(PiUser piUser, ConstraintValidatorContext constraintValidatorContext) {
        List<PiUser> existingUsers = userRepository.findExistingByProvenanceId(piUser.getProvenanceUserId(),
                                                                               piUser.getUserProvenance().name());
        return existingUsers.isEmpty();
    }
}
