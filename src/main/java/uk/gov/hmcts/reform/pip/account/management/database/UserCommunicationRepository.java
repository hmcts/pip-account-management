package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pip.account.management.model.UserCommunication;

import java.util.UUID;

public interface UserCommunicationRepository extends JpaRepository<UserCommunication, UUID> {
}
