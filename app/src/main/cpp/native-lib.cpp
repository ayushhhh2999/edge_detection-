#include <jni.h>
#include <opencv2/opencv.hpp>
#include <cstring>

using namespace cv;

extern "C" {

/**
 * Handles a single camera frame from Android and applies edge detection.
 * Input and output are both grayscale byte arrays.
 */
JNIEXPORT void JNICALL
Java_com_yourname_visionfx_NativeBridge_handleFrame(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray frameIn,
        jint frameWidth,
        jint frameHeight,
        jbyteArray frameOut) {

    // Access raw byte arrays from Java
    jbyte* srcData = env->GetByteArrayElements(frameIn, nullptr);
    jbyte* dstData = env->GetByteArrayElements(frameOut, nullptr);

    // Wrap the incoming data as an OpenCV matrix
    Mat srcGray(frameHeight, frameWidth, CV_8UC1,
                reinterpret_cast<unsigned char*>(srcData));

    // Perform edge detection
    Mat edgeMask;
    const double lowThreshold = 85.0;
    const double highThreshold = 160.0;
    Canny(srcGray, edgeMask, lowThreshold, highThreshold);

    // Copy processed pixels back into output array
    std::memcpy(dstData, edgeMask.data, static_cast<size_t>(frameWidth * frameHeight));

    // Release JNI resources
    env->ReleaseByteArrayElements(frameIn, srcData, JNI_ABORT);
    env->ReleaseByteArrayElements(frameOut, dstData, 0);
}

} // extern "C"
