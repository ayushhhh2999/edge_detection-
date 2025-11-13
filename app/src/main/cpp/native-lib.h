#ifndef NATIVE_LIB_H
#define NATIVE_LIB_H

#include <jni.h>

extern "C" {
JNIEXPORT void JNICALL
Java_com_div_edgedetectionapp_NativeLib_processFrame(
        JNIEnv* env,
        jobject thiz,
        jbyteArray input,
        jint width,
        jint height,
        jbyteArray output);
}

#endif // NATIVE_LIB_H
