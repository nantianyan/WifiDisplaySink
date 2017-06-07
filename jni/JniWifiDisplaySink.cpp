#include <jni.h>
#include <string.h>
#include <cstring>

//#define LOG_NDEBUG 0
#define LOG_TAG "JniWifiDisplaySink"
#include <utils/Log.h>
#include <utils/RefBase.h>

#include "sink/WifiDisplaySink.h"

#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <gui/ISurfaceComposer.h>
#include <gui/SurfaceComposerClient.h>
#include <media/AudioSystem.h>
#include <media/IMediaPlayerService.h>
#include <media/IRemoteDisplay.h>
#include <media/IRemoteDisplayClient.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <ui/DisplayInfo.h>
#include <system/window.h>
#include <cutils/properties.h>
#include <assert.h>
#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/android_view_Surface.h"
#include "android_runtime/AndroidRuntime.h"
#include "android_runtime/Log.h"


using namespace android;

struct fields_t {
    jfieldID    context;
    jmethodID   post_event;
};
static fields_t fields;

class JniWfdSinkListener;

struct WfdNativeContext: public RefBase {
    sp<WifiDisplaySink> mWifiDisplaySink;
    sp<SurfaceComposerClient> mSurfaceComposerClient;
    sp<SurfaceControl> mSurfaceControl;
    sp<Surface> mSurface;
    sp<ALooper> mLooper;
    sp<JniWfdSinkListener> mListener;
    WfdNativeContext() {
        mWifiDisplaySink = NULL;
        mSurfaceComposerClient = NULL;
        mSurfaceControl = NULL;
        mSurface = NULL;
        mLooper = NULL;
    }
    ~WfdNativeContext() {
        mWifiDisplaySink.clear();
        mSurface.clear();
        mSurfaceControl.clear();
        mSurfaceComposerClient.clear();
        mLooper.clear();
    }
};

static WfdNativeContext *getWfdNativeContext(JNIEnv* env, jobject thiz)
{
    ALOGD("getWfdNativeContext");
    WfdNativeContext* context = reinterpret_cast<WfdNativeContext*>(env->GetLongField(thiz, fields.context));
    return context;
}

static void prepareWfdNativeContext(JNIEnv* env, jobject thiz)
{
    ALOGD("prepareWfdNativeContext");
    sp<WfdNativeContext> context = new WfdNativeContext();
    context->incStrong((void*)prepareWfdNativeContext);

    env->SetLongField(thiz, fields.context, (jlong)context.get());
}

class JniWfdSinkListener: public WfdSinkListener
{
public:
    JniWfdSinkListener(JNIEnv* env, jobject thiz, jobject weak_thiz);
    ~JniWfdSinkListener();
    virtual void notify(int msg, int ext1, int ext2, const Parcel *obj = NULL);
private:
    JniWfdSinkListener();
    jclass      mClass;
    jobject     mObject;
};

JniWfdSinkListener::JniWfdSinkListener(JNIEnv* env, jobject thiz, jobject weak_thiz) {
    jclass clazz = env->GetObjectClass(thiz);
    if (clazz == NULL) {
        ALOGE("Can't find com/lc/WifiDisplaySink/WifiDisplaySink");
        jniThrowException(env, "java/lang/Exception", NULL);
        return;
    }

    ALOGD("inc GlobalRef about thiz and weak_thiz");
    mClass = (jclass)env->NewGlobalRef(clazz);
    mObject  = env->NewGlobalRef(weak_thiz);
}

JniWfdSinkListener::~JniWfdSinkListener(){
    ALOGD("~JniWfdSinkListener");
    JNIEnv *env = AndroidRuntime::getJNIEnv();
    env->DeleteGlobalRef(mObject);
    env->DeleteGlobalRef(mClass);
}

void JniWfdSinkListener::notify(int msg, int ext1, int ext2, const Parcel *obj) {
    JNIEnv *env = AndroidRuntime::getJNIEnv();

    ALOGD("notify %d, %d, %d", msg, ext1, ext2);

    if (mObject == NULL) {
        ALOGW("callback on dead wfdsink object");
        return;
    }

    env->CallStaticVoidMethod(mClass, fields.post_event, mObject,
            msg, ext1, ext2, NULL);

    if (env->ExceptionCheck()) {
        ALOGW("An exception occurred while notifying an event.");
        env->ExceptionClear();
    }
}

static int32_t GetInt32Property(
                const char *propName, int32_t defaultValue) {
    char val[PROPERTY_VALUE_MAX];
    if (property_get(propName, val, NULL)) {
        char *end;
        unsigned long x = strtoul(val, &end, 10);

        if (*end == '\0' && end > val && x > 0) {
            return x;
        }
    }

    return defaultValue;
}

static void getDisplayDimensions(ssize_t *w, ssize_t *h) {
    sp<IBinder> display(SurfaceComposerClient::getBuiltInDisplay(ISurfaceComposer::eDisplayIdMain));
    DisplayInfo info;
    SurfaceComposerClient::getDisplayInfo(display, &info);
    bool rotate = true;
    int dimentionRot = GetInt32Property("media.wfd.sink.dimention", 0);
    ALOGI("set buffer dimention: %d", dimentionRot);
    if (dimentionRot > 0) {
        *w = info.h;
        *h = info.w;
    } else {
        *w = info.w;
        *h = info.h;
    }
}

static void
com_lc_wifidisplaysink_WifiDisplaySink_native_setup(JNIEnv *env, jobject thiz, jobject weak_this, jint special, jint is_N10)
{

    ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    bool specialMode = special == 1;
    bool isN10 = is_N10 == 1;

    /*sp<SurfaceComposerClient> composerClient = new SurfaceComposerClient;
    CHECK_EQ(composerClient->initCheck(), (status_t)OK);

    ssize_t displayWidth = 0;
    ssize_t displayHeight = 0;
    getDisplayDimensions(&displayWidth, &displayHeight);

    ALOGD("Sink Display[%d, %d] Special[%d] Nexus10[%d]", displayWidth, displayHeight, specialMode, isN10);

    sp<SurfaceControl> control =
        composerClient->createSurface(
                String8("A Sink Surface"),
//                displayWidth,
//                displayHeight,
                isN10 ? displayHeight : displayWidth,
                isN10 ? displayWidth : displayHeight,
                PIXEL_FORMAT_RGB_565,
                0);


    CHECK(control != NULL);
    CHECK(control->isValid());

    SurfaceComposerClient::openGlobalTransaction();
    CHECK_EQ(control->setLayer(INT_MAX), (status_t)OK);
    CHECK_EQ(control->show(), (status_t)OK);
    SurfaceComposerClient::closeGlobalTransaction();

    sp<Surface> surface = control->getSurface();
    CHECK(surface != NULL);*/

    sp<ANetworkSession> session = new ANetworkSession;
    session->start();

    sp<ALooper> looper = new ALooper;

    sp<WifiDisplaySink> sink = new WifiDisplaySink(
            looper,
            specialMode ? WifiDisplaySink::FLAG_SPECIAL_MODE : 0 /* flags */,
            session);

    looper->registerHandler(sink);

    sp<JniWfdSinkListener> listener = new JniWfdSinkListener(env, thiz, weak_this);
    sink->setListener(listener);

    prepareWfdNativeContext(env, thiz);
    WfdNativeContext *ctx = getWfdNativeContext(env, thiz);
    //ctx->mSurfaceComposerClient = composerClient;
    //ctx->mSurfaceControl = control;
    //ctx->mSurface = surface;
    ctx->mWifiDisplaySink = sink;
    ctx->mLooper = looper;
    //ctx->mListener = listener;

    looper->start(false /* runOnCallingThread */, true /*canCallJava*/ );

    ALOGD("setup finished");
    //composerClient->dispose();

}

static void
com_lc_wifidisplaysink_WifiDisplaySink_nativeInvokeSink(JNIEnv* env, jobject thiz, jstring ipaddr, jint port) {
    const char *ip = env->GetStringUTFChars(ipaddr, NULL);
    ALOGD("Source Addr[%s] Port[%d]", ip, port);

    WfdNativeContext *ctx = getWfdNativeContext(env, thiz);
    sp<WifiDisplaySink> sink = ctx->mWifiDisplaySink;
    if (sink == NULL) {
        ALOGE("should call setup first.");
        return;
    }


    ALOGD("start sink");
    sink->start(ip, port);
}

static void
com_lc_wifidisplaysink_WifiDisplaySink_native_init(JNIEnv *env) {
    jclass clazz;

    clazz = env->FindClass("com/lc/wifidisplaysink/WifiDisplaySink");
    if (clazz == NULL) {
        return;
    }

    fields.context = env->GetFieldID(clazz, "mNativeContext", "J");
    ALOGD("fields.context : %lld", fields.context);
    if (fields.context == NULL) {
        return;
    }
    fields.post_event = env->GetStaticMethodID(clazz, "postEventFromNative",
                           "(Ljava/lang/Object;IIILjava/lang/Object;)V");
    if (fields.post_event == NULL) {
        ALOGE("postEventFromNative is NULL");
        return;
    }

    env->DeleteLocalRef(clazz);
}

static void
com_lc_wifidisplaysink_WifiDisplaySink_native_setVideoSurface(JNIEnv *env, jobject thiz, jobject jsurface)
{
    ALOGD("setVideoSurface");
    sp<IGraphicBufferProducer> new_st;
    if (jsurface) {
        sp<Surface> surface(android_view_Surface_getSurface(env, jsurface));
        if (surface != NULL) {
            new_st = surface->getIGraphicBufferProducer();
            if (new_st == NULL) {
                jniThrowException(env, "java/lang/IllegalArgumentException",
                    "The surface does not have a binding SurfaceTexture!");
                return;
            }
            WfdNativeContext *ctx = getWfdNativeContext(env, thiz);
            ctx->mSurface = surface;
            sp<WifiDisplaySink> sink = ctx->mWifiDisplaySink;
            if (sink == NULL) {
                ALOGE("should call setup first.");
                return;
            }
            sink->setDisplay(new_st);
        } else {
            jniThrowException(env, "java/lang/IllegalArgumentException",
                    "The surface has been released");
            return;
        }
    }
}

static JNINativeMethod gMethods[] = {
    {"native_init", "()V", (void *)com_lc_wifidisplaysink_WifiDisplaySink_native_init},
    {"native_setup", "(Ljava/lang/Object;II)V", (void *)com_lc_wifidisplaysink_WifiDisplaySink_native_setup},
    {
        "native_invokeSink",
        "(Ljava/lang/String;I)V",
        (void *)com_lc_wifidisplaysink_WifiDisplaySink_nativeInvokeSink
    },
    {"native_setVideoSurface", "(Landroid/view/Surface;)V", (void *)com_lc_wifidisplaysink_WifiDisplaySink_native_setVideoSurface},
};

jint JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed\n");
        return -1;
    }

    assert(env != NULL);
    int ret = AndroidRuntime::registerNativeMethods(env,
       "com/lc/wifidisplaysink/WifiDisplaySink", gMethods, NELEM(gMethods));
    if (ret < 0) {
        ALOGE("ERROR: registerNativeMethods failed\n");
        return ret;
    } else {
        return JNI_VERSION_1_4;
    }

}

