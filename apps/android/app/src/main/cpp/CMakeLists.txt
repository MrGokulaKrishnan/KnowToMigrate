cmake_minimum_required(VERSION 3.22.1)
project("ktm_jni" CXX C)

# JNI bridge with native C implementations
add_library(ktm_jni SHARED
    ktm_jni.cpp
    ktm_stubs.c
)

target_include_directories(ktm_jni PRIVATE
    "${CMAKE_SOURCE_DIR}/../../../../../../../../core/ktm-ffi/include"
)

target_link_libraries(ktm_jni
    android
    log
)
