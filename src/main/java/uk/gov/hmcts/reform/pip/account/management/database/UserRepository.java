package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;

public interface UserRepository extends JpaRepository<PiUser, Long> {
}
