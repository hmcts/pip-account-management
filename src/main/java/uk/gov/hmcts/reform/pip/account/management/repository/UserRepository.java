package uk.gov.hmcts.reform.pip.account.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pip.account.management.model.CftUser;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<CftUser, Long> {

    void deleteAll();

    Optional<CftUser> findById(UUID id);

    void deleteById(UUID id);
}
