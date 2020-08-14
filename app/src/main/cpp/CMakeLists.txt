cmake_minimum_required(VERSION 3.4.1)

set(OPENCV_VERSION 3.4)
add_library(native-lib SHARED stitch_img.cpp)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

# Configure OpenCV
include_directories(SYSTEM ./include)


add_library(cv_imgproc STATIC IMPORTED)
set_target_properties(cv_imgproc
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libopencv_imgproc.a)

add_library(cv_features2d STATIC IMPORTED)
set_target_properties(cv_features2d
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libopencv_features2d.a)

add_library(cv_core STATIC IMPORTED)
set_target_properties(cv_core
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libopencv_core.a)


add_library(z-lib STATIC IMPORTED)
set_target_properties(z-lib
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libzlib.a)

if (${ANDROID_ABI} MATCHES "armeabi-v7a" OR ${ANDROID_ABI} MATCHES "arm64-v8a")
    add_library(tegra_hal STATIC IMPORTED)
    set_target_properties(tegra_hal
            PROPERTIES IMPORTED_LOCATION
            ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libtegra_hal.a)
    target_link_libraries(native-lib ${log-lib} ${jnigraphics-lib}
            cv_features2d cv_imgproc cv_core z-lib tegra_hal)

elseif (${ANDROID_ABI} MATCHES "x86" OR ${ANDROID_ABI} MATCHES "x86_64")
    target_link_libraries(native-lib ${log-lib} ${jnigraphics-lib}
            cv_features2d cv_imgproc cv_core z-lib)
endif ()