package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.model.AudienceType;
import uk.gov.hmcts.reform.pip.account.management.model.UserCommunication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserCommunicationRepositoryTest {
    private static final String MESSAGE1 = "Message1";
    private static final String MESSAGE2 = "Message2";
    private static final String MESSAGE3 = "Message3";
    private static final String MESSAGE4 = "Message4";

    @Autowired
    UserCommunicationRepository userCommunicationRepository;

    @BeforeAll
    void setup() {
        UserCommunication userCommunication1 = new UserCommunication();
        userCommunication1.setAudienceType(AudienceType.ALL_USERS);
        userCommunication1.setMessage(MESSAGE1);

        UserCommunication userCommunication2 = new UserCommunication();
        userCommunication2.setAudienceType(AudienceType.USER_PROVENANCE);
        userCommunication2.setUserProvenances(List.of("CFT_IDAM"));
        userCommunication2.setMessage(MESSAGE2);

        UserCommunication userCommunication3 = new UserCommunication();
        userCommunication3.setAudienceType(AudienceType.USER_ROLE);
        userCommunication3.setUserRoles(List.of("VERIFIED"));
        userCommunication3.setMessage(MESSAGE3);

        UserCommunication userCommunication4 = new UserCommunication();
        userCommunication4.setAudienceType(AudienceType.JURISDICTION);
        userCommunication4.setSubJurisdictions(List.of("Crown Court"));
        userCommunication4.setMessage(MESSAGE4);

        userCommunicationRepository.saveAll(
            List.of(userCommunication1, userCommunication2, userCommunication3, userCommunication4)
        );
    }

    @Test
    void shouldFindAllUserCommunications() {
        assertThat(userCommunicationRepository.findAll())
            .hasSize(4)
            .extracting(UserCommunication::getMessage)
            .containsExactlyInAnyOrder(MESSAGE1, MESSAGE2, MESSAGE3, MESSAGE4);
    }
}
