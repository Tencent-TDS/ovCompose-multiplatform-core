#ifndef OH_SYSTRACE_SECTION_H
#define OH_SYSTRACE_SECTION_H
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

    ~SystraceSection() { OH_HiTrace_FinishTrace(); }
};
} // namespace OH

#endif