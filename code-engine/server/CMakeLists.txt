cmake_minimum_required(VERSION 3.15)
set(CMAKE_CXX_STANDARD 14)
project(code-engine)

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

set(MARIAN_ROOT ${SERENADE_LIBRARY_ROOT}/marian)
set(MARIAN_NATIVE_ROOT ${SERENADE_LIBRARY_ROOT}/marian-native)

add_compile_options(
  -Wall
  -Wno-sign-compare
  -Wno-unused-value
  -Wno-unknown-pragmas
  -Wno-uninitialized
  -pthread
  -O3
)

if(APPLE)
  set(CMAKE_OSX_DEPLOYMENT_TARGET "11.0")
  add_compile_options(
    -Wno-deprecated-register
    -Wno-unneeded-internal-declaration
  )
endif()

list(APPEND CMAKE_PREFIX_PATH ${SERENADE_LIBRARY_ROOT}/protobuf)
set(CMAKE_MODULE_PATH "${MARIAN_ROOT}/cmake" ${CMAKE_MODULE_PATH})
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/code-engine)
set(BUILD_SHARED_LIBS FALSE)
if(APPLE)
  set(BUILD_ARCH "x86-64")
  set(CMAKE_OSX_ARCHITECTURES "x86_64")
endif()

if(APPLE)
  include(FindIconv)
  include_directories(Iconv_INCLUDE_DIRS)
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
set(PROTOS
  ${SERENADE_SOURCE_ROOT}/toolbelt/src/main/proto/code-engine.proto
  ${SERENADE_SOURCE_ROOT}/toolbelt/src/main/proto/core.proto
)
protobuf_generate_cpp(PROTO_SRCS PROTO_HEADERS ${PROTOS})
add_library(code-engine_protobuf ${PROTO_HEADERS} ${PROTO_SRCS})

include_directories(${SERENADE_LIBRARY_ROOT}/crow/include)
include_directories(
  ${MARIAN_ROOT}/src
  ${MARIAN_ROOT}/src/3rd_party
)

set(SENTENCEPIECE_ROOT ${SERENADE_LIBRARY_ROOT}/sentencepiece)
include_directories(
  ${SENTENCEPIECE_ROOT}/include
)

include_directories(include)
include_directories(${SERENADE_SOURCE_ROOT}/toolbelt/src/main/cpp/include)
file(GLOB SOURCES src/*.cc src/*/*.cc)
add_executable(serenade-code-engine ${SOURCES} server/code_engine_server.cc)
target_link_libraries(serenade-code-engine
  code-engine_protobuf
  pthread
  ${Protobuf_LIBRARIES}
  ${Boost_LIBRARIES}
)

if(DEFINED ENV{SERENADE_NATIVE})
  target_link_libraries(serenade-code-engine ${MARIAN_NATIVE_ROOT}/build/libmarian.a)
else()
  target_link_libraries(serenade-code-engine ${MARIAN_ROOT}/build/libmarian.a)
endif()

target_link_libraries(serenade-code-engine
  ${SENTENCEPIECE_ROOT}/lib/libsentencepiece.a
  ${SENTENCEPIECE_ROOT}/lib/libsentencepiece_train.a
)

if(APPLE)
  target_link_libraries(serenade-code-engine
    ${Iconv_LIBRARIES}
    "-framework Accelerate"
  )
else()
  target_link_libraries(serenade-code-engine
    "-Wl,--start-group;/opt/intel/mkl/lib/intel64/libmkl_intel_ilp64.a;/opt/intel/mkl/lib/intel64/libmkl_sequential.a;/opt/intel/mkl/lib/intel64/libmkl_core.a;-Wl,--end-group"
    dl
    gomp
  )
  add_executable(config-generator config-generator/config_generator.cc)
  target_link_libraries(config-generator ${MARIAN_ROOT}/build/libmarian.a)
endif()
