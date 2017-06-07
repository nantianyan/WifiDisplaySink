LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libWifiDisplaySink
LOCAL_SRC_FILES := JniWifiDisplaySink.cpp

LOCAL_C_INCLUDES += \
    $(JNI_H_INCLUDE) \
    $(TOP)/frameworks/native/include \
    $(TOP)/frameworks/native/include/media/openmax \
    $(TOP)/frameworks/base/include \
    $(TOP)/frameworks/av/include/media/stagefright/foundation \
    $(TOP)/leadcore/packages/apps/WifiDisplaySink/lib

LOCAL_SHARED_LIBRARIES:= \
    libbinder                       \
    libgui                          \
    libmedia                        \
    libstagefright                  \
    libstagefright_foundation       \
    libstagefright_wfd2              \
    libutils                        \
    libcutils                       \
    libandroid_runtime				\
    libnativehelper					\

LOCAL_CERTIFICATE := platform

include $(BUILD_SHARED_LIBRARY)
