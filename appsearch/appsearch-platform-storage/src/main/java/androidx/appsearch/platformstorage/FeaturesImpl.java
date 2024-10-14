/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.appsearch.platformstorage;

import android.content.Context;
import android.os.Build;
import android.os.ext.SdkExtensions;

import androidx.annotation.NonNull;
import androidx.appsearch.app.Features;
import androidx.appsearch.platformstorage.util.AppSearchVersionUtil;
import androidx.core.util.Preconditions;

/**
 * An implementation of {@link Features}. Feature availability is dependent on Android API
 * level.
 */
final class FeaturesImpl implements Features {
    // Context is used to check mainline module version, as support varies by module version.
    private final Context mContext;

    FeaturesImpl(@NonNull Context context) {
        mContext = Preconditions.checkNotNull(context);
    }

    @Override
    public boolean isFeatureSupported(@NonNull String feature) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // AppSearch platform-storage is not available below Android S.
            return false;
        }
        int tSdkExtensionVersion = SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU);
        switch (feature) {
            // Android T Features
            case Features.ADD_PERMISSIONS_AND_GET_VISIBILITY:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_BY_ID:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK:
                // fall through
            case Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;

            // Android U Features
            case Features.JOIN_SPEC_AND_QUALIFIED_ID:
                // fall through
            case Features.LIST_FILTER_QUERY_LANGUAGE:
                // fall through
            case Features.NUMERIC_SEARCH:
                // fall through
            case Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION:
                // fall through
            case Features.SEARCH_SPEC_PROPERTY_WEIGHTS:
                // fall through
            case Features.SEARCH_SUGGESTION:
                // fall through
            case Features.TOKENIZER_TYPE_RFC822:
                // fall through
            case Features.VERBATIM_SEARCH:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                        || tSdkExtensionVersion >= AppSearchVersionUtil.TExtensionVersions.U_BASE;

            case Features.SET_SCHEMA_CIRCULAR_REFERENCES:
                // This feature is restricted to Android U+ devices only due to rollback
                // compatibility issues. It is not allowed in Android T devices.
                // TODO(b/369703879) Remove this special handling once circular references is
                // backported to Android T devices.
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

            // Features that first landed in T Extensions 10 (Mainline Release M-2023-11) and later
            // in Android V.
            case Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
                        || tSdkExtensionVersion >= AppSearchVersionUtil.TExtensionVersions.M2023_11;
            case Features.SCHEMA_ADD_PARENT_TYPE:
                // Add Parent Type has special handling. Polymorphism was restricted to U+ devices
                // due to rollback compatibility concerns.
                // TODO(b/369703879) Remove this special handling once polymorphism is backported to
                // Android T devices.
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
                        || (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                                && tSdkExtensionVersion
                                        >= AppSearchVersionUtil.TExtensionVersions.M2023_11);

            // Android V Features
            case Features.ENTERPRISE_GLOBAL_SEARCH_SESSION:
                // fall through
            case Features.LIST_FILTER_HAS_PROPERTY_FUNCTION:
                // fall through
            case Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES:
                // fall through
            case Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA:
                // fall through
            case Features.SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG:
                // fall through
            case Features.SET_SCHEMA_REQUEST_ADD_SCHEMA_TYPE_VISIBLE_TO_CONFIG:
                // fall through
            case Features.SET_SCHEMA_REQUEST_SET_PUBLICLY_VISIBLE:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
                        || tSdkExtensionVersion >= AppSearchVersionUtil.TExtensionVersions.V_BASE;

            // Beyond Android V Features
            case Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG:
                // TODO(b/326656531) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SCHEMA_SET_DESCRIPTION:
                // TODO(b/326987971) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS:
                // TODO(b/332620561) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SEARCH_SPEC_ADD_INFORMATIONAL_RANKING_EXPRESSIONS:
                // TODO(b/332642571) : Update when feature is ready in service-appsearch.
                // fall through
            default:
                return false;
        }
    }

    @Override
    public int getMaxIndexedProperties() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return 64;
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            // Sixty-four properties were enabled in mainline module of the U base version
            return AppSearchVersionUtil.getAppSearchVersionCode(mContext)
                    >= AppSearchVersionUtil.MainlineVersions.U_BASE ? 64 : 16;
        } else {
            return 16;
        }
    }
}
