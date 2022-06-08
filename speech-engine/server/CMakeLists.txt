cmake_minimum_required(VERSION 3.15)
set(CMAKE_CXX_STANDARD 17)
project(speech-engine)

if(DEFINED ENV{SERENADE_SOURCE_ROOT})
  set(SERENADE_SOURCE_ROOT $ENV{SERENADE_SOURCE_ROOT})
else()
  set(SERENADE_SOURCE_ROOT $ENV{HOME}/serenade)
endif()

if(DEFINED ENV{SERENADE_LIBRARY_ROOT})
  set(SERENADE_LIBRARY_ROOT $ENV{SERENADE_LIBRARY_ROOT})
else()
  set(SERENADE_LIBRARY_ROOT $ENV{HOME}/libserenade)
endif()

# some options carried over from Kaldi
add_compile_options(
  -Wall
  -Wno-sign-compare
  -Wno-unused-local-typedefs
  -Wno-deprecated-declarations
  -Winit-self
  -DKALDI_DOUBLEPRECISION=0
  -DHAVE_EXECINFO_H=1
  -DHAVE_CXXABI_H
  -pthread
  -O3
)

if(APPLE)
  set(CMAKE_OSX_DEPLOYMENT_TARGET "11.0")
  add_compile_options(
    -Wno-reorder-ctor
    -Wno-dangling-gsl
  )
else()
  add_compile_options(
    -Wno-reorder
  )
endif()

list(APPEND CMAKE_PREFIX_PATH ${SERENADE_LIBRARY_ROOT}/protobuf)
set(CMAKE_MODULE_PATH "${MARIAN_ROOT}/cmake" ${CMAKE_MODULE_PATH})
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/lib)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/lib)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/speech-engine)
if(APPLE)
  set(BUILD_ARCH "x86-64")
  set(CMAKE_OSX_ARCHITECTURES "x86_64")
endif()

find_package(Threads)
set(Boost_USE_STATIC_LIBS ON)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)
find_package(Boost 1.78.0 COMPONENTS regex system thread PATHS ${SERENADE_LIBRARY_ROOT}/boost)
include_directories(${Boost_INCLUDE_DIRS})

set(Protobuf_USE_STATIC_LIBS ON)
include(FindProtobuf)
find_package(Protobuf REQUIRED)
include_directories(${PROTOBUF_INCLUDE_DIR})
include_directories(${CMAKE_CURRENT_BINARY_DIR})
set(PROTOS ${SERENADE_SOURCE_ROOT}/toolbelt/src/main/proto/speech-engine.proto)
protobuf_generate_cpp(PROTO_SRCS PROTO_HEADERS ${PROTOS})
add_library(speech-engine_protobuf ${PROTO_HEADERS} ${PROTO_SRCS})

set(KALDI_DIR ${SERENADE_LIBRARY_ROOT}/kaldi)
set(KALDI_TOOLS_DIR ${KALDI_DIR}/tools)
set(KALDI_SRC_DIR ${KALDI_DIR}/src)
set(OPENFST_LIB_DIR ${KALDI_TOOLS_DIR}/openfst-1.7.2/lib)

include_directories(${SERENADE_LIBRARY_ROOT}/crow/include)
include_directories(
  ${KALDI_SRC_DIR}
  SYSTEM ${KALDI_TOOLS_DIR}/openfst/include
  ${KALDI_TOOLS_DIR}/phonetisaurus-g2p/src
  ${KALDI_TOOLS_DIR}/phonetisaurus-g2p/src/3rdparty/utfcpp/
)

include_directories(include)
include_directories(${SERENADE_SOURCE_ROOT}/toolbelt/src/main/cpp/include)
file(GLOB SOURCES src/*.cc src/*/*.cc)

find_library(LIB_KALDI_BASE NAMES kaldi-base.a PATHS ${KALDI_SRC_DIR}/base)
find_library(LIB_KALDI_CHAIN NAMES kaldi-chain.a PATHS ${KALDI_SRC_DIR}/chain)
find_library(LIB_KALDI_CUDAMATRIX NAMES kaldi-cudamatrix.a PATHS ${KALDI_SRC_DIR}/cudamatrix)
find_library(LIB_KALDI_DECODER NAMES kaldi-decoder.a PATHS ${KALDI_SRC_DIR}/decoder)
find_library(LIB_KALDI_FEAT NAMES kaldi-feat.a PATHS ${KALDI_SRC_DIR}/feat)
find_library(LIB_KALDI_FSTEXT NAMES kaldi-fstext.a PATHS ${KALDI_SRC_DIR}/fstext)
find_library(LIB_KALDI_GMM NAMES kaldi-gmm.a PATHS ${KALDI_SRC_DIR}/gmm)
find_library(LIB_KALDI_HMM NAMES kaldi-hmm.a PATHS ${KALDI_SRC_DIR}/hmm)
find_library(LIB_KALDI_IVECTOR NAMES kaldi-ivector.a PATHS ${KALDI_SRC_DIR}/ivector)
find_library(LIB_KALDI_LAT NAMES kaldi-lat.a PATHS ${KALDI_SRC_DIR}/lat)
find_library(LIB_KALDI_LM NAMES kaldi-lm.a PATHS ${KALDI_SRC_DIR}/lm)
find_library(LIB_KALDI_MATRIX NAMES kaldi-matrix.a PATHS ${KALDI_SRC_DIR}/matrix)
find_library(LIB_KALDI_NNET2 NAMES kaldi-nnet2.a PATHS ${KALDI_SRC_DIR}/nnet2)
find_library(LIB_KALDI_NNET3 NAMES kaldi-nnet3.a PATHS ${KALDI_SRC_DIR}/nnet3)
find_library(LIB_KALDI_ONLINE2 NAMES kaldi-online2.a PATHS ${KALDI_SRC_DIR}/online2)
find_library(LIB_KALDI_TRANSFORM NAMES kaldi-transform.a PATHS ${KALDI_SRC_DIR}/transform)
find_library(LIB_KALDI_TREE NAMES kaldi-tree.a PATHS ${KALDI_SRC_DIR}/tree)
find_library(LIB_KALDI_UTIL NAMES kaldi-util.a PATHS ${KALDI_SRC_DIR}/util)
find_library(LIB_FST NAMES libfst.a PATHS ${OPENFST_LIB_DIR})
set(KALDI_LIBRARIES
  ${LIB_KALDI_BASE}
  ${LIB_KALDI_CHAIN}
  ${LIB_KALDI_CUDAMATRIX}
  ${LIB_KALDI_DECODER}
  ${LIB_KALDI_FEAT}
  ${LIB_KALDI_FSTEXT}
  ${LIB_KALDI_GMM}
  ${LIB_KALDI_HMM}
  ${LIB_KALDI_IVECTOR}
  ${LIB_KALDI_LAT}
  ${LIB_KALDI_LM}
  ${LIB_KALDI_MATRIX}
  ${LIB_KALDI_NNET2}
  ${LIB_KALDI_NNET3}
  ${LIB_KALDI_ONLINE2}
  ${LIB_KALDI_TRANSFORM}
  ${LIB_KALDI_TREE}
  ${LIB_KALDI_UTIL}
  ${LIB_FST}
)

add_executable(serenade-speech-engine ${SOURCES} server/speech_engine_server.cc)
target_link_libraries(serenade-speech-engine
  speech-engine_protobuf
  pthread
  z
  ${Protobuf_LIBRARIES}
  ${Boost_LIBRARIES}
  ${KALDI_LIBRARIES}
  ${KALDI_LIBRARIES}
)

if(APPLE)
  target_link_libraries(serenade-speech-engine "-framework Accelerate")
else()
  target_link_libraries(serenade-speech-engine
    "-Wl,--start-group;/opt/intel/mkl/lib/intel64/libmkl_intel_ilp64.a;/opt/intel/mkl/lib/intel64/libmkl_sequential.a;/opt/intel/mkl/lib/intel64/libmkl_core.a;-Wl,--end-group"
    dl
    gomp
  )
endif()
