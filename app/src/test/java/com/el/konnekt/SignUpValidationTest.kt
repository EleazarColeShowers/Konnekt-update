package com.el.konnekt

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class SignUpValidationTest {

    @Test
    fun `empty email returns error`(){
        val email= ""
        val username = "testuser"
        val password = "password123"

        val result= validateSignUpFields(email, password, username)

        assertTrue(result is ValidationResult.Error)
        assertEquals("Email cannot be empty", (result as ValidationResult.Error).message)

    }

    @Test
    fun `invalid email format returns error`() {
        // Arrange
        val email = "notanemail"
        val emailWithCaps= "Notanemail@gmail.com"
        val password = "password123"
        val username = "johndoe"

        // Act
        val result = validateSignUpFields(email, password, username)
        val result2= validateSignUpFields(emailWithCaps, password, username)

        // Assert
        assertTrue(result is ValidationResult.Error)
        assertEquals("Invalid email format", (result as ValidationResult.Error).message)

        assertTrue(result2 is ValidationResult.Error)
        assertEquals("Invalid email format", ( result as ValidationResult.Error).message)
    }

}

fun validateSignUpFields(email: String, password: String, username: String): ValidationResult {
    return when {
        email.isEmpty() -> ValidationResult.Error("Email cannot be empty")
        password.isEmpty() -> ValidationResult.Error("Password cannot be empty")
        username.isEmpty() -> ValidationResult.Error("Username cannot be empty")
        !email.contains("@") -> ValidationResult.Error("Invalid email format")
        password.length < 6 -> ValidationResult.Error("Password too short")
        else -> ValidationResult.Success
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}