#include <native_drawing/drawing_font_collection.h>
#include "oh_resource_manager.h"

namespace OH {

// ========== DrawingResourceFactory 实现 ==========

ResourceHandle<OH_Drawing_FontCollection>
DrawingResourceFactory::createFontCollection() {
    OH_Drawing_FontCollection* resource = OH_Drawing_CreateSharedFontCollection();

    return ResourceHandle<OH_Drawing_FontCollection>(
        resource,
        [](OH_Drawing_FontCollection* ptr) {
            if (ptr) {
                OH_Drawing_DestroyFontCollection(ptr);
            }
        }
    );
}

ResourceHandle<OH_Drawing_TypographyStyle>
DrawingResourceFactory::createTypographyStyle() {
    OH_Drawing_TypographyStyle* resource = OH_Drawing_CreateTypographyStyle();

    return ResourceHandle<OH_Drawing_TypographyStyle>(
        resource,
        [](OH_Drawing_TypographyStyle* ptr) {
            if (ptr) {
                OH_Drawing_DestroyTypographyStyle(ptr);
            }
        }
    );
}

ResourceHandle<OH_Drawing_TextStyle>
DrawingResourceFactory::createTextStyle() {
    OH_Drawing_TextStyle* resource = OH_Drawing_CreateTextStyle();

    return ResourceHandle<OH_Drawing_TextStyle>(
        resource,
        [](OH_Drawing_TextStyle* ptr) {
            if (ptr) {
                OH_Drawing_DestroyTextStyle(ptr);
            }
        }
    );
}

ResourceHandle<OH_Drawing_TypographyCreate>
DrawingResourceFactory::createTypographyHandler(
    OH_Drawing_TypographyStyle* typoStyle,
    OH_Drawing_FontCollection* fontCollection
) {
    OH_Drawing_TypographyCreate* resource =
        OH_Drawing_CreateTypographyHandler(typoStyle, fontCollection);

    return ResourceHandle<OH_Drawing_TypographyCreate>(
        resource,
        [](OH_Drawing_TypographyCreate* ptr) {
            if (ptr) {
                OH_Drawing_DestroyTypographyHandler(ptr);
            }
        }
    );
}

ResourceHandle<OH_Drawing_Typography>
DrawingResourceFactory::createTypography(
    OH_Drawing_TypographyCreate* handler
) {
    OH_Drawing_Typography* resource = OH_Drawing_CreateTypography(handler);

    return ResourceHandle<OH_Drawing_Typography>(
        resource,
        [](OH_Drawing_Typography* ptr) {
            if (ptr) {
                OH_Drawing_DestroyTypography(ptr);
            }
        }
    );
}

} // namespace OH 