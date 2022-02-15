package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.service.ApplicationService;


@RestController
@Api(tags = "Application Management - API for managing applications")
@RequestMapping("/application")
@Validated
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

}
