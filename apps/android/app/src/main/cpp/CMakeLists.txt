cmake_minimum_required(VERSION 3.22.1)
project("ktm_jni" CXX)

# Pre-built Rust library (libktm.so cross-compiled via cargo-ndk)
add_library(ktm SHARED IMPORTED)
set_target_properties(ktm PROPERTIES
    IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libktm.so"
)

# JNI bridge
add_library(ktm_jni SHARED
    ktm_jni.cpp
)

target_include_directories(ktm_jni PRIVATE
    "${CMAKE_SOURCE_DIR}/../../../../../../../../core/ktm-ffi/include"
)

target_link_libraries(ktm_jni
    ktm
    android
    log
)
