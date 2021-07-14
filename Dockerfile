FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4
ARG APP_INSIGHTS_AGENT_VERSION=2.5.1

ENV APP pip-business-rules.jar

COPY build/libs/$APP /opt/app/
COPY lib/AI-Agent.xml /opt/app/

EXPOSE 4550
CMD [ "pip-account-management.jar" ]