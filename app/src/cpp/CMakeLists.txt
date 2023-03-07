cmake_minimum_required(VERSION 3.4.1)

project(sslsockspor)

# Custom function declarations
include(tools.cmake)

# OpenSSL build parametes 
# STATIC or SHARED
SET(SSLLIBTYPE STATIC)
SET(OPENSSL_PATH "openssl")
include(openssl/openssl.cmake)

#################
# STUNNEL BUILD #
#################
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
# END OF STUNNEL BUILD

###########################
#  OPENVPN3 Related build #
###########################

include(GetGitRevisionDescription.cmake)
git_describe(OPENVPN3_GIT "${CMAKE_CURRENT_SOURCE_DIR}/openvpn3" "--tags" "--always" "--long")
message("OpenVPN 3.x version ${OPENVPN3_GIT}")


# dependencies
include(lzo.cmake)
include(lz4.cmake)

FIND_PACKAGE(SWIG 3.0 REQUIRED)

add_custom_command(OUTPUT "ovpncli_wrap.cxx"
        COMMAND ${CMAKE_COMMAND} -E make_directory ovpn3
        COMMAND ${SWIG_EXECUTABLE} -outdir ovpn3
        -c++
        -java -package net.openvpn.ovpn3
        -outcurrentdir
        -DOPENVPN_PLATFORM_ANDROID
        -I${CMAKE_SOURCE_DIR}/openvpn3/client
        -I${CMAKE_SOURCE_DIR}/openvpn3
            ${CMAKE_SOURCE_DIR}/openvpn3/client/ovpncli.i)

set(ovpn3_SRCS
        openvpn3/client/ovpncli.cpp
        openvpn3/openvpn/openssl/xkey/xkey_provider.c
        openvpn3/openvpn/openssl/xkey/xkey_helper.c
        ovpncli_wrap.cxx)

add_library(ovpn3 SHARED ${ovpn3_SRCS})

target_include_directories(ovpn3 PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/lzo/include
        ${CMAKE_CURRENT_SOURCE_DIR}/openvpn3
        ${CMAKE_CURRENT_SOURCE_DIR}/asio/asio/include
        ${CMAKE_CURRENT_SOURCE_DIR}/openvpn3/client
        #${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/include
        )

target_compile_definitions(ovpn3 PRIVATE
        -DUSE_OPENSSL
        )
target_link_libraries(ovpn3 crypto ssl lzo lz4)

target_compile_definitions(ovpn3 PRIVATE
        -DHAVE_CONFIG_H
        -DHAVE_LZO
        -DHAVE_LZ4
        -DASIO_STANDALONE
        -DUSE_ASIO
        -DOPENVPN_CORE_GIT_VERSION=\"${OPENVPN3_GIT}\"
        -DOPENVPN_SHOW_SESSION_TOKEN
        -DOPENSSL_API_COMPAT=0x10200000L
        -DOPENVPN_ALLOW_INSECURE_CERTPROFILE
         -DENABLE_EXTERNAL_PKI
        )


