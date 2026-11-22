# syntax=docker/dockerfile:1.7
#
# epubcheck-web: EPUBCheck as a web service, on Tomcat 11 / Java 21 (Temurin, Alpine).
#
#   docker build --platform linux/amd64 -t epubcheck-web:5.4.0-1 .
#
# The webapp is unpacked at /usr/local/tomcat/webapps/epubcheck/, so a branding image can
# replace its default page by copying files over it (see README.md).

ARG JRE_IMAGE=eclipse-temurin:21.0.13.1_2-jre-alpine-3.24@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70
ARG JDK_IMAGE=eclipse-temurin:21.0.13.1_2-jdk-alpine-3.24@sha256:c07bb82e004a166b5d3d63b84ad70e2e822f228505e2e0c0b8dfdc2890d22f13
ARG ALPINE_IMAGE=alpine:3.24.4@sha256:f59abe656b692d901e184be3445e084786602108599d2a51d229c0f5a48c71bf

# ---------------------------------------------------------------------------
# Tomcat 11 on the Temurin 21 JRE. Alpine (musl), not the official Ubuntu-based tomcat
# images: glibc 2.34+ can't create threads under the seccomp profile of Docker < 20.10.10.
# 21.0.13.1+2 carries JDK-8393212, the fix for CVE-2026-114938 ("Java4Shell").
FROM ${JRE_IMAGE} AS runtime

ARG TOMCAT_VERSION=11.0.29
ARG TOMCAT_SHA512=be277ce58876a584653f980fd695c82e6708029f05fc7ad9fcfbdcede3836ae6ffa4f858eac26de6ff938f130f31158be8a54b5f546393ea1ad5da0cd202873c

ENV CATALINA_HOME=/usr/local/tomcat
ENV PATH=$CATALINA_HOME/bin:$PATH

RUN set -eux; \
    wget -q -O /tmp/tomcat.tgz "https://archive.apache.org/dist/tomcat/tomcat-11/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz"; \
    echo "${TOMCAT_SHA512}  /tmp/tomcat.tgz" | sha512sum -c -; \
    mkdir -p "$CATALINA_HOME"; \
    tar -xzf /tmp/tomcat.tgz -C "$CATALINA_HOME" --strip-components=1; \
    rm /tmp/tomcat.tgz; \
    rm -rf "$CATALINA_HOME"/webapps/* "$CATALINA_HOME"/bin/*.bat; \
    # Don't reveal the Tomcat version, or stack traces, on Tomcat's own error pages.
    sed -i 's#</Host>#  <Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false"/>\n      </Host>#' "$CATALINA_HOME"/conf/server.xml; \
    grep -q 'showServerInfo="false"' "$CATALINA_HOME"/conf/server.xml; \
    # Log only to the console, so Docker's log (rotated by the host) is the one log. Drop the access
    # log, which grows a file a day forever; the reverse proxy logs requests.
    sed -i '/<Valve className="org.apache.catalina.valves.AccessLogValve"/,/\/>/d' "$CATALINA_HOME"/conf/server.xml; \
    ! grep -q AccessLogValve "$CATALINA_HOME"/conf/server.xml; \
    printf '%s\n' \
        'handlers = java.util.logging.ConsoleHandler' \
        '.handlers = java.util.logging.ConsoleHandler' \
        'java.util.logging.ConsoleHandler.level = FINE' \
        'java.util.logging.ConsoleHandler.formatter = org.apache.juli.OneLineFormatter' \
        'java.util.logging.ConsoleHandler.encoding = UTF-8' \
        > "$CATALINA_HOME"/conf/logging.properties; \
    addgroup -S tomcat && adduser -S -G tomcat -H -h "$CATALINA_HOME" tomcat; \
    # The tarball ships files as 640/750: let the tomcat group read and run them, but not change them.
    chown -R root:tomcat "$CATALINA_HOME"; \
    chmod -R g+rX,o-rwx "$CATALINA_HOME"; \
    mkdir -p "$CATALINA_HOME"/conf/Catalina; \
    chown -R tomcat:tomcat "$CATALINA_HOME"/conf/Catalina "$CATALINA_HOME"/logs "$CATALINA_HOME"/temp "$CATALINA_HOME"/work

# ---------------------------------------------------------------------------
# The official EPUBCheck release (epubcheck.jar and its lib/), checksum-pinned.
FROM ${ALPINE_IMAGE} AS epubcheck
ARG EPUBCHECK_VERSION=5.4.0
ADD --checksum=sha256:33350c61038e71dfb3d45a76aed04bf5481e6d5500cb780f6e98db8bbd15a28c \
    https://github.com/w3c/epubcheck/releases/download/v${EPUBCHECK_VERSION}/epubcheck-${EPUBCHECK_VERSION}.zip /tmp/epubcheck.zip
# epubcheck.jar replaces some of Jing's classes and resources (e.g. Localizer and the
# Schematron messages), so it must load before jing-*.jar. `java -jar` gets that order from
# the manifest, but Tomcat doesn't order WEB-INF/lib. Unpacking epubcheck.jar into
# WEB-INF/classes, which the servlet spec loads before WEB-INF/lib, guarantees it.
# (With the wrong order, Schematron USAGE notes are reported as errors.)
RUN set -eux; \
    unzip -q /tmp/epubcheck.zip -d /tmp; \
    mkdir -p /out/lib /out/classes; \
    mv /tmp/epubcheck-${EPUBCHECK_VERSION}/lib/*.jar /out/lib/; \
    unzip -q /tmp/epubcheck-${EPUBCHECK_VERSION}/epubcheck.jar -d /out/classes -x 'META-INF/MANIFEST.MF'

# ---------------------------------------------------------------------------
# Compile the servlet against Tomcat's own Servlet API and EPUBCheck.
FROM ${JDK_IMAGE} AS build
COPY --from=runtime /usr/local/tomcat/lib/servlet-api.jar /build/servlet-api.jar
COPY --from=epubcheck /out/lib /build/lib
COPY --from=epubcheck /out/classes /build/epubcheck-classes
COPY src/main/java /build/src
RUN set -eux; \
    mkdir -p /build/classes; \
    javac --release 21 -Xlint:all,-path -Werror -d /build/classes \
      -cp "/build/servlet-api.jar:/build/epubcheck-classes:/build/lib/*" $(find /build/src -name '*.java')

# ---------------------------------------------------------------------------
FROM runtime
ARG EPUBCHECK_VERSION=5.4.0
LABEL org.opencontainers.image.title="epubcheck-web" \
      org.opencontainers.image.description="EPUBCheck ${EPUBCHECK_VERSION} as a web service (Tomcat 11, Java 21)" \
      org.opencontainers.image.source="https://github.com/jcdarwin/epubcheck-web" \
      org.opencontainers.image.licenses="BSD-3-Clause"

ENV EPUBCHECK_VERSION=${EPUBCHECK_VERSION}
# Sized for a small host (the droplet has 1 GB RAM and 1 CPU). Override at run time if needed.
ENV CATALINA_OPTS="-Xmx256m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC"

COPY webapp/ $CATALINA_HOME/webapps/epubcheck/
COPY --from=epubcheck /out/classes/ $CATALINA_HOME/webapps/epubcheck/WEB-INF/classes/
COPY --from=build /build/classes/ $CATALINA_HOME/webapps/epubcheck/WEB-INF/classes/
COPY --from=epubcheck /out/lib/ $CATALINA_HOME/webapps/epubcheck/WEB-INF/lib/

WORKDIR $CATALINA_HOME
USER tomcat
EXPOSE 8080
CMD ["catalina.sh", "run"]
