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

package example.passkeys.mvi

import app.cash.turbine.test
import com.example.auth_credentials.PasskeyError
import com.example.domain.model.AuthenticationOptionsJson
import com.example.domain.model.RegistrationOptionsJson
import com.example.domain.repository.PasskeysRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PasskeysViewModelTest {

    private val repository = FakePasskeysRepository()
    private lateinit var viewModel: PasskeysViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = PasskeysViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `begin registration asks the server and launches create with its options`() = runTest {
        viewModel.effects.test {
            viewModel.onIntent(PasskeysIntent.BeginRegistration("user-1"))

            assertEquals(
                PasskeysEffect.LaunchCreate(RegistrationOptionsJson(REGISTRATION_OPTIONS)),
                awaitItem()
            )
        }
        assertEquals("user-1", repository.registrationOptionsRequestedFor)
        assertTrue(viewModel.state.value.loading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `begin login asks the server and launches get with its options`() = runTest {
        viewModel.effects.test {
            viewModel.onIntent(PasskeysIntent.BeginLogin("kyriakos"))

            assertEquals(
                PasskeysEffect.LaunchGet(AuthenticationOptionsJson(AUTHENTICATION_OPTIONS)),
                awaitItem()
            )
        }
        assertEquals("kyriakos", repository.authenticationOptionsRequestedFor)
        assertTrue(viewModel.state.value.loading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `a failing options call shows an error and launches nothing`() = runTest {
        val failure = IllegalStateException("no network")
        repository.failure = failure

        viewModel.effects.test {
            viewModel.onIntent(PasskeysIntent.BeginRegistration("user-1"))

            expectNoEvents()
        }
        assertFalse(viewModel.state.value.loading)
        assertEquals(PasskeyError.OperationFailed(failure), viewModel.state.value.error)
    }

    @Test
    fun `a finished registration is sent to the server as it is`() = runTest {
        viewModel.onIntent(PasskeysIntent.BeginRegistration("user-1"))

        viewModel.onIntent(PasskeysIntent.RegistrationDone(RESPONSE))

        assertEquals(listOf(RESPONSE), repository.verifiedRegistrations)
        assertFalse(viewModel.state.value.loading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `a finished sign-in is sent to the server as it is`() = runTest {
        viewModel.onIntent(PasskeysIntent.BeginLogin("kyriakos"))

        viewModel.onIntent(PasskeysIntent.AuthenticationDone(RESPONSE))

        assertEquals(listOf(RESPONSE), repository.verifiedAuthentications)
        assertFalse(viewModel.state.value.loading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `a response the server rejects shows an error`() = runTest {
        viewModel.onIntent(PasskeysIntent.BeginRegistration("user-1"))
        val failure = IllegalStateException("challenge mismatch")
        repository.failure = failure

        viewModel.onIntent(PasskeysIntent.RegistrationDone(RESPONSE))

        assertTrue(repository.verifiedRegistrations.isEmpty())
        assertFalse(viewModel.state.value.loading)
        assertEquals(PasskeyError.OperationFailed(failure), viewModel.state.value.error)
    }

    @Test
    fun `a failure from the passkey sheet is shown as it is`() = runTest {
        viewModel.onIntent(PasskeysIntent.BeginLogin("kyriakos"))

        viewModel.onIntent(PasskeysIntent.Failed(PasskeyError.CanceledByUser))

        assertFalse(viewModel.state.value.loading)
        assertEquals(PasskeyError.CanceledByUser, viewModel.state.value.error)
    }

    @Test
    fun `a canceled passkey launch stops loading without an error`() = runTest {
        viewModel.onIntent(PasskeysIntent.BeginRegistration("user-1"))

        viewModel.onIntent(PasskeysIntent.Canceled)

        assertFalse(viewModel.state.value.loading)
        assertNull(viewModel.state.value.error)
    }

    private class FakePasskeysRepository : PasskeysRepository {
        var failure: Throwable? = null
        var registrationOptionsRequestedFor: String? = null
        var authenticationOptionsRequestedFor: String? = null
        val verifiedRegistrations = mutableListOf<String>()
        val verifiedAuthentications = mutableListOf<String>()

        override suspend fun getRegistrationOptions(userId: String): RegistrationOptionsJson {
            failure?.let { throw it }
            registrationOptionsRequestedFor = userId
            return RegistrationOptionsJson(REGISTRATION_OPTIONS)
        }

        override suspend fun verifyRegistration(responseJson: String) {
            failure?.let { throw it }
            verifiedRegistrations += responseJson
        }

        override suspend fun getAuthenticationOptions(usernameOrEmail: String): AuthenticationOptionsJson {
            failure?.let { throw it }
            authenticationOptionsRequestedFor = usernameOrEmail
            return AuthenticationOptionsJson(AUTHENTICATION_OPTIONS)
        }

        override suspend fun verifyAuthentication(responseJson: String) {
            failure?.let { throw it }
            verifiedAuthentications += responseJson
        }
    }

    private companion object {
        const val REGISTRATION_OPTIONS = """{"challenge":"cmVnaXN0ZXI","rp":{"id":"example.com"}}"""
        const val AUTHENTICATION_OPTIONS = """{"challenge":"c2lnbi1pbg","rpId":"example.com"}"""
        const val RESPONSE = """{"id":"abc","type":"public-key","response":{"clientDataJSON":"e30"}}"""
    }
}
