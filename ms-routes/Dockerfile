FROM maven:3.9.11-eclipse-temurin-25 AS builder

ARG UID=1010
ARG GID=1010

RUN groupadd -g "${GID}" group2 \
    && useradd --create-home --no-log-init -u "${UID}" -g group2 group2

WORKDIR /opt/app

COPY --chown=group2:group2 pom.xml .
RUN mvn dependency:go-offline -q

COPY --chown=group2:group2 src ./src

RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:25-jre AS runtime

ARG BUILD_DATE=unknown
ARG BUILD_VERSION=unknown
ARG IMAGE_DESCRIPTION=unknown
ARG IMAGE_NAME="Fleet Control - MS routes"
ARG UID=1010
ARG GID=1010

LABEL group2.fleet-control.build-date=$BUILD_DATE \
      group2.fleet-control.name=$IMAGE_NAME \
      group2.fleet-control.description=$IMAGE_DESCRIPTION \
      group2.fleet-control.base.image="eclipse-temurin:25-jre" \
      group2.fleet-control.version=$BUILD_VERSION \
      maintainer="group2"

RUN groupadd -g "${GID}" group2 \
    && useradd --create-home --no-log-init -u "${UID}" -g group2 group2

WORKDIR /opt/app

COPY --from=builder --chown=group2:group2 /opt/app/target/ms-routes.jar app.jar

USER group2

ENTRYPOINT ["java","-jar","app.jar"]