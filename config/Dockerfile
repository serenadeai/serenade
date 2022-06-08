FROM ubuntu:20.04

ARG BUILD_TYPE
ARG DEVICE_TYPE
WORKDIR /root/serenade

ENV DEBIAN_FRONTEND="noninteractive"
COPY patches patches
COPY scripts/setup scripts/setup
RUN ./scripts/setup/setup-ubuntu.sh $([ "${DEVICE_TYPE}" = "gpu" ] && echo "--gpu") && \
  apt-get clean && \
  rm -rf /var/lib/apt/lists/*
RUN ./scripts/setup/build-dependencies.sh $([ "${BUILD_TYPE}" = "minimal" ] && echo "--minimal") $([ "${DEVICE_TYPE}" = "gpu" ] && echo "--gpu")

ENV JAVA_HOME="/root/libserenade/jdk-14.0.1"
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV PATH="/root/libserenade/gradle-7.4.2/bin:/root/libserenade/jdk-14.0.1/bin:/root/serenade/scripts/serenade/bin:${PATH}"
ENV PYTHONPATH="/root/serenade/scripts"
ENV SERENADE_SOURCE_ROOT="/root/serenade"
ENV SERENADE_LIBRARY_ROOT="/root/libserenade"
