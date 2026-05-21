FROM eclipse-temurin:17-jre
VOLUME ["/tmp","/log"]
EXPOSE 8080
ARG JAR_FILE
ENV JAVA_UPPER_VERSION=eclipse-temurin:21-jre
COPY ./target/TenantService.jar app.jar
ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005","--enable-preview","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
