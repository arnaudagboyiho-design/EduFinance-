package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AuthState
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.EduFinanceTheme
import com.example.ui.theme.Slate900

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EduFinanceTheme {
                EduFinanceRoot()
            }
        }
    }
}

@Composable
fun EduFinanceRoot(
    viewModel: MainViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (authState) {
                is AuthState.Authenticated -> {
                    EduFinanceApp(
                        viewModel = viewModel,
                        onLogout = { viewModel.logout() }
                    )
                }
                else -> {
                    LoginScreen(
                        authState = authState,
                        availableUsers = viewModel.repository.availableUsers,
                        onLoginClick = { email, password ->
                            viewModel.login(email, password)
                        },
                        onForgotPasswordClick = { email, callback ->
                            viewModel.sendPasswordResetEmail(email, callback)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EduFinanceApp(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val kpis by viewModel.kpis.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val grades by viewModel.grades.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    val academicTab by viewModel.academicTab.collectAsStateWithLifecycle()
    val pedagogyTab by viewModel.pedagogyTab.collectAsStateWithLifecycle()
    val financeTab by viewModel.financeTab.collectAsStateWithLifecycle()

    val selectedReportCard by viewModel.selectedReportCard.collectAsStateWithLifecycle()
    val showNewStudentDialog by viewModel.showNewStudentDialog.collectAsStateWithLifecycle()
    val showNewPaymentDialog by viewModel.showNewPaymentDialog.collectAsStateWithLifecycle()
    val showNewExpenseDialog by viewModel.showNewExpenseDialog.collectAsStateWithLifecycle()
    val showNewGradeDialog by viewModel.showNewGradeDialog.collectAsStateWithLifecycle()
    val showQrVerificationDialog by viewModel.showQrVerificationDialog.collectAsStateWithLifecycle()
    val showRoleSwitcherDialog by viewModel.showRoleSwitcherDialog.collectAsStateWithLifecycle()

    // Android back button handling
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        viewModel.navigateTo(AppScreen.DASHBOARD)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletLandscape = maxWidth >= 720.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AppTopBar(
                    currentUser = currentUser,
                    onSwitchUserClick = { viewModel.openRoleSwitcher() },
                    onLogoutClick = onLogout
                )
            },
            bottomBar = {
                if (!isTabletLandscape) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        val navItems = listOf(
                            Triple(AppScreen.DASHBOARD, "Bord", Icons.Default.Dashboard),
                            Triple(AppScreen.ACADEMIC, "Académique", Icons.Default.School),
                            Triple(AppScreen.PEDAGOGY, "Pédagogie", Icons.Default.Assessment),
                            Triple(AppScreen.FINANCE, "Trésorerie", Icons.Default.AccountBalanceWallet),
                            Triple(AppScreen.SECURITY, "Sécurité", Icons.Default.Shield)
                        )

                        navItems.forEach { (screen, label, icon) ->
                            NavigationBarItem(
                                selected = currentScreen == screen,
                                onClick = { viewModel.navigateTo(screen) },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (currentScreen == screen) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Navigation Rail for tablet / large screens
                if (isTabletLandscape) {
                    NavigationRail(
                        containerColor = Slate900,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("side_navigation_rail")
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val railItems = listOf(
                            Triple(AppScreen.DASHBOARD, "Bord", Icons.Default.Dashboard),
                            Triple(AppScreen.ACADEMIC, "Académique", Icons.Default.School),
                            Triple(AppScreen.PEDAGOGY, "Pédagogie", Icons.Default.Assessment),
                            Triple(AppScreen.FINANCE, "Trésorerie", Icons.Default.AccountBalanceWallet),
                            Triple(AppScreen.SECURITY, "Sécurité", Icons.Default.Shield)
                        )

                        railItems.forEach { (screen, label, icon) ->
                            NavigationRailItem(
                                selected = currentScreen == screen,
                                onClick = { viewModel.navigateTo(screen) },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 10.sp) },
                                modifier = Modifier.testTag("rail_item_${screen.name.lowercase()}")
                            )
                        }
                    }
                }

                // Main screen view
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentScreen) {
                        AppScreen.DASHBOARD -> DashboardScreen(
                            kpis = kpis,
                            currentUser = currentUser,
                            recentTransactions = transactions,
                            onNavigate = { viewModel.navigateTo(it) },
                            onOpenNewStudent = { viewModel.openNewStudentDialog() },
                            onOpenNewPayment = { viewModel.openNewPaymentDialog() },
                            onOpenNewExpense = { viewModel.openNewExpenseDialog() }
                        )

                        AppScreen.ACADEMIC -> AcademicScreen(
                            currentTab = academicTab,
                            onTabSelected = { viewModel.setAcademicTab(it) },
                            students = students,
                            classes = classes,
                            teachers = teachers,
                            subjects = subjects,
                            currentUser = currentUser,
                            onOpenNewStudent = { viewModel.openNewStudentDialog() },
                            onViewReportCard = { studentId ->
                                viewModel.openReportCard(studentId)
                                viewModel.navigateTo(AppScreen.PEDAGOGY)
                                viewModel.setPedagogyTab(0)
                            }
                        )

                        AppScreen.PEDAGOGY -> PedagogyScreen(
                            currentTab = pedagogyTab,
                            onTabSelected = { viewModel.setPedagogyTab(it) },
                            students = students,
                            classes = classes,
                            subjects = subjects,
                            grades = grades,
                            attendance = attendance,
                            currentUser = currentUser,
                            selectedReportCard = selectedReportCard,
                            onGenerateReportCard = { viewModel.openReportCard(it) },
                            onOpenNewGrade = { viewModel.openNewGradeDialog() },
                            onUpdateAttendance = { id, status, reason ->
                                viewModel.updateStudentAttendance(id, status, reason)
                            },
                            onVerifyQrClick = { viewModel.openQrVerification() }
                        )

                        AppScreen.FINANCE -> FinanceScreen(
                            currentTab = financeTab,
                            onTabSelected = { viewModel.setFinanceTab(it) },
                            transactions = transactions,
                            currentUser = currentUser,
                            onOpenNewPayment = { viewModel.openNewPaymentDialog() },
                            onOpenNewExpense = { viewModel.openNewExpenseDialog() },
                            onExportReport = {
                                viewModel.showFeedback("Rapport comptable généré (Format PDF/Tableur prêt pour export).")
                            }
                        )

                        AppScreen.SECURITY -> SecurityScreen(
                            currentUser = currentUser,
                            availableUsers = viewModel.repository.availableUsers,
                            onSwitchUser = { viewModel.switchUser(it) },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }

        // Dialogs
        if (showNewStudentDialog) {
            NewStudentDialog(
                classes = classes,
                onDismiss = { viewModel.closeNewStudentDialog() },
                onConfirm = { fn, ln, g, bd, cl, pn, pp, pe, t ->
                    viewModel.addStudent(fn, ln, g, bd, cl, pn, pp, pe, t)
                }
            )
        }

        if (showNewPaymentDialog) {
            NewPaymentDialog(
                students = students,
                onDismiss = { viewModel.closeNewPaymentDialog() },
                onConfirm = { sId, cat, amt, meth, notes ->
                    viewModel.addPayment(sId, cat, amt, meth, notes)
                }
            )
        }

        if (showNewExpenseDialog) {
            NewExpenseDialog(
                onDismiss = { viewModel.closeNewExpenseDialog() },
                onConfirm = { cat, desc, amt, payee, meth ->
                    viewModel.addExpense(cat, desc, amt, payee, meth)
                }
            )
        }

        if (showNewGradeDialog) {
            NewGradeDialog(
                students = students,
                subjects = subjects,
                onDismiss = { viewModel.closeNewGradeDialog() },
                onConfirm = { sId, sub, coeff, per, t1, t2, ex ->
                    viewModel.addGrade(sId, sub, coeff, per, t1, t2, ex)
                }
            )
        }

        if (showQrVerificationDialog && selectedReportCard != null) {
            QrVerificationModal(
                report = selectedReportCard!!,
                onDismiss = { viewModel.closeQrVerification() }
            )
        }

        if (showRoleSwitcherDialog) {
            RoleSwitcherModal(
                currentUser = currentUser,
                availableUsers = viewModel.repository.availableUsers,
                onSelectUser = { viewModel.switchUser(it) },
                onDismiss = { viewModel.closeRoleSwitcher() }
            )
        }
    }
}
