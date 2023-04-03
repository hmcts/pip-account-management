package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ProvenanceUserIdValidatorTest {

    private final PiUser piUser = new PiUser();

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ProvenanceUserIdValidator provenanceUserIdValidator;

    @BeforeEach
    void setup() {
        piUser.setUserProvenance(UserProvenances.PI_AAD);
        piUser.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        piUser.setEmail("email@email.com");
        piUser.setProvenanceUserId("123");
        lenient().when(userRepository.findExistingByProvenanceId(piUser.getProvenanceUserId(),
                                                        piUser.getUserProvenance().name()))
            .thenReturn(List.of());
        lenient().when(userRepository.findExistingByProvenanceId("321", piUser.getUserProvenance().name()))
            .thenReturn(List.of(piUser));
    }

    @Test
    void testUniqueUserReturnsTrue() {
        assertTrue(provenanceUserIdValidator.isValid(piUser, null),
                   "Unique user should return true");
    }

    @Test
    void testExistingUserReturnsFalse() {
        piUser.setProvenanceUserId("321");
        assertFalse(provenanceUserIdValidator.isValid(piUser, null),
                    "Existing users should return false");
    }

    @Test
    void testNullRepositoryReturnsTrue() {
        ProvenanceUserIdValidator provenanceUserIdValidator = new ProvenanceUserIdValidator();
        assertTrue(provenanceUserIdValidator.isValid(piUser, null),
                   "Should return true");
    }
}
