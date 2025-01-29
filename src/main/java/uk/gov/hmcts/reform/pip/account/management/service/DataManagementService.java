package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import uk.gov.hmcts.reform.pip.model.location.Location;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;
import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Slf4j
@Service
/**
 * Service to communicate with data-management microservice.
 */
public class DataManagementService {

    private final WebClient webClient;

    @Value("${service-to-service.data-management}")
    private String url;

    @Autowired
    public DataManagementService(WebClient webClient) {
        this.webClient = webClient;
    }


    public String getCourtName(String locationId) {
        try {
            Location location = webClient.get().uri(String.format("%s/locations/%s", url, locationId))
                .attributes(clientRegistrationId("dataManagementApi"))
                .retrieve()
                .bodyToMono(Location.class)
                .block();
            if (location != null) {
                return location.getName();
            }
            return null;
        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("Data management request to get location name failed for LocationId: %s. Response: %s",
                              locationId, ex.getMessage())
            ));
            return null;
        }
    }

}
