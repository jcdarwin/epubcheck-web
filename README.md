# EPUBCheck for Web

This project serves [EPUBCheck](https://github.com/w3c/epubcheck) as a web service: upload an EPUB through a web
page and get EPUBCheck's report back. It's packaged as a Docker image running
[Apache Tomcat](https://tomcat.apache.org/) 11 on a Java 21 JRE.

This project is licensed under the same license as the EPUBCheck project, namely, the
[BSD 3 license](https://opensource.org/licenses/BSD-3-Clause).

## The rationale

Originally, `epubcheck.jar` was accompanied by a `.war` file, which could be hosted by a Java web server such as
Tomcat to provide web-based EPUB validation. Recent EPUBCheck releases don't include one, so this project builds the
web service from an official EPUBCheck release.

## What's in the image

| Part | Version | Notes |
|---|---|---|
| EPUBCheck | 5.4.0 | The official release zip, checked against a pinned SHA-256 |
| Tomcat | 11.0.29 | Apache's tarball, checked against its SHA-512 |
| Java | Temurin 21.0.13.1+2 JRE, Alpine | Includes the fix for CVE-2026-114938 |

Versions and checksums are pinned in the `Dockerfile`.

The only code in this repo is the servlet (`src/main/java/com/adobe/epubcheck/web/`):

- `EpubCheckServlet` takes an upload (form field `file`, up to 250 MB) at `/Check`, runs EPUBCheck on it, and
  returns the report as plain text. It deletes its temp files after every request.
- `WebReport` writes EPUBCheck's fatal errors, errors and warnings in the command-line tool's format, naming the
  uploaded file rather than the server's temp file.

The webapp is unpacked at `/usr/local/tomcat/webapps/epubcheck/`, with a plain default page (`webapp/index.html`).

Note that `epubcheck.jar` is unpacked into `WEB-INF/classes` rather than placed in `WEB-INF/lib`. It replaces some of
Jing's classes and resources, so it must load before `jing-*.jar`. Tomcat doesn't order `WEB-INF/lib`, but always
loads `WEB-INF/classes` first.

## Build and run

    docker build --platform linux/amd64 -t epubcheck-web:5.4.0-1 .
    docker compose up

Then open <http://localhost:8080/epubcheck/>.

To check a file from the command line:

    curl -F file=@book.epub http://localhost:8080/epubcheck/Check

The image sets `CATALINA_OPTS="-Xmx256m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC"` for a small host (1 GB RAM,
1 CPU). Override it with `-e CATALINA_OPTS=…`. Don't lower the thread stack size (`-Xss`): EPUBCheck's schema
validator recurses deeply, and below the 1 MB default it fails with a `StackOverflowError`.

## Branding

To use your own page, build an image from this one and copy your files over the default page. For example, a repo
containing `index.html`, `styles.css` and a logo:

    ARG BASE=epubcheck-web:5.4.0-1
    FROM ${BASE}
    COPY index.html styles.css logo.jpg $CATALINA_HOME/webapps/epubcheck/

Your page's form must `POST` to the relative action `Check`, with `enctype="multipart/form-data"` and a file field
named `file`.

When building locally on Docker Desktop, build the branded image with a builder that can see local images
(`--builder desktop-linux`), or the `FROM` line will try to pull the base image from Docker Hub.

## Upgrading EPUBCheck

1. In the `Dockerfile`, set `EPUBCHECK_VERSION` (it appears in two stages) and the `--checksum` of the new release
   zip (`shasum -a 256 epubcheck-<version>.zip`).
2. Rebuild, and check some EPUBs against the same version of the command-line tool.
