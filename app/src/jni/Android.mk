LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := doJNI
LOCAL_SRC_FILES := doJNI.c zhc.c Base128Lib.c qmcLib.c
include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)