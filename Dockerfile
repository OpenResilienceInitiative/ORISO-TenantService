FROM eclipse-temurin:17-jre
VOLUME ["/tmp","/log"]
EXPOSE 8081
ARG JAR_FILE
ENV JAVA_UPPER_VERSION=eclipse-temurin:17-jre
COPY ./target/TenantService.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-XX:MaxRAMPercentage=75","-jar","/app.jar"]
