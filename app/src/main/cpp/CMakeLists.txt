cmake_minimum_required(VERSION 3.10.2)
project("edge_detection")

# Path to OpenCV Android SDK
set(OpenCV_DIR "D:/C DRIVE LIBRARY/Downloads/opencv-4.10.0-android-sdk/OpenCV-android-sdk/sdk/native/jni")
find_package(OpenCV REQUIRED)

# Build native library
add_library(
        native-lib
        SHARED
        native-lib.cpp
)

include_directories(
        ${OpenCV_INCLUDE_DIRS}
        src/main/cpp
)

target_link_libraries(
        native-lib
        ${OpenCV_LIBS}
        log
)
