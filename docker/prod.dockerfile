ARG VERSION="0.0.1-SNAPSHOT"

# Maven base image for building the java code
FROM maven:3-eclipse-temurin-21-jammy as builder


WORKDIR /usr/share/app
COPY pom.xml .
# Downloads packages defined in pom.xml
RUN mvn clean package
COPY src src
# Build the source code to generate fat jar
RUN mvn clean package -Dmaven.test.skip=true

# Java runtime as final base image
FROM eclipse-temurin:21.0.5_11-jre-jammy


ARG VERSION
ENV JAR="iudx.catalogue.server-cluster-${VERSION}-fat.jar"

WORKDIR /usr/share/app
# Copying openapi docs 
COPY docs docs
COPY iudx-pmd-ruleset.xml iudx-pmd-ruleset.xml
COPY google_checks.xml google_checks.xml
# Copying cluster fatjar from builder image stage to final image 
COPY --from=builder /usr/share/app/target/${JAR} ./fatjar.jar
# HTTP cat server port
EXPOSE 8080
# HTTPS cat server port
EXPOSE 8443
# Metrics http server port
EXPOSE 9000 
# creating a non-root user
RUN useradd -r -u 1001 -g root catuser
# Setting non-root user to use when container starts
USER catuser
