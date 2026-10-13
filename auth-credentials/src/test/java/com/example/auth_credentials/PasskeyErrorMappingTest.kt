/*
 * Copyright 2026 Kyriakos Georgiopoulos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.auth_credentials

import androidx.credentials.exceptions.GetCredentialCancellationException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PasskeyErrorMappingTest {

    @Test
    fun `a coroutine cancellation is rethrown instead of mapped`() {
        val cancellation = CancellationException("the scope was canceled")
        val result = Result.failure<String>(cancellation)

        val thrown = assertThrows(CancellationException::class.java) {
            result.mapToDomainError(::toPasskeyException)
        }

        assertSame(cancellation, thrown)
    }

    @Test
    fun `the user closing the sheet still becomes CanceledByUser`() {
        val result = Result.failure<String>(GetCredentialCancellationException("closed by the user"))
            .mapToDomainError(::toPasskeyException)

        assertEquals(PasskeyError.CanceledByUser, result.passkeyError())
    }

    @Test
    fun `any other failure becomes OperationFailed and keeps its cause`() {
        val cause = IllegalStateException("boom")

        val result = Result.failure<String>(cause).mapToDomainError(::toPasskeyException)

        assertEquals(PasskeyError.OperationFailed(cause), result.passkeyError())
    }

    @Test
    fun `a success passes through unchanged`() {
        val result = Result.success("response").mapToDomainError(::toPasskeyException)

        assertEquals("response", result.getOrNull())
    }

    private fun Result<*>.passkeyError(): PasskeyError? =
        (exceptionOrNull() as? PasskeyException)?.error
}
