cmake_minimum_required(VERSION 3.4.1)

project(sslsockspor)

# Custom function declarations
include(tools.cmake)

# OpenSSL build parametes 
# STATIC or SHARED
SET(SSLLIBTYPE STATIC)
SET(OPENSSL_PATH "openssl")
include(openssl/openssl.cmake)


### Stunnel build

include(ExternalProject)

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(target_autotools "aarch64-linux-android")
endif()
if (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(target_autotools "armv7a-linux-androideabi")
endif()
if (${ANDROID_ABI} STREQUAL "x86_64")
    set(target_autotools "x86_64-linux-android")
endif()
if (${ANDROID_ABI} STREQUAL "x86")
    set(target_autotools "i686-linux-android")
endif()

set(stunnel_module "stunnel-android")
set(stunnel_libfolder "/stunnel/src/stunnel_ext-build/src/.libs")
set(stunnel_install "/stunnel/install")

ExternalProject_Add(stunnel_ext
    # DEPENDS crypto ssl
    SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/${stunnel_module}
    CONFIGURE_COMMAND . ${CMAKE_CURRENT_SOURCE_DIR}/${stunnel_module}/vars/android-sdk-path
    && . ${CMAKE_CURRENT_SOURCE_DIR}/${stunnel_module}/vars/vars-${ANDROID_ABI}
    && ${CMAKE_CURRENT_SOURCE_DIR}/${stunnel_module}/configure
        --with-ssl=${CMAKE_CURRENT_SOURCE_DIR}/openssl 
        --disable-silent-rules
        --host ${target_autotools} 
    BUILD_COMMAND ${MAKE} 
    PREFIX "stunnel"
    ## this is a workaround for ninja that looks for the file before build process begins
    ## https://gitlab.kitware.com/cmake/cmake/-/issues/23056
    BUILD_BYPRODUCTS libstunnel_ar.a
    INSTALL_COMMAND cmake -E echo "moving the built libraries to build folder" 
    && mkdir -p ${CMAKE_CURRENT_BINARY_DIR}${stunnel_install}/lib
    && cp ${CMAKE_CURRENT_BINARY_DIR}${stunnel_libfolder}/libstunnel.a
        ${CMAKE_CURRENT_BINARY_DIR}/libstunnel_ar.a
)

add_library(stunnel_ar
    STATIC
    IMPORTED
)
set_target_properties(stunnel_ar
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_CURRENT_BINARY_DIR}/libstunnel_ar.a
)

# find_library(stunnel_ar stunnel_ar)
# if(NOT stunnel_ar)
#   message(FATAL_ERROR "stunnel archive library not found")
# endif()
add_library(stunnel SHARED stunnel-android/src/jnicalls.cpp)
add_dependencies(stunnel stunnel_ext)
target_include_directories(
    stunnel PRIVATE 
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel-android/src/include
)
target_link_libraries(stunnel ssl stunnel_ar)

