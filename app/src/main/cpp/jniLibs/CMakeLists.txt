cmake_minimum_required(VERSION 3.4.1)

add_library( native-lib SHARED native-lib.cpp )

find_library( jnigraphics-lib jnigraphics )

# Configure OpenCV
include_directories(SYSTEM /home/cpu11499-local/StitchScreenshot/opencv/build/x86/install/sdk/native/jni/include )

add_library( cv_core-lib SHARED IMPORTED)
set_target_properties( cv_core-lib
        PROPERTIES IMPORTED_LOCATION
        /home/cpu11499-local/StitchScreenshot/opencv/build/x86/install/sdk/native/libs/x86/libopencv_core.so )

add_library( cv_imgproc-lib SHARED IMPORTED)
set_target_properties( cv_imgproc-lib
        PROPERTIES IMPORTED_LOCATION
        /home/cpu11499-local/StitchScreenshot/opencv/build/x86/install/sdk/native/libs/x86/libopencv_imgproc.so )

add_library( cv_imgcodecs-lib SHARED IMPORTED)
set_target_properties( cv_imgcodecs-lib
        PROPERTIES IMPORTED_LOCATION
        /home/cpu11499-local/StitchScreenshot/opencv/build/x86/install/sdk/native/libs/x86/libopencv_imgcodecs.so )

target_link_libraries( native-lib ${jnigraphics-lib} cv_core-lib cv_imgproc-lib cv_imgcodecs-lib)