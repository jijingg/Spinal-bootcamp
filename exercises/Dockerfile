FROM ubuntu

ARG JDK_VERSION=11
ARG MILL_VERSION=0.9.7
ARG SCALA_VERSION=2.13.6
ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies
RUN apt-get update && apt-get install -y openjdk-$JDK_VERSION-jdk iverilog verilator python3-pip curl && pip3 install cocotb

COPY . /tmp
# Install mill
RUN \
  # echo "mill=${MILL_VERSION} jdk=$JDK_VERSION scala=$SCALA_VERSION" && \
  curl -L -o /usr/local/bin/mill https://github.com/lihaoyi/mill/releases/download/$MILL_VERSION/$MILL_VERSION && \
  chmod +x /usr/local/bin/mill && \
  # touch build.sc && \
  cd /tmp && \
  mill -i resolve _ && \
  rm -rf *

