LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

ifeq ($(PLATFORM_VERSION), 6.0.1)
WFDSINK_JAVA_PATH := "$(LOCAL_PATH)/src/com/lc/wifidisplaysink"
$(info $(shell cp $(WFDSINK_JAVA_PATH)/HidDeviceAdapterService.java.stub $(WFDSINK_JAVA_PATH)/HidDeviceAdapterService.java))
endif

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_JAVA_LIBRARIES := com.broadcom.bt javax.obex

LOCAL_PACKAGE_NAME := WifiDisplaySink
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_JNI_SHARED_LIBRARIES := libWifiDisplaySink
LOCAL_REQUIRED_MODULES := libWifiDisplaySink

include $(BUILD_PACKAGE)
include $(call all-makefiles-under, $(LOCAL_PATH))
