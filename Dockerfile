# Reference: Play-Docker-Example @ https://github.com/oleksandra-holovina/docker-play-example
FROM openjdk:11.0.13-slim-bullseye AS app_assembly

WORKDIR /app

# Install required dependencies to run sbt
RUN bash -c "apt-get update \
  && apt-get install -y gnupg curl --no-install-recommends \
  && echo 'deb https://repo.scala-sbt.org/scalasbt/debian /' | tee -a /etc/apt/sources.list.d/sbt.list \
  && curl -sL 'https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823' | apt-key add \
  && apt-get update \
  && apt-get install -y sbt --no-install-recommends \
  && rm -rf /var/lib/apt/lists/* /usr/share/doc /usr/share/man \
  && apt-get clean \
  && useradd --create-home java \
  && chown java:java -R /app"

USER java

# Copy build.sbt and project plugins to cache them first
COPY --chown=java:java build.sbt build.sbt
COPY --chown=java:java project project

RUN sbt assemblyPackageDependency

# Copy the rest of the stuff (targets are ignored from .dockerignore)
COPY --chown=java:java . ./

# Re-assemble
RUN sbt assembly

# Expose the port
EXPOSE 9000

# Run the app
CMD ["sbt", "run"]
