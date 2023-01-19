package uk.gov.hmcts.reform.pip.account.management.config.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        FlywayNoOpStrategy flywayNoOpStrategy = new FlywayNoOpStrategy();
        try {
            flywayNoOpStrategy.migrate(flyway);
            Assert.fail("No exception thrown for pending flyway");
        } catch (PendingMigrationScriptException e) {
            assertNotNull(e.getMessage(), "Exception is not empty");
        }
    }
}
