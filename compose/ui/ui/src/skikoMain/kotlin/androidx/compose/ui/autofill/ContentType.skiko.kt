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

// TODO https://youtrack.jetbrains.com/issue/CMP-7154/Adopt-Autofill-semantic-properties

actual class ContentType actual constructor(contentHint: String) {
    actual companion object {
        actual val Username: ContentType = throw NotImplementedError()
        actual val Password: ContentType = throw NotImplementedError()
        actual val EmailAddress: ContentType = throw NotImplementedError()
        actual val NewUsername: ContentType = throw NotImplementedError()
        actual val NewPassword: ContentType = throw NotImplementedError()
        actual val PostalAddress: ContentType = throw NotImplementedError()
        actual val PostalCode: ContentType = throw NotImplementedError()
        actual val CreditCardNumber: ContentType = throw NotImplementedError()
        actual val CreditCardSecurityCode: ContentType = throw NotImplementedError()
        actual val CreditCardExpirationDate: ContentType = throw NotImplementedError()
        actual val CreditCardExpirationMonth: ContentType = throw NotImplementedError()
        actual val CreditCardExpirationYear: ContentType = throw NotImplementedError()
        actual val CreditCardExpirationDay: ContentType = throw NotImplementedError()
        actual val AddressCountry: ContentType = throw NotImplementedError()
        actual val AddressRegion: ContentType = throw NotImplementedError()
        actual val AddressLocality: ContentType = throw NotImplementedError()
        actual val AddressStreet: ContentType = throw NotImplementedError()
        actual val AddressAuxiliaryDetails: ContentType = throw NotImplementedError()
        actual val PostalCodeExtended: ContentType = throw NotImplementedError()
        actual val PersonFullName: ContentType = throw NotImplementedError()
        actual val PersonFirstName: ContentType = throw NotImplementedError()
        actual val PersonLastName: ContentType = throw NotImplementedError()
        actual val PersonMiddleName: ContentType = throw NotImplementedError()
        actual val PersonMiddleInitial: ContentType = throw NotImplementedError()
        actual val PersonNamePrefix: ContentType = throw NotImplementedError()
        actual val PersonNameSuffix: ContentType = throw NotImplementedError()
        actual val PhoneNumber: ContentType = throw NotImplementedError()
        actual val PhoneNumberDevice: ContentType = throw NotImplementedError()
        actual val PhoneCountryCode: ContentType = throw NotImplementedError()
        actual val PhoneNumberNational: ContentType = throw NotImplementedError()
        actual val Gender: ContentType = throw NotImplementedError()
        actual val BirthDateFull: ContentType = throw NotImplementedError()
        actual val BirthDateDay: ContentType = throw NotImplementedError()
        actual val BirthDateMonth: ContentType = throw NotImplementedError()
        actual val BirthDateYear: ContentType = throw NotImplementedError()
        actual val SmsOtpCode: ContentType = throw NotImplementedError()
    }
}
