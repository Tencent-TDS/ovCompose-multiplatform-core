#ifndef OH_SYSTRACE_SECTION_H
#define OH_SYSTRACE_SECTION_H

#ifdef ENABLE_TRACE

#include <hitrace/trace.h>
#include <sstream>
#include <string>

namespace OH {
struct SystraceSection {
public:
    template <typename... ConvertsToStringPiece>
    explicit SystraceSection(const char *name, ConvertsToStringPiece &&...args) {
        std::ostringstream oss;
        (oss << ... << args);
        std::string result = std::string(name) + oss.str();
        OH_HiTrace_StartTrace(result.c_str());
    }

    ~SystraceSection() {
        OH_HiTrace_FinishTrace();
    }
};
} // namespace OH

#else

namespace OH {
struct SystraceSection {
public:
    template <typename... ConvertsToStringPiece>
    explicit SystraceSection(const char *name, ConvertsToStringPiece &&...args) {
        // No-op in non-DEBUG builds
    }

    ~SystraceSection() {
        // No-op in non-DEBUG builds
    }
};
} // namespace OH

#endif // ENABLE_TRACE 

#endif // OH_SYSTRACE_SECTION_H