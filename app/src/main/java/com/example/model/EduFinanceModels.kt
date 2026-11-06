package com.example.model

enum class UserRole(
    val title: String,
    val description: String,
    val canManageFinances: Boolean,
    val canEditStudents: Boolean,
    val canInputGrades: Boolean,
    val canTakeAttendance: Boolean,
    val canViewFullReports: Boolean
) {
    ADMIN(
        title = "Administrateur",
        description = "Accès système complet & RLS",
        canManageFinances = true,
        canEditStudents = true,
        canInputGrades = true,
        canTakeAttendance = true,
        canViewFullReports = true
    ),
    DIRECTEUR(
        title = "Directeur",
        description = "Supervision pédagogique et financière",
        canManageFinances = true,
        canEditStudents = true,
        canInputGrades = true,
        canTakeAttendance = true,
        canViewFullReports = true
    ),
    COMPTABLE(
        title = "Comptable",
        description = "Gestion trésorerie, caisse et encaissements",
        canManageFinances = true,
        canEditStudents = false,
        canInputGrades = false,
        canTakeAttendance = false,
        canViewFullReports = true
    ),
    SECRETAIRE(
        title = "Secrétaire",
        description = "Inscriptions, registres et appels",
        canManageFinances = false,
        canEditStudents = true,
        canInputGrades = false,
        canTakeAttendance = true,
        canViewFullReports = false
    ),
    ENSEIGNANT(
        title = "Enseignant",
        description = "Saisie des notes et présences de cours",
        canManageFinances = false,
        canEditStudents = false,
        canInputGrades = true,
        canTakeAttendance = true,
        canViewFullReports = false
    )
}

data class UserSession(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val avatarInitials: String
)

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val session: UserSession) : AuthState()
    data class AuthError(val message: String) : AuthState()
}

data class AuthValidationResult(
    val emailError: String? = null,
    val passwordError: String? = null
) {
    val isValid: Boolean get() = emailError == null && passwordError == null
}

enum class StudentStatus(val label: String) {
    ACTIF("Actif"),
    EN_REGLE("En règle"),
    EN_RETARD("En retard"),
    ARCHIVE("Archivé")
}

data class Student(
    val id: String,
    val matricule: String,
    val firstName: String,
    val lastName: String,
    val gender: String, // "M" or "F"
    val birthDate: String,
    val className: String,
    val parentName: String,
    val parentPhone: String,
    val parentEmail: String,
    val tuitionTotal: Double,
    val tuitionPaid: Double,
    val status: StudentStatus,
    val registrationDate: String
) {
    val fullName: String get() = "$lastName $firstName"
    val tuitionBalance: Double get() = (tuitionTotal - tuitionPaid).coerceAtLeast(0.0)
    val isFullyPaid: Boolean get() = tuitionPaid >= tuitionTotal
}

enum class SchoolCycle(val label: String, val shortName: String) {
    PRIMAIRE("Cycle Primaire (CI - CM2)", "Primaire"),
    COLLEGE("Collège (6ème - 3ème)", "Collège"),
    LYCEE("Lycée (2nde - Tle)", "Lycée")
}

data class SchoolClass(
    val id: String,
    val name: String,
    val level: String, // "Primaire", "Collège", "Lycée"
    val cycle: SchoolCycle = SchoolCycle.PRIMAIRE,
    val capacity: Int,
    val studentCount: Int,
    val mainTeacherName: String,
    val roomNumber: String
)

data class Teacher(
    val id: String,
    val name: String,
    val subjects: List<String>,
    val phone: String,
    val email: String,
    val monthlySalary: Double,
    val status: String // "Actif", "En congé"
)

data class Subject(
    val id: String,
    val name: String,
    val code: String,
    val coefficient: Int,
    val className: String,
    val teacherName: String,
    val cycle: SchoolCycle = SchoolCycle.PRIMAIRE
)

enum class AttendanceStatus(val label: String, val code: String) {
    PRESENT("Présent", "P"),
    ABSENT_JUSTIFIE("Absent justifié", "AJ"),
    ABSENT_NON_JUSTIFIE("Absent non justifié", "ANJ"),
    RETARD("En retard", "R")
}

data class AttendanceRecord(
    val id: String,
    val studentId: String,
    val studentName: String,
    val className: String,
    val date: String,
    val status: AttendanceStatus,
    val reason: String = ""
)

data class GradeRecord(
    val id: String,
    val studentId: String,
    val studentName: String,
    val className: String,
    val subjectName: String,
    val coefficient: Int,
    val period: String, // "Trimestre 1", "Trimestre 2", "Trimestre 3"
    val test1: Float,
    val test2: Float,
    val exam: Float
) {
    val average: Float
        get() = ((test1 + test2 + (2f * exam)) / 4f)
}

enum class TransactionType(val label: String) {
    ENCAISSEMENT("Encaissement (Recette)"),
    DEPENSE("Décaissement (Dépense)")
}

enum class PaymentMethod(val label: String) {
    ESPECES("Espèces"),
    VIREMENT("Virement Bancaire"),
    MOBILE_MONEY("Mobile Money"),
    CHEQUE("Chèque")
}

data class Transaction(
    val id: String,
    val type: TransactionType,
    val category: String,
    val amount: Double,
    val date: String,
    val referenceNumber: String,
    val studentId: String? = null,
    val studentName: String? = null,
    val payeeOrPayer: String,
    val paymentMethod: PaymentMethod,
    val notes: String = "",
    val recordedBy: String
)

data class SubjectReportItem(
    val subjectName: String,
    val coefficient: Int,
    val test1: Float,
    val test2: Float,
    val exam: Float,
    val average: Float,
    val weightedTotal: Float,
    val classAverage: Float = 12.5f,
    val subjectRank: String = "1er",
    val teacherAppreciation: String
)

data class ReportCard(
    val student: Student,
    val period: String,
    val subjects: List<SubjectReportItem>,
    val overallAverage: Float,
    val classAverage: Float = 12.8f,
    val rankInClass: Int,
    val totalClassSize: Int,
    val totalPoints: Float,
    val totalCoefficients: Int,
    val appreciation: String,
    val decision: String,
    val verificationHash: String,
    val qrPayload: String,
    val attendanceSummary: String,
    val cycle: SchoolCycle = SchoolCycle.PRIMAIRE
)
