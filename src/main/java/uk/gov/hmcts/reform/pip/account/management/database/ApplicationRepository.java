package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pip.account.management.model.Application;


@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long>{

}
