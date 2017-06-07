#include "PlantUtils.h"
#include <utils/RefBase.h>

namespace android {

sp<AMessage> PlantUtils::newAMessage(uint32_t what, const sp<const AHandler> &handler) {
#ifdef ANDROID6_0
        return new AMessage(what, handler);
#else
        return new AMessage(what, handler->id());
#endif
}

AString PlantUtils::newStringPrintf(const char *format, ...) {
    va_list ap;
    va_start(ap, format);
    char *buffer;
    vasprintf(&buffer, format, ap);
    va_end(ap);
    AString result(buffer);
    free(buffer);
    buffer = NULL;
    return result;
}

} // namespace android
