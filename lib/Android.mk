LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
		PlantUtils.cpp				\
        MediaReceiver.cpp               \
        Parameters.cpp                  \
        rtp/RTPAssembler.cpp            \
        rtp/RTPReceiver.cpp             \
        sink/DirectRenderer.cpp         \
        sink/WifiDisplaySink.cpp        \
        TimeSyncer.cpp                  \
        VideoFormats.cpp                \
		ANetworkSession.cpp				\

LOCAL_C_INCLUDES:= \
        $(TOP)/frameworks/av/media/libstagefright \
        $(TOP)/frameworks/native/include/media/openmax \
        $(TOP)/frameworks/av/media/libstagefright/mpeg2ts \

LOCAL_SHARED_LIBRARIES:= \
        libbinder                       \
        libcutils                       \
        liblog                          \
        libgui                          \
        libmedia                        \
        libstagefright                  \
        libstagefright_foundation       \
        libui                           \
        libutils                        \

ifeq ($(PLATFORM_VERSION), 6.0.1)
	LOCAL_CFLAGS += -DANDROID6_0
else
	ifeq ($(PLATFORM_VERSION), 5.1.1)
	LOCAL_CFLAGS += -DANDROID5_1
else
	ifeq ($(PLATFORM_VERSION), 4.4.4)
	LOCAL_CFLAGS += -DANDROID4_4
endif
endif
endif

LOCAL_MODULE:= libstagefright_wfd2

LOCAL_MODULE_TAGS:= optional

include $(BUILD_SHARED_LIBRARY)
