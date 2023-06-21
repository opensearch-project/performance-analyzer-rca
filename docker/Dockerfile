#
# Copyright OpenSearch Contributors
# SPDX-License-Identifier: Apache-2.0

################################################################################
# Build stage 0 `prep_open_search_files`:
# Extract opensearch artifact
# Install required plugins
# Set gid=0 and make group perms==owner perms
################################################################################

FROM centos:7 AS prep_open_search_files

ENV PATH /usr/share/opensearch/bin:$PATH
RUN curl -s https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_linux-x64_bin.tar.gz | tar -C /opt -zxf -
ENV OPENSEARCH_JAVA_HOME /opt/jdk-11.0.1
RUN yum install -y unzip
RUN yum install -y lsof

RUN groupadd -g 1000 opensearch && \
    adduser -u 1000 -g 1000 -d /usr/share/opensearch opensearch

USER 1000


WORKDIR /usr/share/opensearch

# Bust cache for wgets
ENV BUST_CACHE 1576286189

# Download and extract defined OpenSearch version.
RUN curl -fsSL https://artifacts.opensearch.org/snapshots/core/opensearch/1.3.11-SNAPSHOT/opensearch-min-1.3.11-SNAPSHOT-linux-x64-latest.tar.gz | \
    tar zx --strip-components=1

RUN set -ex && for opensearchdirs in config data logs; do \
        mkdir -p "$opensearchdirs"; \
    done

COPY --chown=1000:0 opensearch.yml log4j2.properties config/

COPY --chown=1000:0 performance-analyzer-rca-1.3.11.0-SNAPSHOT.zip config/

COPY --chown=1000:0 opensearch-performance-analyzer-1.3.11.0-SNAPSHOT.zip /tmp/

RUN opensearch-plugin install --batch file:///tmp/opensearch-performance-analyzer-1.3.11.0-SNAPSHOT.zip; \
        rm /tmp/opensearch-performance-analyzer-1.3.11.0-SNAPSHOT.zip

USER 0

# Set gid to 0 for opensearch and make group permission similar to that of user
RUN chown -R opensearch:0 . && \
    chmod -R g=u /usr/share/opensearch

RUN unzip config/performance-analyzer-rca-1.3.11.0-SNAPSHOT.zip

RUN chmod -R 755 /dev/shm
################################################################################
# Build stage 1 (the actual OpenSearch image):
# Copy OpenSearch from stage 0
# Add entrypoint
################################################################################

FROM centos:7

ENV OPENSEARCH_CONTAINER true

RUN \
  rpm --rebuilddb && yum clean all && \
  yum install -y epel-release && \
  yum update -y && \
  yum install -y \
                  iproute \
                  python-setuptools \
                  hostname \
                  inotify-tools \
                  yum-utils \
                  which \
                  jq \
                  lsof \
                  python-pip \
                  rsync && \
  yum clean all && \
  pip install supervisor

RUN yum update -y && \
    yum install -y nc unzip wget which && \
    yum clean all
COPY CENTOS_LICENSING.txt /root
COPY --from=prep_open_search_files --chown=1000:0 /opt/jdk-11.0.1 /opt/jdk-11.0.1
ENV OPENSEARCH_JAVA_HOME /opt/jdk-11.0.1
ENV OPENSEARCH_PATH_CONF /usr/share/opensearch/config

# Replace OpenJDK's built-in CA certificate keystore with the one from the OS
# vendor. The latter is superior in several ways.
RUN ln -sf /etc/pki/ca-trust/extracted/java/cacerts /opt/jdk-11.0.1/lib/security/cacerts

ENV PATH $PATH:$OPENSEARCH_JAVA_HOME/bin

RUN mkdir /usr/share/opensearch && \
    groupadd -g 1000 opensearch && \
    adduser -u 1000 -g 1000 -G 0 -d /usr/share/opensearch opensearch && \
    chmod 0775 /usr/share/opensearch && \
    chgrp 0 /usr/share/opensearch

RUN mkdir -p /usr/share/supervisor/performance_analyzer

WORKDIR /usr/share/opensearch
COPY --from=prep_open_search_files --chown=1000:0 /usr/share/opensearch /usr/share/opensearch
ENV PATH /usr/share/opensearch/bin:$PATH

ADD --chown=1000:0 docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

# Openshift overrides USER and uses ones with randomly uid>1024 and gid=0
# Allow ENTRYPOINT (and OpenSearch) to run even with a different user
RUN chgrp 0 /usr/local/bin/docker-entrypoint.sh && \
    chmod g=u /etc/passwd && \
    chmod 0775 /usr/local/bin/docker-entrypoint.sh

# Bind to all interfaces so that the docker container is accessible from the host machine
RUN sed -i "s|#webservice-bind-host =|webservice-bind-host = 0.0.0.0|g" /usr/share/opensearch/config/opensearch-performance-analyzer/performance-analyzer.properties

EXPOSE 9200 9300 9600 9650

LABEL org.label-schema.schema-version="1.0" \
  org.label-schema.name="opensearch" \
  org.label-schema.version="1.3.11" \
  org.label-schema.url="https://opensearch.org/" \
  org.label-schema.vcs-url="https://github.com/opensearch-project/opensearch-build" \
  org.label-schema.license="Apache-2.0" \
  org.label-schema.vendor="Amazon" \
  org.label-schema.build-date="19.12.13"

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
# Dummy overridable parameter parsed by entrypoint
CMD ["opensearchwrapper"]
