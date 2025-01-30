/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.autofill

import kotlin.jvm.JvmInline

// TODO https://youtrack.jetbrains.com/issue/CMP-7154/Adopt-Autofill-semantic-properties

actual sealed interface ContentType {
    actual companion object {
        actual val Username: ContentType = PlatformContentType(1)
        actual val Password: ContentType = PlatformContentType(2)
        actual val EmailAddress: ContentType = PlatformContentType(3)
        actual val NewUsername: ContentType = PlatformContentType(4)
        actual val NewPassword: ContentType = PlatformContentType(5)
        actual val PostalAddress: ContentType = PlatformContentType(6)
        actual val PostalCode: ContentType = PlatformContentType(7)
        actual val CreditCardNumber: ContentType = PlatformContentType(8)
        actual val CreditCardSecurityCode: ContentType = PlatformContentType(9)
        actual val CreditCardExpirationDate: ContentType = PlatformContentType(10)
        actual val CreditCardExpirationMonth: ContentType = PlatformContentType(11)
        actual val CreditCardExpirationYear: ContentType = PlatformContentType(12)
        actual val CreditCardExpirationDay: ContentType = PlatformContentType(13)
        actual val AddressCountry: ContentType = PlatformContentType(14)
        actual val AddressRegion: ContentType = PlatformContentType(15)
        actual val AddressLocality: ContentType = PlatformContentType(16)
        actual val AddressStreet: ContentType = PlatformContentType(17)
        actual val AddressAuxiliaryDetails: ContentType = PlatformContentType(18)
        actual val PostalCodeExtended: ContentType = PlatformContentType(19)
        actual val PersonFullName: ContentType = PlatformContentType(20)
        actual val PersonFirstName: ContentType = PlatformContentType(21)
        actual val PersonLastName: ContentType = PlatformContentType(22)
        actual val PersonMiddleName: ContentType = PlatformContentType(23)
        actual val PersonMiddleInitial: ContentType = PlatformContentType(24)
        actual val PersonNamePrefix: ContentType = PlatformContentType(25)
        actual val PersonNameSuffix: ContentType = PlatformContentType(26)
        actual val PhoneNumber: ContentType = PlatformContentType(27)
        actual val PhoneNumberDevice: ContentType = PlatformContentType(28)
        actual val PhoneCountryCode: ContentType = PlatformContentType(29)
        actual val PhoneNumberNational: ContentType = PlatformContentType(30)
        actual val Gender: ContentType = PlatformContentType(31)
        actual val BirthDateFull: ContentType = PlatformContentType(32)
        actual val BirthDateDay: ContentType = PlatformContentType(33)
        actual val BirthDateMonth: ContentType = PlatformContentType(34)
        actual val BirthDateYear: ContentType = PlatformContentType(35)
        actual val SmsOtpCode: ContentType = PlatformContentType(36)
    }
}

@JvmInline
private value class PlatformContentType(val type: Int) : ContentType
