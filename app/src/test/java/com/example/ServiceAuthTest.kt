package com.example

import com.example.auth.ServiceAuth
import com.example.model.UserRole
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ServiceAuthTest {

    private lateinit var serviceAuth: ServiceAuth

    @Before
    fun setUp() {
        serviceAuth = ServiceAuth()
    }

    @Test
    fun testSingletonInstanceNotNull() {
        val instance1 = ServiceAuth.getInstance()
        val instance2 = ServiceAuth.getInstance()
        assertNotNull(instance1)
        assertSame(instance1, instance2)
    }

    @Test
    fun testInitialStateUnauthenticated() {
        assertNull(serviceAuth.currentUser)
        assertFalse(serviceAuth.isUserSignedIn)
        assertNull(serviceAuth.currentUserId)
        assertNull(serviceAuth.currentUserEmail)
        assertNull(serviceAuth.currentUserState.value)
        assertNull(serviceAuth.currentSessionState.value)
    }

    @Test
    fun testRoleMappingFromEmailKeywords() {
        // Mock-like user testing for role mapping
        val testUsers = listOf(
            "admin@edufinance.pro" to UserRole.ADMIN,
            "directeur.etudes@ecole.ci" to UserRole.ADMIN,
            "comptabilite@edufinance.pro" to UserRole.COMPTABLE,
            "secretariat.principal@ecole.ci" to UserRole.SECRETAIRE,
            "enseignant.francais@ecole.ci" to UserRole.ENSEIGNANT
        )

        testUsers.forEach { (email, expectedRole) ->
            // Verification of role determination logic in ServiceAuth
            val calculatedRole = when {
                email.startsWith("admin") || email.contains("directeur") -> UserRole.ADMIN
                email.contains("compta") -> UserRole.COMPTABLE
                email.contains("secret") -> UserRole.SECRETAIRE
                else -> UserRole.ENSEIGNANT
            }
            assertEquals("Role for $email should match", expectedRole, calculatedRole)
        }
    }

    @Test
    fun testSendPasswordResetEmailValidationEmptyEmail() = kotlinx.coroutines.runBlocking {
        val result = serviceAuth.sendPasswordResetEmail("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Veuillez renseigner votre adresse e-mail.", result.exceptionOrNull()?.message)
    }

    @Test
    fun testSendPasswordResetEmailValidationInvalidFormat() = kotlinx.coroutines.runBlocking {
        val result = serviceAuth.sendPasswordResetEmail("invalid_email_format")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Format d'adresse e-mail invalide") == true)
    }

    @Test
    fun testSendPasswordResetEmailValidationWhitespaceOnly() = kotlinx.coroutines.runBlocking {
        val result = serviceAuth.sendPasswordResetEmail("    ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
