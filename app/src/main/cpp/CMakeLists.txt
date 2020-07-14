cmake_minimum_required(VERSION 3.4.1)

add_library(native-lib SHARED native-lib.cpp)

find_library(jnigraphics-lib jnigraphics)
find_library(log-lib log)

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

add_library(cv_imgcodecs-lib SHARED IMPORTED)
set_target_properties(cv_imgcodecs-lib
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libopencv_imgcodecs.so)

target_link_libraries(native-lib ${jnigraphics-lib} ${log-lib} cv_core-lib cv_imgproc-lib cv_imgcodecs-lib)