package com.div.edgedetectionapp

object NativeLib {
    init {
        System.loadLibrary("native-lib")
    }

    external fun processFrame(input: ByteArray, width: Int, height: Int, output: ByteArray)
}
