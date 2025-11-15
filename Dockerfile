FROM openjdk:17-oracle
VOLUME ["/tmp","/log"]
EXPOSE 8080
ARG JAR_FILE
COPY ./TenantService.jar app.jar
ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005","--enable-preview","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
