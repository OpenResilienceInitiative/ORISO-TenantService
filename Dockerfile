FROM eclipse-temurin:21-jre@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a
VOLUME ["/tmp","/log"]
EXPOSE 8081
ARG JAR_FILE
ENV JAVA_UPPER_VERSION=eclipse-temurin:21-jre@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a
# The Ubuntu-based Temurin image ships Canonical's pebble service manager,
# which is unused here and carries fixable Go CVEs; drop it from the image.
RUN rm -f /usr/bin/pebble
# Same pinned base as AgencyService: it already ships the Ubuntu OpenSSL, curl
# and libbrotli1 fixes. apt/libapt-pkg7.0 still need the fix for CVE-2026-109518.
RUN apt-get update \
    && apt-get install -y --no-install-recommends --only-upgrade apt libapt-pkg7.0 \
    && rm -rf /var/lib/apt/lists/*
COPY ./target/TenantService.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-XX:MaxRAMPercentage=75","-jar","/app.jar"]
