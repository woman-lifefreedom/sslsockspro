cmake_minimum_required(VERSION 3.4.1)

project(sslsockspro)

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

set(stunnel_srcs
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/client.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/cron.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/ctx.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/dhparam.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/env.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/fd.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/file.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/libwrap.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/log.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/network.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/options.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/protocol.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/pty.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/resolver.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/ssl.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/sthreads.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/str.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/stunnel.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/tls.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src/verify.c
)

set(stunnel_srcs_extra
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel-extra/ui_unix.c
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel-extra/jnicalls.cpp
)

add_library(stunnel SHARED ${stunnel_srcs} ${stunnel_srcs_extra})

target_include_directories(stunnel PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel/src
    ${CMAKE_CURRENT_SOURCE_DIR}/stunnel-extra/include
)

target_link_libraries(stunnel crypto ssl)

target_compile_options(stunnel PRIVATE 
    -DHAVE_CONFIG_H
    -pthread 
    -Wall 
    -Wextra 
    -Wpedantic
    -Wformat=2
    -Wconversion
    -Wno-long-long
    -Wno-deprecated-declarations
    -fstack-protector
)

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


