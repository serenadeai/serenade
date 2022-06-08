#!/bin/bash

set -e

HERE=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
. $HERE/paths.sh
cd $SERENADE_LIBRARY_ROOT

if ! [ -x "$(command -v brew)" ]; then
  echo 'Install brew before continuing (https://brew.sh).'
  exit 1
fi

if ! [ -x "$(command -v python3)" ]; then
  echo 'Install Python3 before continuing (https://www.python.org).'
  exit 1
fi

if ! [ -x "$(command -v 'python2.7')" ]; then
  echo 'Install Python2.7 before continuing (https://www.python.org).'
  exit 1
fi

if ! [ -x "$(command -v node)" ]; then
  echo 'Install nodejs before continuing (https://nodejs.org).'
  exit 1
fi

brew install \
  autoconf \
  awk \
  cmake \
  gcc \
  libtool \
  sox \
  subversion \
  swig \
  wget

curl https://download.java.net/java/GA/jdk14.0.1/664493ef4a6946b186ff29eb326336a2/7/GPL/openjdk-14.0.1_osx-x64_bin.tar.gz -Lso jdk.tar.gz
tar xf jdk.tar.gz
rm jdk.tar.gz

echo ""
echo "Install complete!"
echo "Now, run build-dependencies.sh and add the following to your ~/.zshrc or ~/.bashrc:"
echo "export PATH=\"$SERENADE_LIBRARY_ROOT/jdk-14.0.1.jdk/Contents/Home/bin:$SERENADE_LIBRARY_ROOT/gradle-7.4.2/bin:\$PATH\""
echo "export JAVA_HOME=\"$SERENADE_LIBRARY_ROOT/jdk-14.0.1.jdk/Contents/Home\""
