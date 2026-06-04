# the minimum version of CMake.
cmake_minimum_required(VERSION 3.5.0)
project(compose_arkui_utils)

if(DEFINED INCLUDE_DIRS)
    foreach(include_path ${INCLUDE_DIRS})
        include_directories(${include_path})
    endforeach()
endif()

set(NATIVERENDER_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})

if(DEFINED PACKAGE_FIND_FILE)
    include(${PACKAGE_FIND_FILE})
endif()

#add_definitions(-DDEBUG) # Add DEBUG definition
add_definitions(-std=c++17)

include_directories(${NATIVERENDER_ROOT_PATH}
                    ${NATIVERENDER_ROOT_PATH}/include)

file(
GLOB_RECURSE SOURCES
${NATIVERENDER_ROOT_PATH}/compose/*
)

find_library(
    # Sets the name of the path variable.
    EGL-lib
    # Specifies the name of the NDK library that
    # you want CMake to locate.
    EGL
)

find_library(
    # Sets the name of the path variable.
    GLES-lib
    # Specifies the name of the NDK library that
    # you want CMake to locate.
    GLESv3
)

find_library(
    # Sets the name of the path variable.
    libace-lib
    # Specifies the name of the NDK library that
    # you want CMake to locate.
    ace_ndk.z
)

find_library(
    # Sets the name of the path variable.
    hilog-lib
    # Specifies the name of the NDK library that
    # you want CMake to locate.
    hilog_ndk.z
)

find_library(
    # Sets the name of the path variable.
    libnapi-lib
    # Specifies the name of the NDK library that
    # you want CMake to locate.
    ace_napi.z
)

find_library(
    # Sets the name of the path variable.
    libuv-lib
    # Specifies the name of the NDK library that
    # you want CMake to locate.
    uv
)

add_library(compose_arkui_utils STATIC napi_init.cpp ${SOURCES})
target_link_libraries(compose_arkui_utils PUBLIC libace_napi.z.so)
target_link_libraries(compose_arkui_utils PUBLIC ${EGL-lib} ${GLES-lib} ${hilog-lib} ${libace-lib} ${libnapi-lib} ${libuv-lib} libc++_shared.so)
