package com.avelcam.android.encoder

interface EncodedFrameSink {
    fun onEncodedAccessUnit(accessUnit: EncodedAccessUnit)
    fun onFrameSinkError(error: Throwable) = Unit
}

