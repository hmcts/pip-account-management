ARG APP_INSIGHTS_AGENT_VERSION=3.2.10
FROM hmctspublic.azurecr.io/base/java:17-distroless

ENV APP pip-account-management.jar

COPY build/libs/$APP /opt/app/

EXPOSE 6969
CMD [ "pip-account-management.jar" ]
