package com.example

import com.example.data.EduFinanceRepository
import com.example.model.AuthState
import com.example.model.PaymentMethod
import com.example.model.StudentStatus
import com.example.model.TransactionType
import com.example.model.UserRole
import com.example.ui.MainViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExampleUnitTest {

    private lateinit var repository: EduFinanceRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        repository = EduFinanceRepository()
        viewModel = MainViewModel(repository)
    }

    @Test
    fun testInitialSeedDataLoaded() {
        assertTrue(repository.students.value.isNotEmpty())
        assertTrue(repository.classes.value.isNotEmpty())
        assertTrue(repository.teachers.value.isNotEmpty())
        assertTrue(repository.subjects.value.isNotEmpty())
        assertTrue(repository.transactions.value.isNotEmpty())
    }

    @Test
    fun testStudentRegistrationAndMatricule() {
        val initialCount = repository.students.value.size
        val newStudent = repository.addStudent(
            firstName = "Kouassi",
            lastName = "YAO",
            gender = "M",
            birthDate = "2008-01-15",
            className = "Terminale S1",
            parentName = "M. Yao",
            parentPhone = "+225 07 11 22 33",
            parentEmail = "yao@example.com",
            tuitionTotal = 450000.0
        )

        assertEquals(initialCount + 1, repository.students.value.size)
        assertTrue(newStudent.matricule.startsWith("EDF-2026-"))
        assertEquals("YAO Kouassi", newStudent.fullName)
        assertEquals(StudentStatus.ACTIF, newStudent.status)
    }

    @Test
    fun testCashbookEncaissementUpdatesStudentBalance() {
        val student = repository.students.value.first { it.tuitionBalance > 0 }
        val initialPaid = student.tuitionPaid
        val payAmount = 150000.0

        repository.addTransaction(
            type = TransactionType.ENCAISSEMENT,
            category = "Scolarité",
            amount = payAmount,
            date = "2026-11-06",
            studentId = student.id,
            payeeOrPayer = student.fullName,
            paymentMethod = PaymentMethod.ESPECES,
            notes = "Test paiement scolarité"
        )

        val updatedStudent = repository.students.value.first { it.id == student.id }
        assertEquals(initialPaid + payAmount, updatedStudent.tuitionPaid, 0.01)
    }

    @Test
    fun testReportCardGenerationAndQrPayload() {
        val student = repository.students.value.first { it.matricule == "EDF-2026-001" }
        val report = repository.generateReportCard(student.id, "Trimestre 1")

        assertNotNull(report)
        assertTrue(report!!.overallAverage > 0f)
        assertTrue(report.overallAverage <= 20f)
        assertTrue(report.totalPoints > 0f)
        assertTrue(report.totalCoefficients > 0)
        assertTrue(report.verificationHash.startsWith("CERT-"))
        assertTrue(report.qrPayload.contains(student.matricule))
        assertTrue(report.qrPayload.contains("edufinance.pro/verify"))
    }

    @Test
    fun testRolePermissionsMatrix() {
        assertTrue(UserRole.ADMIN.canManageFinances)
        assertTrue(UserRole.ADMIN.canEditStudents)
        assertTrue(UserRole.ADMIN.canInputGrades)

        assertTrue(UserRole.COMPTABLE.canManageFinances)
        assertFalse(UserRole.COMPTABLE.canEditStudents)
        assertFalse(UserRole.COMPTABLE.canInputGrades)

        assertFalse(UserRole.SECRETAIRE.canManageFinances)
        assertTrue(UserRole.SECRETAIRE.canEditStudents)
        assertFalse(UserRole.SECRETAIRE.canInputGrades)

        assertFalse(UserRole.ENSEIGNANT.canManageFinances)
        assertFalse(UserRole.ENSEIGNANT.canEditStudents)
        assertTrue(UserRole.ENSEIGNANT.canInputGrades)
    }

    @Test
    fun testCredentialsValidationLogic() {
        // Empty email
        val res1 = viewModel.validateCredentials("", "password123")
        assertFalse(res1.isValid)
        assertNotNull(res1.emailError)
        assertNull(res1.passwordError)

        // Invalid email format (missing @)
        val res2 = viewModel.validateCredentials("adminedufinance.pro", "password123")
        assertFalse(res2.isValid)
        assertNotNull(res2.emailError)

        // Short password (< 6 chars)
        val res3 = viewModel.validateCredentials("admin@edufinance.pro", "123")
        assertFalse(res3.isValid)
        assertNull(res3.emailError)
        assertNotNull(res3.passwordError)

        // Valid credentials
        val res4 = viewModel.validateCredentials("admin@edufinance.pro", "Admin@2026")
        assertTrue(res4.isValid)
        assertNull(res4.emailError)
        assertNull(res4.passwordError)
    }

    @Test
    fun testAuthenticationSuccessAndLogout() = runBlocking {
        // Initial state is unauthenticated
        assertEquals(AuthState.Unauthenticated, repository.authState.value)

        // Valid login
        val result = repository.login("admin@edufinance.pro", "Admin@2026")
        assertTrue(result.isSuccess)
        val session = result.getOrNull()
        assertNotNull(session)
        assertEquals("admin@edufinance.pro", session?.email)
        assertEquals(UserRole.ADMIN, session?.role)
        assertTrue(repository.authState.value is AuthState.Authenticated)

        // Logout
        repository.logout()
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }

    @Test
    fun testAuthenticationFailureWrongPassword() = runBlocking {
        val result = repository.login("admin@edufinance.pro", "wrongpw")
        assertTrue(result.isFailure)
        assertTrue(repository.authState.value is AuthState.AuthError)
    }

    @Test
    fun testPrimaryCycleClassesAndHierarchicalOrder() {
        val classes = repository.classes.value
        val primaryClasses = classes.filter { it.cycle == com.example.model.SchoolCycle.PRIMAIRE }
        assertEquals(6, primaryClasses.size)

        val primaryNames = primaryClasses.map { it.name }
        assertTrue(primaryNames.any { it.startsWith("CI") })
        assertTrue(primaryNames.any { it.startsWith("CP") })
        assertTrue(primaryNames.any { it.startsWith("CE1") })
        assertTrue(primaryNames.any { it.startsWith("CE2") })
        assertTrue(primaryNames.any { it.startsWith("CM1") })
        assertTrue(primaryNames.any { it.startsWith("CM2") })
    }

    @Test
    fun testPrimaryCycleSubjectsAndAdaptedCoefficients() {
        val subjects = repository.subjects.value
        val primarySubjects = subjects.filter { it.cycle == com.example.model.SchoolCycle.PRIMAIRE }
        assertTrue(primarySubjects.isNotEmpty())

        val lecture = primarySubjects.firstOrNull { it.name.contains("Lecture") }
        assertNotNull(lecture)
        assertEquals(4, lecture?.coefficient)

        val maths = primarySubjects.firstOrNull { it.name.contains("Mathématiques") }
        assertNotNull(maths)
        assertEquals(4, maths?.coefficient)

        val ecm = primarySubjects.firstOrNull { it.name.contains("ECM") }
        assertNotNull(ecm)
        assertEquals(2, ecm?.coefficient)
    }

    @Test
    fun testPrimaryReportCardComputesClassAverageAndSubjectRanks() {
        // Yann KOUASSI in CM2 B
        val student = repository.students.value.first { it.matricule == "EDF-2026-P01" }
        val report = repository.generateReportCard(student.id, "Trimestre 1")

        assertNotNull(report)
        assertEquals(com.example.model.SchoolCycle.PRIMAIRE, report!!.cycle)
        assertTrue(report.overallAverage > 0f)
        assertTrue(report.classAverage > 0f)
        assertTrue(report.rankInClass >= 1)

        // Verify each discipline has classAverage, subjectRank and adapted appreciation
        report.subjects.forEach { item ->
            assertTrue("Class average should be > 0", item.classAverage > 0f)
            assertTrue("Subject rank should be set", item.subjectRank.isNotEmpty())
            assertTrue("Appreciation should not be blank", item.teacherAppreciation.isNotBlank())
        }
    }
}
