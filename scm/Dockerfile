FROM ghcr.io/graalvm/jdk:ol9-java17-22.3.1

# Env variables
ARG SCALA_VERSION=3.2.2
ENV SCALA_VERSION ${SCALA_VERSION:-3.2.2}
ARG SBT_VERSION=1.8.2
ENV SBT_VERSION ${SBT_VERSION:-1.8.2}
ARG USER_ID
ENV USER_ID ${USER_ID:-1001}
ARG GROUP_ID
ENV GROUP_ID ${GROUP_ID:-1001}

RUN microdnf update -y oraclelinux-release-el9 \
    && microdnf --enablerepo ol9_codeready_builder install -y bzip2-devel ed gcc gcc-c++ gcc-gfortran gzip file fontconfig less libcurl-devel make openssl openssl-devel readline-devel tar glibc-langpack-en \
    vi which xz-devel zlib-devel findutils glibc-static libstdc++ libstdc++-devel libstdc++-static zlib-static libxcrypt-compat \
    && microdnf clean all

RUN fc-cache -f -v

# Install sbt
RUN \
  microdnf install -y wget unzip gcc make glibc-devel zlib-devel && wget https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.zip -P /usr/share
  
RUN unzip /usr/share/sbt-$SBT_VERSION.zip -d /usr/share 

RUN \
  chown -R root:root /usr/share/sbt && \
  chmod -R 755 /usr/share/sbt && \
  ln -s /usr/share/sbt/bin/sbt /usr/local/bin/sbt

RUN \
  URL=https://github.com/lampepfl/dotty/releases/download/$SCALA_VERSION/scala3-$SCALA_VERSION.zip && wget $URL -P /usr/share && unzip /usr/share/scala3-$SCALA_VERSION.zip -d /usr/share

RUN mv /usr/share/scala3-$SCALA_VERSION /usr/share/scala

RUN \
  chown -R root:root /usr/share/scala && \
  chmod -R 755 /usr/share/scala && \
  ln -s /usr/share/scala/bin/* /usr/local/bin && \
  echo '@main def main = println(s"Scala library version ${dotty.tools.dotc.config.Properties.versionNumberString}")' > test.scala \
  scala -nocompdaemon test.scala && rm test.scala

# Add and use user sbtuser
RUN groupadd --gid $GROUP_ID sbtuser && useradd --gid $GROUP_ID --uid $USER_ID sbtuser --shell /bin/bash
USER sbtuser

# Switch working directory
WORKDIR /home/sbtuser

# Prepare sbt (warm cache)
RUN \
  sbt sbtVersion && \
  mkdir -p project && \
  echo "scalaVersion := \"${SCALA_VERSION}\"" > build.sbt && \
  echo "sbt.version=${SBT_VERSION}" > project/build.properties && \
  echo "// force sbt compiler-bridge download" > project/Dependencies.scala && \
  echo "case object Temp" > Temp.scala && \
  sbt compile && \
  rm -r project && rm build.sbt && rm Temp.scala && rm -r target

# Link everything into root as well
# This allows users of this container to choose, whether they want to run the container as sbtuser (non-root) or as root
USER root
RUN \
  rm -rf /tmp/..?* /tmp/.[!.]* * && \
  ln -s /home/sbtuser/.cache /root/.cache && \
  ln -s /home/sbtuser/.sbt /root/.sbt && \
  if [ -d "/home/sbtuser/.ivy2" ]; then ln -s /home/sbtuser/.ivy2 /root/.ivy2; fi

# Switch working directory back to root
## Users wanting to use this container as non-root should combine the two following arguments
## -u sbtuser
## -w /home/sbtuser
WORKDIR /root