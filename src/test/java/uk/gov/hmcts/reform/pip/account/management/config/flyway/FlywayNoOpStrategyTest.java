package uk.gov.hmcts.reform.pip.account.management.config.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlywayNoOpStrategyTest {

    @Mock
    Flyway flyway;

    @Mock
    MigrationInfoService migrationInfoService;

    @Mock
    MigrationInfo migrationInfo;


    @Test
    void testMigrateFlywayWithoutError() {
        MigrationInfo[] migrationInfos = {migrationInfo};

        when(flyway.info()).thenReturn(migrationInfoService);
        when(migrationInfoService.all()).thenReturn(migrationInfos);
        when(migrationInfo.getState()).thenReturn(MigrationState.SUCCESS);

        assertDoesNotThrow(() -> {
            new FlywayNoOpStrategy().migrate(flyway);
        }, "Exception thrown for successful flyway");
    }

    @Test
    void testMigrateFlywayWithError() {
        MigrationInfo[] migrationInfos = {migrationInfo};

        when(flyway.info()).thenReturn(migrationInfoService);
        when(migrationInfoService.all()).thenReturn(migrationInfos);
        when(migrationInfo.getState()).thenReturn(MigrationState.PENDING);

        assertThrows(PendingMigrationScriptException.class, () -> {
            new FlywayNoOpStrategy().migrate(flyway);
        }, "No exception thrown for pending flyway");
    }
}