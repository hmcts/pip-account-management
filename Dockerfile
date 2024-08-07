ARG APP_INSIGHTS_AGENT_VERSION=3.4.14
FROM hmctspublic.azurecr.io/base/java:21-distroless

ENV APP pip-account-management.jar

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/$APP /opt/app/

EXPOSE 6969
CMD [ "pip-account-management.jar" ]
