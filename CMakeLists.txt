cmake_minimum_required (VERSION 2.8.6)
project (perf-map-agent)

# uncomment to make a debug build (including source positions and symbols)
# set(CMAKE_BUILD_TYPE DEBUG)

set(OUTDIR ${PROJECT_BINARY_DIR}/out)
set(LIBRARY_OUTPUT_PATH ${OUTDIR})

find_package(JNI)
if (JNI_FOUND)
    message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
    message (STATUS "JAVA_INCLUDE_PATH=${JAVA_INCLUDE_PATH}")
    message (STATUS "JAVA_INCLUDE_PATH2=${JAVA_INCLUDE_PATH2}")
endif()
INCLUDE_DIRECTORIES(${JAVA_INCLUDE_PATH})
INCLUDE_DIRECTORIES(${JAVA_INCLUDE_PATH2})

add_library(perfmap SHARED src/main/native/perf-map-agent.c src/main/native/perf-map-file.c)
