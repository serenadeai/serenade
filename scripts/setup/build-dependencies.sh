#!/bin/bash

set -e

HERE=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
. $HERE/paths.sh
cd $SERENADE_LIBRARY_ROOT

osx_version="11.0"
gpu=false
minimal=false
while [[ $# -gt 0 ]]; do
  case $1 in
    --gpu)
      gpu=true
      ;;
    --cpu)
      gpu=false
      ;;
    --minimal)
      minimal=true
      ;;
    *)
      echo "Unknown argument: $1"
      exit 1
      ;;
  esac
  shift
done

pip3 install --upgrade \
  awscli \
  black \
  certbot \
  certbot_dns_route53 \
  certifi \
  click \
  fabric \
  jsonlines \
  numpy \
  pip \
  psutil \
  psycopg2-binary \
  pybars3 \
  pyenchant \
  pyyaml \
  requests \
  sentencepiece==0.1.95

sudo-non-docker npm install -g \
  prettier \
  prettier-plugin-java

rm -rf \
  antlr \
  boost \
  boost-src \
  crow \
  gradle-* \
  kaldi \
  marian \
  sentencepiece \
  sentencepiece-src

curl https://services.gradle.org/distributions/gradle-7.4.2-bin.zip -Lso gradle.zip
unzip -qq gradle.zip
rm gradle.zip

mkdir antlr
curl https://www.antlr.org/download/antlr-4.7.2-complete.jar -Lso antlr/antlr-4.7.2-complete.jar

git clone https://github.com/CrowCpp/crow crow
cd crow
git checkout v1.0+1
cd ..

git clone --recursive https://github.com/boostorg/boost.git boost-src
cd boost-src
git checkout boost-1.78.0
./bootstrap.sh --prefix=$PWD/../boost
./b2 install
cd ..
rm -rf boost-src

git clone --recursive -b v3.14.0 https://github.com/protocolbuffers/protobuf.git protobuf-src
mkdir -p protobuf
cd protobuf-src
./autogen.sh
./configure --prefix=$PWD/../protobuf --disable-shared --with-pic
if [[ `uname` == "Darwin" ]] ; then
  make CFLAGS="-mmacosx-version-min=$osx_version" CXXFLAGS="-g -std=c++11 -DNDEBUG -mmacosx-version-min=$osx_version" -j2
else
  make -j2
fi
make install
cd ..
rm -rf protobuf-src

git clone https://github.com/google/sentencepiece.git sentencepiece-src
cd sentencepiece-src
git checkout v0.1.96
mkdir build
cd build
cmake .. \
  -DCMAKE_OSX_ARCHITECTURES=x86_64 \
  -DCMAKE_INSTALL_PREFIX=$PWD/../../sentencepiece \
  -DCMAKE_OSX_DEPLOYMENT_TARGET=$osx_version
cmake --build . --config Release -j2
cmake --install .
cd ../..
rm -rf sentencepiece-src

git clone https://github.com/marian-nmt/marian-dev marian
cd marian
git checkout 737f43014a939a3ded2806b00cbaa661fbcc5f49
git apply $SERENADE_SOURCE_ROOT/patches/marian/fix-warnings.patch
git apply $SERENADE_SOURCE_ROOT/patches/marian/max-history.patch
mkdir build
cd build
if [[ "$gpu" == "true" ]] ; then
  CC=/usr/bin/gcc-8 CXX=/usr/bin/g++-8 cmake .. \
    -DBUILD_ARCH=x86-64 \
    -DCMAKE_OSX_ARCHITECTURES=x86_64 \
    -DCOMPILE_CUDA=on \
    -DUSE_DOXYGEN=off
elif [[ `uname` == "Darwin" ]] ; then
  cmake .. \
    -DBUILD_ARCH=x86-64 \
    -DCMAKE_OSX_ARCHITECTURES=x86_64 \
    -DCOMPILE_CUDA=off \
    -DUSE_DOXYGEN=off \
    -DUSE_APPLE_ACCELERATE=on \
    -DCMAKE_OSX_DEPLOYMENT_TARGET=$osx_version
else
  cmake .. \
    -DBUILD_ARCH=x86-64 \
    -DCMAKE_OSX_ARCHITECTURES=x86_64 \
    -DCOMPILE_CUDA=off \
    -DUSE_DOXYGEN=off
fi
rm -f ../src/3rd_party/sentencepiece/version
cmake --build . --config Release -j2
cd ../..

git clone https://github.com/kaldi-asr/kaldi
cd kaldi
git checkout 3ec108da76e3d9dba901fb69f046d0e46170b8e7
git apply $SERENADE_SOURCE_ROOT/patches/kaldi/stateless.patch
cd tools
if [[ `uname` == 'Darwin' ]] ; then
  perl -i -pe"s/-g -O2/-g -O2 -mmacosx-version-min=$osx_version/g" Makefile
fi
make -j2
cd ../src
if [[ `uname` == 'Darwin' ]] ; then
  ./configure --shared --use-cuda=no
  perl -i -pe"s/-O1/-O3 -DNDEBUG -mmacosx-version-min=$osx_version/g" kaldi.mk
else
  ./configure --shared --mathlib=MKL --use-cuda=no
  perl -i -pe's/-O1/-O3 -DNDEBUG/g' kaldi.mk
fi
perl -i -pe's/-g //g' kaldi.mk
make -j clean depend
make -j2
cd ../tools
./extras/install_phonetisaurus.sh
cd ../..

if [[ "$minimal" == "true" ]] ; then
  rm -rf \
    marian/.git \
    marian/build/src \
    marian/build/marian \
    marian/build/marian-* \
    marian/build/spm_* \
    kaldi/.git \
    kaldi/tools/openfst-1.7.2/bin \
    kaldi/tools/openfst-1.7.2/lib/libfstscript.a \
    kaldi/tools/openfst-1.7.2/lib/libfstfarscript.a \
    kaldi/tools/openfst-1.7.2/lib/libfstlookahead.a \
    kaldi/tools/openfst-1.7.2/src/script/.libs \
    kaldi/tools/openfst-1.7.2/src/extensions/lookahead/.libs \
    kaldi/tools/openfst-1.7.2/src/extensions/far/.libs \
    kaldi/tools/openfst-1.7.2/src/extensions/ngram/.libs \
    kaldi/tools/phonetisaurus-g2p/phonetisaurus-*

  find kaldi/src -type f ! -name "*.*" -delete
  find kaldi -type f -name "*.so*" -delete
  find kaldi -type f -name "*.o" -delete
fi

