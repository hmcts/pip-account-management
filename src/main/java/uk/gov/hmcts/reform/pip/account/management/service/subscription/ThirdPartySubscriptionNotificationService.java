package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.ApiOauthConfigurationRepository;
import uk.gov.hmcts.reform.pip.account.management.database.ApiSubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiSubscription;
import uk.gov.hmcts.reform.pip.account.management.service.PublicationService;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;
import uk.gov.hmcts.reform.pip.model.thirdparty.ApiOauthConfiguration;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyAction;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartySubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class ThirdPartySubscriptionNotificationService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final ApiSubscriptionRepository apiSubscriptionRepository;
    private final ApiOauthConfigurationRepository apiOauthConfigurationRepository;
    private final PublicationService publicationService;

    @Autowired
    public ThirdPartySubscriptionNotificationService(
        ApiSubscriptionRepository apiSubscriptionRepository,
        ApiOauthConfigurationRepository apiOauthConfigurationRepository,
        PublicationService publicationService) {
        this.apiSubscriptionRepository = apiSubscriptionRepository;
        this.apiOauthConfigurationRepository = apiOauthConfigurationRepository;
        this.publicationService = publicationService;
    }

    public void handleThirdPartySubscription(Artefact artefact) {
        List<ApiOauthConfiguration> apiOauthConfigurationList = collectThirdPartySubscriberConfigurationList(artefact);
        ThirdPartyAction thirdPartyAction = artefact.getSupersededCount() > 0
            ? ThirdPartyAction.UPDATE_PUBLICATION
            : ThirdPartyAction.NEW_PUBLICATION;
        publicationService.sendThirdPartySubscription(
            new ThirdPartySubscription(apiOauthConfigurationList, artefact.getArtefactId(), thirdPartyAction)
        );
    }

    public void handleThirdPartySubscriptionForDeletedPublication(Artefact artefact) {
        List<ApiOauthConfiguration> apiOauthConfigurationList = collectThirdPartySubscriberConfigurationList(artefact);
        publicationService.sendThirdPartySubscription(
            new ThirdPartySubscription(apiOauthConfigurationList, artefact.getArtefactId(),
                                       ThirdPartyAction.DELETE_PUBLICATION)
        );
    }

    private List<ApiOauthConfiguration> collectThirdPartySubscriberConfigurationList(Artefact artefact) {
        List<ApiSubscription> apiSubscriptions = buildThirdPartySubscriberList(artefact);
        List<ApiOauthConfiguration> apiOauthConfigurations = new ArrayList<>();

        apiSubscriptions.forEach(apiSubscription -> {
            Optional<uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration>
                foundApiOauthConfiguration = apiOauthConfigurationRepository.findByUserId(apiSubscription.getUserId());

            if (foundApiOauthConfiguration.isPresent()) {
                try {
                    ApiOauthConfiguration apiOauthConfiguration = OBJECT_MAPPER.readValue(
                        OBJECT_MAPPER.writeValueAsString(foundApiOauthConfiguration), ApiOauthConfiguration.class
                    );
                    apiOauthConfigurations.add(apiOauthConfiguration);
                } catch (JsonProcessingException e) {
                    log.error(writeLog(
                        String.format("Error converting OAuth configuration for third-party user with ID %s",
                                      apiSubscription.getUserId())
                    ));
                }
            } else {
                log.error(writeLog(String.format("No OAuth configuration found for third-party user with ID %s",
                                                apiSubscription.getUserId())));
            }
        });
        return apiOauthConfigurations;
    }

    private List<ApiSubscription> buildThirdPartySubscriberList(Artefact artefact) {
        List<Sensitivity> allowedApiSensitivities = determineAllowedApiSensitivities(artefact);
        return apiSubscriptionRepository.findByListTypeAndSensitivityIn(
            artefact.getListType(),
            allowedApiSensitivities
        );
    }

    private List<Sensitivity> determineAllowedApiSensitivities(Artefact artefact) {
        List<Sensitivity> allowedApiSensitivities = new ArrayList<>(List.of(Sensitivity.CLASSIFIED));

        if (Sensitivity.PUBLIC.equals(artefact.getSensitivity())) {
            allowedApiSensitivities.addAll(List.of(Sensitivity.PUBLIC, Sensitivity.PRIVATE));
        } else if (Sensitivity.PRIVATE.equals(artefact.getSensitivity())) {
            allowedApiSensitivities.add(Sensitivity.PRIVATE);
        }
        return allowedApiSensitivities;
    }
}
