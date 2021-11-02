package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RootControllerTest {

    @Test
    void testRootControllerResponseCode() {
        RootController rootController = new RootController();
        ResponseEntity<String> responseEntity = rootController.welcome();

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "Returned status is OK");
    }
}
