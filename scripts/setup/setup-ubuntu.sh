#!/bin/bash

set -e

HERE=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
. $HERE/paths.sh
cd $SERENADE_LIBRARY_ROOT

gpu=false
while [[ $# -gt 0 ]]; do
  case $1 in
    --gpu)
      gpu=true
      ;;
    --cpu)
      gpu=false
      ;;
    *)
      echo "Unknown argument: $1"
      exit 1
      ;;
  esac
  shift
done

sudo-non-docker apt-get update
sudo-non-docker apt-get install --upgrade -y \
  apt-transport-https \
  curl \
  gnupg2 \
  wget

if [[ "$gpu" == "true" ]] ; then
  sudo-non-docker apt-get install --upgrade -y ubuntu-drivers-common
  sudo-non-docker ubuntu-drivers autoinstall
fi

curl -sL https://apt.repos.intel.com/intel-gpg-keys/GPG-PUB-KEY-INTEL-SW-PRODUCTS-2019.PUB | sudo-non-docker apt-key add -
echo "deb https://apt.repos.intel.com/mkl all main" | sudo-non-docker tee /etc/apt/sources.list.d/intel-mkl.list
sudo-non-docker apt-get update
sudo-non-docker apt-get install --upgrade -y \
  autoconf \
  automake \
  build-essential \
  ca-certificates \
  clang-format-9 \
  cmake \
  ffmpeg \
  fonts-liberation \
  gawk \
  gconf-service \
  gdb \
  gfortran \
  git \
  groff \
  intel-mkl-64bit-2020.2-108 \
  libasound2 \
  libc++-dev \
  libssl-dev \
  libpq-dev \
  libtool \
  logrotate \
  lsb-release \
  nodejs \
  npm \
  $([[ "$gpu" == "true" ]] && echo "nvidia-cuda-toolkit")  \
  postgresql-client \
  psmisc \
  python2-minimal \
  python3 \
  python3-dev \
  python3-pip \
  redis-tools \
  rsync \
  sox \
  subversion \
  swig \
  unzip \
  vim \
  xdg-utils \
  yarn \
  zlib1g-dev

curl https://download.java.net/java/GA/jdk14.0.1/664493ef4a6946b186ff29eb326336a2/7/GPL/openjdk-14.0.1_linux-x64_bin.tar.gz -Lso jdk.tar.gz
tar xf jdk.tar.gz
rm jdk.tar.gz

echo ""
echo "Install complete!"
echo "Now, run build-dependencies.sh and add the following to your ~/.zshrc or ~/.bashrc:"
echo "export PATH=\"$SERENADE_LIBRARY_ROOT/jdk-14.0.1/bin:$SERENADE_LIBRARY_ROOT/gradle-7.4.2/bin:\$PATH\""
echo "export JAVA_HOME=\"$SERENADE_LIBRARY_ROOT/jdk-14.0.1\""

# If we're not installing on docker, we need to restart.
if [[ "$gpu" == "true" && "$EUID" != 0 ]] ; then
  echo ""
  echo "Restart your system to complete setup."
fi
