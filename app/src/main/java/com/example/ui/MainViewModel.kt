package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.ServiceAuth
import com.example.data.EduFinanceRepository
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen(val title: String, val iconName: String) {
    DASHBOARD("Tableau de bord", "dashboard"),
    ACADEMIC("Académique", "school"),
    PEDAGOGY("Pédagogie", "assessment"),
    FINANCE("Trésorerie", "account_balance_wallet"),
    SECURITY("Sécurité & Rôles", "shield")
}

data class DashboardKpis(
    val activeStudents: Int,
    val totalRevenue: Double,
    val totalExpenses: Double,
    val netBalance: Double,
    val recoveryRate: Float,
    val todayAttendanceRate: Float,
    val totalClasses: Int,
    val totalTeachers: Int
)

class MainViewModel(
    val repository: EduFinanceRepository = EduFinanceRepository()
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Sub-tab selectors
    private val _academicTab = MutableStateFlow(0) // 0: Élèves, 1: Classes, 2: Enseignants, 3: Matières
    val academicTab: StateFlow<Int> = _academicTab.asStateFlow()

    private val _pedagogyTab = MutableStateFlow(0) // 0: Bulletins & QR, 1: Saisie Notes, 2: Présences
    val pedagogyTab: StateFlow<Int> = _pedagogyTab.asStateFlow()

    private val _financeTab = MutableStateFlow(0) // 0: Journal Caisse, 1: Bilan Financier
    val financeTab: StateFlow<Int> = _financeTab.asStateFlow()

    // Search and filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedClassFilter = MutableStateFlow<String?>(null)
    val selectedClassFilter: StateFlow<String?> = _selectedClassFilter.asStateFlow()

    // Selected Report Card for viewing / printing
    private val _selectedReportCard = MutableStateFlow<ReportCard?>(null)
    val selectedReportCard: StateFlow<ReportCard?> = _selectedReportCard.asStateFlow()

    // Dialog states
    private val _showNewStudentDialog = MutableStateFlow(false)
    val showNewStudentDialog: StateFlow<Boolean> = _showNewStudentDialog.asStateFlow()

    private val _showNewPaymentDialog = MutableStateFlow(false)
    val showNewPaymentDialog: StateFlow<Boolean> = _showNewPaymentDialog.asStateFlow()

    private val _showNewExpenseDialog = MutableStateFlow(false)
    val showNewExpenseDialog: StateFlow<Boolean> = _showNewExpenseDialog.asStateFlow()

    private val _showNewGradeDialog = MutableStateFlow(false)
    val showNewGradeDialog: StateFlow<Boolean> = _showNewGradeDialog.asStateFlow()

    private val _showQrVerificationDialog = MutableStateFlow(false)
    val showQrVerificationDialog: StateFlow<Boolean> = _showQrVerificationDialog.asStateFlow()

    private val _showRoleSwitcherDialog = MutableStateFlow(false)
    val showRoleSwitcherDialog: StateFlow<Boolean> = _showRoleSwitcherDialog.asStateFlow()

    // Status snackbar / feedback
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val currentUser = repository.currentUser
    val authState: StateFlow<AuthState> = repository.authState
    val students = repository.students
    val classes = repository.classes
    val teachers = repository.teachers
    val subjects = repository.subjects
    val attendance = repository.attendance
    val grades = repository.grades
    val transactions = repository.transactions

    fun login(email: String, password: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repository.login(email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                showFeedback("Connexion réussie : Bienvenue ${user?.name}")
                onComplete?.invoke(true)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Erreur d'authentification."
                showFeedback(err)
                onComplete?.invoke(false)
            }
        }
    }

    fun logout() {
        repository.logout()
        showFeedback("Session verrouillée / Déconnexion effectuée.")
    }

    fun validateCredentials(email: String, password: String): AuthValidationResult {
        val trimmedEmail = email.trim()
        val emailError = when {
            trimmedEmail.isEmpty() -> "L'identifiant ou l'adresse e-mail est obligatoire."
            !trimmedEmail.contains("@") || !trimmedEmail.contains(".") -> "Format d'adresse e-mail invalide (ex: admin@edufinance.pro)."
            else -> null
        }

        val passwordError = when {
            password.isEmpty() -> "Le mot de passe est obligatoire."
            password.length < 6 -> "Le mot de passe doit comporter au moins 6 caractères."
            else -> null
        }

        return AuthValidationResult(emailError = emailError, passwordError = passwordError)
    }

    val serviceAuth: ServiceAuth by lazy { ServiceAuth.getInstance() }

    fun sendPasswordResetEmail(email: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = serviceAuth.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                showFeedback("E-mail de réinitialisation de mot de passe envoyé à $email")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Échec de l'envoi de l'e-mail de réinitialisation."
                showFeedback(err)
            }
            onResult(result)
        }
    }

    // Computed Dashboard KPIs
    val kpis: StateFlow<DashboardKpis> = combine(
        students, transactions, attendance, classes, teachers
    ) { stds, txs, atts, clss, tchs ->
        val active = stds.count { it.status != StudentStatus.ARCHIVE }
        val revenue = txs.filter { it.type == TransactionType.ENCAISSEMENT }.sumOf { it.amount }
        val expenses = txs.filter { it.type == TransactionType.DEPENSE }.sumOf { it.amount }
        val net = revenue - expenses

        val totalExpected = stds.sumOf { it.tuitionTotal }
        val totalPaid = stds.sumOf { it.tuitionPaid }
        val recovery = if (totalExpected > 0) (totalPaid / totalExpected * 100).toFloat() else 0f

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayAtt = atts.filter { it.date == today }
        val presentCount = todayAtt.count { it.status == AttendanceStatus.PRESENT }
        val attRate = if (todayAtt.isNotEmpty()) (presentCount.toFloat() / todayAtt.size * 100) else 94.5f

        DashboardKpis(
            activeStudents = active,
            totalRevenue = revenue,
            totalExpenses = expenses,
            netBalance = net,
            recoveryRate = recovery,
            todayAttendanceRate = attRate,
            totalClasses = clss.size,
            totalTeachers = tchs.size
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardKpis(0, 0.0, 0.0, 0.0, 0f, 0f, 0, 0)
    )

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setAcademicTab(tab: Int) { _academicTab.value = tab }
    fun setPedagogyTab(tab: Int) { _pedagogyTab.value = tab }
    fun setFinanceTab(tab: Int) { _financeTab.value = tab }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedClassFilter(className: String?) { _selectedClassFilter.value = className }

    fun openNewStudentDialog() { _showNewStudentDialog.value = true }
    fun closeNewStudentDialog() { _showNewStudentDialog.value = false }

    fun openNewPaymentDialog() { _showNewPaymentDialog.value = true }
    fun closeNewPaymentDialog() { _showNewPaymentDialog.value = false }

    fun openNewExpenseDialog() { _showNewExpenseDialog.value = true }
    fun closeNewExpenseDialog() { _showNewExpenseDialog.value = false }

    fun openNewGradeDialog() { _showNewGradeDialog.value = true }
    fun closeNewGradeDialog() { _showNewGradeDialog.value = false }

    fun openRoleSwitcher() { _showRoleSwitcherDialog.value = true }
    fun closeRoleSwitcher() { _showRoleSwitcherDialog.value = false }

    fun openReportCard(studentId: String, period: String = "Trimestre 1") {
        val report = repository.generateReportCard(studentId, period)
        _selectedReportCard.value = report
    }

    fun closeReportCard() {
        _selectedReportCard.value = null
    }

    fun openQrVerification() {
        _showQrVerificationDialog.value = true
    }

    fun closeQrVerification() {
        _showQrVerificationDialog.value = false
    }

    fun switchUser(userId: String) {
        repository.switchUser(userId)
        _showRoleSwitcherDialog.value = false
        showFeedback("Connecté avec le profil: ${repository.currentUser.value.name} (${repository.currentUser.value.role.title})")
    }

    fun addStudent(
        firstName: String,
        lastName: String,
        gender: String,
        birthDate: String,
        className: String,
        parentName: String,
        parentPhone: String,
        parentEmail: String,
        tuitionTotal: Double
    ) {
        val s = repository.addStudent(
            firstName, lastName, gender, birthDate, className,
            parentName, parentPhone, parentEmail, tuitionTotal
        )
        _showNewStudentDialog.value = false
        showFeedback("Élève inscrit avec succès : ${s.fullName} (${s.matricule})")
    }

    fun addPayment(
        studentId: String,
        category: String,
        amount: Double,
        method: PaymentMethod,
        notes: String
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val tx = repository.addTransaction(
            type = TransactionType.ENCAISSEMENT,
            category = category,
            amount = amount,
            date = today,
            studentId = studentId,
            payeeOrPayer = "Élève / Parent",
            paymentMethod = method,
            notes = notes
        )
        _showNewPaymentDialog.value = false
        showFeedback("Encaissement enregistré : ${EduFinanceRepository.formatCurrency(amount)} (${tx.referenceNumber})")
    }

    fun addExpense(
        category: String,
        description: String,
        amount: Double,
        payee: String,
        method: PaymentMethod
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val tx = repository.addTransaction(
            type = TransactionType.DEPENSE,
            category = category,
            amount = amount,
            date = today,
            studentId = null,
            payeeOrPayer = payee,
            paymentMethod = method,
            notes = description
        )
        _showNewExpenseDialog.value = false
        showFeedback("Dépense enregistrée : ${EduFinanceRepository.formatCurrency(amount)} (${tx.referenceNumber})")
    }

    fun addGrade(
        studentId: String,
        subjectName: String,
        coefficient: Int,
        period: String,
        test1: Float,
        test2: Float,
        exam: Float
    ) {
        val student = students.value.find { it.id == studentId } ?: return
        repository.saveGrade(
            studentId = studentId,
            studentName = student.fullName,
            className = student.className,
            subjectName = subjectName,
            coefficient = coefficient,
            period = period,
            test1 = test1,
            test2 = test2,
            exam = exam
        )
        _showNewGradeDialog.value = false
        showFeedback("Note enregistrée pour ${student.fullName} en $subjectName")
    }

    fun updateStudentAttendance(studentId: String, status: AttendanceStatus, reason: String = "") {
        val student = students.value.find { it.id == studentId } ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val record = AttendanceRecord(
            id = "ATT-${System.currentTimeMillis()}",
            studentId = studentId,
            studentName = student.fullName,
            className = student.className,
            date = today,
            status = status,
            reason = reason
        )
        repository.recordDailyAttendance(listOf(record))
        showFeedback("Présence mise à jour pour ${student.fullName}: ${status.label}")
    }

    fun showFeedback(msg: String) {
        _feedbackMessage.value = msg
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
