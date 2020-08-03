cmake_minimum_required(VERSION 3.4.1)

add_library(native-lib SHARED stitch_img.cpp)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics )


# Configure OpenCV
include_directories(SYSTEM ./include)

add_library(cv_core-lib SHARED IMPORTED)
set_target_properties(cv_core-lib
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libopencv_core.so)

add_library(cv_imgproc-lib SHARED IMPORTED)
set_target_properties(cv_imgproc-lib
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libopencv_imgproc.so)

add_library(cv_features2d-lib SHARED IMPORTED)
set_target_properties(cv_features2d-lib
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libopencv_features2d.so)


target_link_libraries(native-lib ${log-lib} ${jnigraphics-lib} cv_core-lib
                        cv_imgproc-lib cv_features2d-lib)