package uk.gov.hmcts.reform.pip.account.management.service.thirdparty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.ApiOauthConfigurationRepository;
import uk.gov.hmcts.reform.pip.account.management.database.ApiSubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.database.ApiUserRepository;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiSubscription;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUserStatus;
import uk.gov.hmcts.reform.pip.account.management.service.PublicationService;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyAction;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyOauthConfiguration;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartySubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class ThirdPartySubscriptionNotificationService {
    private final ApiSubscriptionRepository apiSubscriptionRepository;
    private final ApiOauthConfigurationRepository apiOauthConfigurationRepository;
    private final PublicationService publicationService;
    private final ApiUserRepository apiUserRepository;

    @Autowired
    public ThirdPartySubscriptionNotificationService(
        ApiSubscriptionRepository apiSubscriptionRepository,
        ApiOauthConfigurationRepository apiOauthConfigurationRepository,
        PublicationService publicationService,
        ApiUserRepository apiUserRepository) {
        this.apiSubscriptionRepository = apiSubscriptionRepository;
        this.apiOauthConfigurationRepository = apiOauthConfigurationRepository;
        this.publicationService = publicationService;
        this.apiUserRepository = apiUserRepository;
    }

    public void handleThirdPartySubscription(Artefact artefact) {
        List<ThirdPartyOauthConfiguration> thirdPartyOauthConfigurationList =
            collectThirdPartySubscriberConfigurationList(artefact);

        ThirdPartyAction thirdPartyAction = artefact.getSupersededCount() > 0
            ? ThirdPartyAction.UPDATE_PUBLICATION
            : ThirdPartyAction.NEW_PUBLICATION;

        if (!thirdPartyOauthConfigurationList.isEmpty()) {
            publicationService.sendThirdPartySubscription(new ThirdPartySubscription(
                thirdPartyOauthConfigurationList, artefact.getArtefactId(), thirdPartyAction
            ));
        }
    }

    public void handleThirdPartySubscriptionForDeletedPublication(Artefact artefact) {
        List<ThirdPartyOauthConfiguration> thirdPartyOauthConfigurationList =
            collectThirdPartySubscriberConfigurationList(artefact);

        if (!thirdPartyOauthConfigurationList.isEmpty()) {
            publicationService.sendThirdPartySubscription(new ThirdPartySubscription(
                thirdPartyOauthConfigurationList, artefact.getArtefactId(), ThirdPartyAction.DELETE_PUBLICATION
            ));
        }
    }

    private List<ThirdPartyOauthConfiguration> collectThirdPartySubscriberConfigurationList(Artefact artefact) {
        List<ApiSubscription> apiSubscriptions = buildThirdPartySubscriberList(artefact);
        List<ThirdPartyOauthConfiguration> thirdPartyOauthConfigurationList = new ArrayList<>();

        apiSubscriptions.forEach(apiSubscription -> {
            apiUserRepository.findByUserId(apiSubscription.getUserId()).ifPresentOrElse(apiUser -> {
                if (ApiUserStatus.ACTIVE.equals(apiUser.getStatus())) {
                    Optional<ApiOauthConfiguration> foundApiOauthConfiguration = apiOauthConfigurationRepository
                        .findByUserId(apiSubscription.getUserId());

                    if (foundApiOauthConfiguration.isPresent()) {
                        thirdPartyOauthConfigurationList.add(
                            buildThirdPartySubscriberConfiguration(foundApiOauthConfiguration.get())
                        );
                    } else {
                        log.error(writeLog(String.format("No OAuth configuration found for third-party user with ID %s",
                                                        apiSubscription.getUserId())));
                    }
                } else {
                    log.info(writeLog(String.format("Status is not ACTIVE for third-party user with ID %s",
                        apiSubscription.getUserId())));
                }
            }, () -> {
                log.error(writeLog(String.format("No third-party user found with ID %s",
                    apiSubscription.getUserId())));
            });
        });
        return thirdPartyOauthConfigurationList;
    }

    private ThirdPartyOauthConfiguration buildThirdPartySubscriberConfiguration(
        ApiOauthConfiguration foundApiOauthConfiguration
    ) {
        ThirdPartyOauthConfiguration thirdPartyOauthConfiguration = new ThirdPartyOauthConfiguration();
        thirdPartyOauthConfiguration.setUserId(foundApiOauthConfiguration.getUserId());
        thirdPartyOauthConfiguration.setDestinationUrl(foundApiOauthConfiguration.getDestinationUrl());
        thirdPartyOauthConfiguration.setTokenUrl(foundApiOauthConfiguration.getTokenUrl());
        thirdPartyOauthConfiguration.setClientIdKey(foundApiOauthConfiguration.getClientIdKey());
        thirdPartyOauthConfiguration.setClientSecretKey(foundApiOauthConfiguration.getClientSecretKey());
        thirdPartyOauthConfiguration.setScopeKey(foundApiOauthConfiguration.getScopeKey());
        return thirdPartyOauthConfiguration;
    }

    private List<ApiSubscription> buildThirdPartySubscriberList(Artefact artefact) {
        List<Sensitivity> allowedApiSensitivities = determineAllowedApiSensitivities(artefact.getSensitivity());
        return apiSubscriptionRepository.findByListTypeAndSensitivityIn(
            artefact.getListType(),
            allowedApiSensitivities
        );
    }

    private List<Sensitivity> determineAllowedApiSensitivities(Sensitivity sensitivity) {
        List<Sensitivity> allowedApiSensitivities = new ArrayList<>(List.of(Sensitivity.CLASSIFIED));

        if (Sensitivity.PUBLIC.equals(sensitivity)) {
            allowedApiSensitivities.addAll(List.of(Sensitivity.PUBLIC, Sensitivity.PRIVATE));
        } else if (Sensitivity.PRIVATE.equals(sensitivity)) {
            allowedApiSensitivities.add(Sensitivity.PRIVATE);
        }
        return allowedApiSensitivities;
    }
}
