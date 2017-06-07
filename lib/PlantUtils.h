#ifndef __PLANT_UTILS__
#define __PLANT_UTILS__

#include <utils/RefBase.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/foundation/AHandler.h>
#include <media/stagefright/foundation/AString.h>


namespace android{

class PlantUtils {
public:
    static sp<AMessage> newAMessage(uint32_t what, const sp<const AHandler> &handler);
    static AString newStringPrintf(const char *format, ...);
};

} // namespace
#endif //__PLANT_UTILS__
