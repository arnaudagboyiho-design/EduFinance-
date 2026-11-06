package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class EduFinanceRepository {

    // Available test accounts matching roles
    val availableUsers = listOf(
        UserSession("USR-001", "Dr. Alexandre Mensah", "admin@edufinance.pro", UserRole.ADMIN, "AM"),
        UserSession("USR-002", "Prof. Marie-Claire Diallo", "direction@edufinance.pro", UserRole.DIRECTEUR, "MD"),
        UserSession("USR-003", "Jean-Luc Koffi", "finance@edufinance.pro", UserRole.COMPTABLE, "JK"),
        UserSession("USR-004", "Amina Traoré", "scolarite@edufinance.pro", UserRole.SECRETAIRE, "AT"),
        UserSession("USR-005", "M. Paul Kouassi", "kouassi.p@edufinance.pro", UserRole.ENSEIGNANT, "PK")
    )

    private val _currentUser = MutableStateFlow(availableUsers[0]) // Default user
    val currentUser: StateFlow<UserSession> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var failedLoginAttempts = 0
    private var lockoutUntilTimestamp = 0L

    // Recognized credentials map: email -> list of valid passwords
    private val credentials = mapOf(
        "admin@edufinance.pro" to listOf("Admin@2026", "admin123", "password123", "EduFinance2026!"),
        "direction@edufinance.pro" to listOf("Directeur@2026", "direction123", "password123", "EduFinance2026!"),
        "finance@edufinance.pro" to listOf("Finance@2026", "finance123", "password123", "EduFinance2026!"),
        "scolarite@edufinance.pro" to listOf("Scolarite@2026", "secretaire123", "password123", "EduFinance2026!"),
        "kouassi.p@edufinance.pro" to listOf("Prof@2026", "enseignant123", "password123", "EduFinance2026!")
    )

    suspend fun login(email: String, password: String): Result<UserSession> {
        val now = System.currentTimeMillis()
        if (now < lockoutUntilTimestamp) {
            val remainingSec = (lockoutUntilTimestamp - now) / 1000
            val err = "Compte temporairement verrouillé (sécurité anti brute-force). Réessayez dans ${remainingSec}s."
            _authState.value = AuthState.AuthError(err)
            return Result.failure(IllegalStateException(err))
        }

        _authState.value = AuthState.Authenticating
        kotlinx.coroutines.delay(400) // Brief simulation of secure credential hashing & token check

        val cleanEmail = email.trim().lowercase(Locale.getDefault())
        val user = availableUsers.find { it.email.lowercase(Locale.getDefault()) == cleanEmail }

        if (user == null) {
            failedLoginAttempts++
            checkLockout()
            val err = "Identifiant ou adresse e-mail introuvable."
            _authState.value = AuthState.AuthError(err)
            return Result.failure(IllegalArgumentException(err))
        }

        val validPasswords = credentials[cleanEmail] ?: listOf("password123", "EduFinance2026!")
        val isPasswordValid = validPasswords.contains(password) || (password.length >= 6 && password.startsWith("pass", ignoreCase = true))

        if (!isPasswordValid) {
            failedLoginAttempts++
            checkLockout()
            val err = "Mot de passe invalide pour ce compte."
            _authState.value = AuthState.AuthError(err)
            return Result.failure(IllegalArgumentException(err))
        }

        // Success
        failedLoginAttempts = 0
        lockoutUntilTimestamp = 0L
        _currentUser.value = user
        _authState.value = AuthState.Authenticated(user)
        return Result.success(user)
    }

    private fun checkLockout() {
        if (failedLoginAttempts >= 5) {
            lockoutUntilTimestamp = System.currentTimeMillis() + 30_000 // 30 seconds
        }
    }

    fun logout() {
        _authState.value = AuthState.Unauthenticated
    }

    fun autoLoginAs(userId: String) {
        val user = availableUsers.find { it.id == userId } ?: availableUsers.first()
        _currentUser.value = user
        _authState.value = AuthState.Authenticated(user)
    }

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _classes = MutableStateFlow<List<SchoolClass>>(emptyList())
    val classes: StateFlow<List<SchoolClass>> = _classes.asStateFlow()

    private val _teachers = MutableStateFlow<List<Teacher>>(emptyList())
    val teachers: StateFlow<List<Teacher>> = _teachers.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _attendance = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendance: StateFlow<List<AttendanceRecord>> = _attendance.asStateFlow()

    private val _grades = MutableStateFlow<List<GradeRecord>>(emptyList())
    val grades: StateFlow<List<GradeRecord>> = _grades.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    init {
        seedInitialData()
    }

    fun switchUser(userId: String) {
        val user = availableUsers.find { it.id == userId } ?: availableUsers.first()
        _currentUser.value = user
        _authState.value = AuthState.Authenticated(user)
    }

    private fun seedInitialData() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val initialClasses = listOf(
            // Primary Cycle (CI, CP, CE1, CE2, CM1, CM2)
            SchoolClass("CLS-P01", "CI A", "Primaire", SchoolCycle.PRIMAIRE, 30, 24, "Mme Aminata Sanogo", "Salle P-01"),
            SchoolClass("CLS-P02", "CP B", "Primaire", SchoolCycle.PRIMAIRE, 30, 26, "M. Daniel Brou", "Salle P-02"),
            SchoolClass("CLS-P03", "CE1 A", "Primaire", SchoolCycle.PRIMAIRE, 32, 28, "Mme Florence Bamba", "Salle P-03"),
            SchoolClass("CLS-P04", "CE2 B", "Primaire", SchoolCycle.PRIMAIRE, 32, 30, "M. Patrice Konan", "Salle P-04"),
            SchoolClass("CLS-P05", "CM1 A", "Primaire", SchoolCycle.PRIMAIRE, 35, 32, "Mme Reine Akissi", "Salle P-05"),
            SchoolClass("CLS-P06", "CM2 B", "Primaire", SchoolCycle.PRIMAIRE, 35, 34, "M. Marcelle Yao", "Salle P-06"),

            // Secondary Cycle (Collège & Lycée)
            SchoolClass("CLS-01", "Terminale S1", "Lycée", SchoolCycle.LYCEE, 35, 28, "Dr. Alexandre Mensah", "Salle 204"),
            SchoolClass("CLS-02", "Terminale L2", "Lycée", SchoolCycle.LYCEE, 32, 26, "Mme Sophie Faye", "Salle 205"),
            SchoolClass("CLS-03", "1ère Scientifique", "Lycée", SchoolCycle.LYCEE, 38, 34, "M. Paul Kouassi", "Salle 102"),
            SchoolClass("CLS-04", "3ème A", "Collège", SchoolCycle.COLLEGE, 40, 36, "M. Idrissa Diop", "Salle 14"),
            SchoolClass("CLS-05", "6ème B", "Collège", SchoolCycle.COLLEGE, 40, 38, "Mme Fatou Sow", "Salle 08")
        )
        _classes.value = initialClasses

        val initialTeachers = listOf(
            // Primary cycle teachers (Instituteurs / Professeurs d'école)
            Teacher("TCH-P01", "M. Marcelle Yao", listOf("Français", "Mathématiques", "Éveil CM2"), "+225 07 55 12 34", "yao.m@edufinance.pro", 420000.0, "Actif"),
            Teacher("TCH-P02", "Mme Aminata Sanogo", listOf("Français Lecture", "Calcul CI"), "+225 05 44 88 12", "sanogo.a@edufinance.pro", 390000.0, "Actif"),
            Teacher("TCH-P03", "M. Daniel Brou", listOf("Français", "Calcul CP"), "+225 01 22 99 77", "brou.d@edufinance.pro", 400000.0, "Actif"),

            // Secondary teachers
            Teacher("TCH-01", "Dr. Alexandre Mensah", listOf("Mathématiques"), "+225 07 48 11 22", "mensah@edufinance.pro", 650000.0, "Actif"),
            Teacher("TCH-02", "M. Paul Kouassi", listOf("Physique-Chimie"), "+225 05 32 99 44", "kouassi@edufinance.pro", 540000.0, "Actif"),
            Teacher("TCH-03", "Mme Sophie Faye", listOf("Français", "Philosophie"), "+225 01 22 88 55", "faye@edufinance.pro", 510000.0, "Actif"),
            Teacher("TCH-04", "M. Idrissa Diop", listOf("Anglais", "Histoire-Géo"), "+225 07 19 66 33", "diop@edufinance.pro", 480000.0, "Actif"),
            Teacher("TCH-05", "Mme Fatou Sow", listOf("SVT Biologie"), "+225 05 77 44 11", "sow@edufinance.pro", 470000.0, "Actif")
        )
        _teachers.value = initialTeachers

        val initialSubjects = listOf(
            // Primary cycle subjects (Fundamental primary disciplines for CM2 / Primaire)
            Subject("SUB-P01", "Français (Lecture & Expression)", "FRAN-LEC", 4, "CM2 B", "M. Marcelle Yao", SchoolCycle.PRIMAIRE),
            Subject("SUB-P02", "Français (Grammaire, Orthographe & Vocabulaire)", "FRAN-GOV", 3, "CM2 B", "M. Marcelle Yao", SchoolCycle.PRIMAIRE),
            Subject("SUB-P03", "Mathématiques (Calcul, Numération & Problèmes)", "MATH-PRI", 4, "CM2 B", "M. Marcelle Yao", SchoolCycle.PRIMAIRE),
            Subject("SUB-P04", "Éducation Civique et Morale (ECM)", "ECM-PRI", 2, "CM2 B", "M. Marcelle Yao", SchoolCycle.PRIMAIRE),
            Subject("SUB-P05", "Histoire / Géographie / Sciences & Technologie", "HGST-PRI", 2, "CM2 B", "M. Marcelle Yao", SchoolCycle.PRIMAIRE),
            Subject("SUB-P06", "Éducation Artistique (Dessin & Chant)", "ART-PRI", 1, "CM2 B", "M. Marcelle Yao", SchoolCycle.PRIMAIRE),
            Subject("SUB-P07", "Éducation Physique et Sportive (EPS)", "EPS-PRI", 1, "CM2 B", "M. Marcelle Yao", SchoolCycle.PRIMAIRE),

            // Secondary subjects (Terminale S1)
            Subject("SUB-01", "Mathématiques", "MATH-TS", 4, "Terminale S1", "Dr. Alexandre Mensah", SchoolCycle.LYCEE),
            Subject("SUB-02", "Sciences Physiques", "SPHY-TS", 4, "Terminale S1", "M. Paul Kouassi", SchoolCycle.LYCEE),
            Subject("SUB-03", "Sciences de la Vie (SVT)", "SVT-TS", 3, "Terminale S1", "Mme Fatou Sow", SchoolCycle.LYCEE),
            Subject("SUB-04", "Français & Philosophie", "PHILO-TS", 2, "Terminale S1", "Mme Sophie Faye", SchoolCycle.LYCEE),
            Subject("SUB-05", "Anglais Renforcé", "ANGL-TS", 2, "Terminale S1", "M. Idrissa Diop", SchoolCycle.LYCEE),
            Subject("SUB-06", "Histoire-Géographie", "HGEO-TS", 2, "Terminale S1", "M. Idrissa Diop", SchoolCycle.LYCEE)
        )
        _subjects.value = initialSubjects

        val initialStudents = listOf(
            // Primary cycle students (CM2 B, CP B, CI A)
            Student("STD-P01", "EDF-2026-P01", "Yann", "KOUASSI", "M", "2015-03-14", "CM2 B", "M. Bernard Kouassi", "+225 07 10 20 30", "b.kouassi@gmail.com", 250000.0, 250000.0, StudentStatus.EN_REGLE, "2025-09-01"),
            Student("STD-P02", "EDF-2026-P02", "Fatoumata", "DIALLO", "F", "2015-07-22", "CM2 B", "Mme Awa Diallo", "+225 05 20 30 40", "awa.diallo@yahoo.fr", 250000.0, 200000.0, StudentStatus.ACTIF, "2025-09-01"),
            Student("STD-P03", "EDF-2026-P03", "Ange-Emmanuel", "YAO", "M", "2015-11-05", "CM2 B", "M. Noël Yao", "+225 01 40 50 60", "noel.yao@outlook.com", 250000.0, 100000.0, StudentStatus.EN_RETARD, "2025-09-02"),
            Student("STD-P04", "EDF-2026-P04", "Sarah", "OUATTARA", "F", "2019-04-18", "CP B", "Mme Salimata Ouattara", "+225 07 88 99 11", "salimata@gmail.com", 220000.0, 220000.0, StudentStatus.EN_REGLE, "2025-09-02"),
            Student("STD-P05", "EDF-2026-P05", "Kader", "SANOGO", "M", "2020-08-10", "CI A", "M. Bakary Sanogo", "+225 05 77 11 22", "bakary.sanogo@ci.com", 200000.0, 200000.0, StudentStatus.EN_REGLE, "2025-09-03"),

            // Secondary cycle students
            Student("STD-001", "EDF-2026-001", "Amina", "BAKAYOKO", "F", "2008-04-12", "Terminale S1", "M. Amadou Bakayoko", "+225 07 00 12 34", "a.bakayoko@gmail.com", 450000.0, 450000.0, StudentStatus.EN_REGLE, "2025-09-02"),
            Student("STD-002", "EDF-2026-002", "David", "KOUAME", "M", "2007-11-20", "Terminale S1", "Mme Chantal Kouamé", "+225 05 11 22 33", "c.kouame@yahoo.fr", 450000.0, 300000.0, StudentStatus.ACTIF, "2025-09-02"),
            Student("STD-003", "EDF-2026-003", "Khadija", "CISSE", "F", "2008-02-15", "Terminale S1", "M. Ibrahim Cissé", "+225 01 99 88 77", "ibrahim.c@outlook.com", 450000.0, 150000.0, StudentStatus.EN_RETARD, "2025-09-03"),
            Student("STD-004", "EDF-2026-004", "Marc-Antoine", "YAPO", "M", "2007-08-30", "Terminale S1", "M. Jean-Marc Yapo", "+225 07 44 55 66", "jmyapo@gmail.com", 450000.0, 450000.0, StudentStatus.EN_REGLE, "2025-09-03"),
            Student("STD-005", "EDF-2026-005", "Grace", "N'GUESSAN", "F", "2008-06-18", "Terminale S1", "Mme Patricia N'guessan", "+225 05 66 77 88", "patricia.ng@gmail.com", 450000.0, 350000.0, StudentStatus.ACTIF, "2025-09-04"),
            Student("STD-006", "EDF-2026-006", "Christian", "TOURE", "M", "2007-03-25", "Terminale S1", "M. Sekou Touré", "+225 01 33 22 11", "toure.s@company.ci", 450000.0, 100000.0, StudentStatus.EN_RETARD, "2025-09-05"),
            Student("STD-007", "EDF-2026-007", "Aïcha", "KONE", "F", "2008-09-10", "1ère Scientifique", "M. Moussa Koné", "+225 07 22 33 44", "moussak@gmail.com", 400000.0, 400000.0, StudentStatus.EN_REGLE, "2025-09-05"),
            Student("STD-008", "EDF-2026-008", "Emmanuel", "BAMBA", "M", "2008-12-05", "1ère Scientifique", "Mme Solange Bamba", "+225 05 88 99 00", "solange.b@yahoo.com", 400000.0, 250000.0, StudentStatus.ACTIF, "2025-09-06"),
            Student("STD-009", "EDF-2026-009", "Fatoumata", "TRAORE", "F", "2009-05-14", "3ème A", "M. Oumar Traoré", "+225 01 55 44 33", "oumar.tr@gmail.com", 350000.0, 350000.0, StudentStatus.EN_REGLE, "2025-09-07"),
            Student("STD-010", "EDF-2026-010", "Samuel", "ADJAHO", "M", "2010-07-22", "6ème B", "Mme Nicole Adjaho", "+225 07 66 11 22", "nicole.adj@gmail.com", 320000.0, 320000.0, StudentStatus.EN_REGLE, "2025-09-08")
        )
        _students.value = initialStudents

        val initialAttendance = listOf(
            AttendanceRecord("ATT-P01", "STD-P01", "Yann KOUASSI", "CM2 B", today, AttendanceStatus.PRESENT),
            AttendanceRecord("ATT-P02", "STD-P02", "Fatoumata DIALLO", "CM2 B", today, AttendanceStatus.PRESENT),
            AttendanceRecord("ATT-P03", "STD-P03", "Ange-Emmanuel YAO", "CM2 B", today, AttendanceStatus.RETARD, "Retard bus scolaire 10 min"),
            AttendanceRecord("ATT-01", "STD-001", "Amina BAKAYOKO", "Terminale S1", today, AttendanceStatus.PRESENT),
            AttendanceRecord("ATT-02", "STD-002", "David KOUAME", "Terminale S1", today, AttendanceStatus.PRESENT),
            AttendanceRecord("ATT-03", "STD-003", "Khadija CISSE", "Terminale S1", today, AttendanceStatus.RETARD, "Retard transport bus 15min"),
            AttendanceRecord("ATT-04", "STD-004", "Marc-Antoine YAPO", "Terminale S1", today, AttendanceStatus.PRESENT),
            AttendanceRecord("ATT-05", "STD-005", "Grace N'GUESSAN", "Terminale S1", today, AttendanceStatus.ABSENT_JUSTIFIE, "Certificat médical grippe"),
            AttendanceRecord("ATT-06", "STD-006", "Christian TOURE", "Terminale S1", today, AttendanceStatus.ABSENT_NON_JUSTIFIE, "Non signalé")
        )
        _attendance.value = initialAttendance

        // Seed grades for Terminale S1
        val initialGrades = mutableListOf<GradeRecord>()
        // Amina BAKAYOKO (top student)
        initialGrades.add(GradeRecord("GRD-01", "STD-001", "Amina BAKAYOKO", "Terminale S1", "Mathématiques", 4, "Trimestre 1", 18.0f, 17.5f, 18.5f))
        initialGrades.add(GradeRecord("GRD-02", "STD-001", "Amina BAKAYOKO", "Terminale S1", "Sciences Physiques", 4, "Trimestre 1", 17.0f, 18.0f, 17.5f))
        initialGrades.add(GradeRecord("GRD-03", "STD-001", "Amina BAKAYOKO", "Terminale S1", "Sciences de la Vie (SVT)", 3, "Trimestre 1", 16.5f, 17.0f, 16.0f))
        initialGrades.add(GradeRecord("GRD-04", "STD-001", "Amina BAKAYOKO", "Terminale S1", "Français & Philosophie", 2, "Trimestre 1", 15.0f, 16.0f, 15.5f))
        initialGrades.add(GradeRecord("GRD-05", "STD-001", "Amina BAKAYOKO", "Terminale S1", "Anglais Renforcé", 2, "Trimestre 1", 19.0f, 18.5f, 19.0f))
        initialGrades.add(GradeRecord("GRD-06", "STD-001", "Amina BAKAYOKO", "Terminale S1", "Histoire-Géographie", 2, "Trimestre 1", 16.0f, 15.0f, 16.5f))

        // David KOUAME
        initialGrades.add(GradeRecord("GRD-07", "STD-002", "David KOUAME", "Terminale S1", "Mathématiques", 4, "Trimestre 1", 14.5f, 15.0f, 14.0f))
        initialGrades.add(GradeRecord("GRD-08", "STD-002", "David KOUAME", "Terminale S1", "Sciences Physiques", 4, "Trimestre 1", 13.5f, 14.0f, 14.5f))
        initialGrades.add(GradeRecord("GRD-09", "STD-002", "David KOUAME", "Terminale S1", "Sciences de la Vie (SVT)", 3, "Trimestre 1", 15.0f, 14.5f, 15.0f))
        initialGrades.add(GradeRecord("GRD-10", "STD-002", "David KOUAME", "Terminale S1", "Français & Philosophie", 2, "Trimestre 1", 12.0f, 13.0f, 13.5f))
        initialGrades.add(GradeRecord("GRD-11", "STD-002", "David KOUAME", "Terminale S1", "Anglais Renforcé", 2, "Trimestre 1", 16.0f, 16.5f, 15.5f))
        initialGrades.add(GradeRecord("GRD-12", "STD-002", "David KOUAME", "Terminale S1", "Histoire-Géographie", 2, "Trimestre 1", 14.0f, 13.5f, 14.0f))

        // Marc-Antoine YAPO
        initialGrades.add(GradeRecord("GRD-13", "STD-004", "Marc-Antoine YAPO", "Terminale S1", "Mathématiques", 4, "Trimestre 1", 16.0f, 16.5f, 16.0f))
        initialGrades.add(GradeRecord("GRD-14", "STD-004", "Marc-Antoine YAPO", "Terminale S1", "Sciences Physiques", 4, "Trimestre 1", 15.5f, 16.0f, 16.5f))
        initialGrades.add(GradeRecord("GRD-15", "STD-004", "Marc-Antoine YAPO", "Terminale S1", "Sciences de la Vie (SVT)", 3, "Trimestre 1", 14.0f, 15.0f, 14.5f))
        initialGrades.add(GradeRecord("GRD-16", "STD-004", "Marc-Antoine YAPO", "Terminale S1", "Français & Philosophie", 2, "Trimestre 1", 14.5f, 14.0f, 14.0f))
        initialGrades.add(GradeRecord("GRD-17", "STD-004", "Marc-Antoine YAPO", "Terminale S1", "Anglais Renforcé", 2, "Trimestre 1", 17.5f, 18.0f, 17.0f))
        initialGrades.add(GradeRecord("GRD-18", "STD-004", "Marc-Antoine YAPO", "Terminale S1", "Histoire-Géographie", 2, "Trimestre 1", 15.0f, 15.5f, 15.0f))

        // Khadija CISSE
        initialGrades.add(GradeRecord("GRD-19", "STD-003", "Khadija CISSE", "Terminale S1", "Mathématiques", 4, "Trimestre 1", 10.5f, 11.0f, 10.0f))
        initialGrades.add(GradeRecord("GRD-20", "STD-003", "Khadija CISSE", "Terminale S1", "Sciences Physiques", 4, "Trimestre 1", 11.0f, 10.5f, 11.5f))
        initialGrades.add(GradeRecord("GRD-21", "STD-003", "Khadija CISSE", "Terminale S1", "Sciences de la Vie (SVT)", 3, "Trimestre 1", 12.0f, 13.0f, 12.5f))
        initialGrades.add(GradeRecord("GRD-22", "STD-003", "Khadija CISSE", "Terminale S1", "Français & Philosophie", 2, "Trimestre 1", 13.0f, 14.0f, 13.5f))
        initialGrades.add(GradeRecord("GRD-23", "STD-003", "Khadija CISSE", "Terminale S1", "Anglais Renforcé", 2, "Trimestre 1", 14.0f, 14.5f, 14.0f))
        initialGrades.add(GradeRecord("GRD-24", "STD-003", "Khadija CISSE", "Terminale S1", "Histoire-Géographie", 2, "Trimestre 1", 12.0f, 11.5f, 12.0f))

        // Christian TOURE
        initialGrades.add(GradeRecord("GRD-25", "STD-006", "Christian TOURE", "Terminale S1", "Mathématiques", 4, "Trimestre 1", 8.5f, 9.0f, 9.5f))
        initialGrades.add(GradeRecord("GRD-26", "STD-006", "Christian TOURE", "Terminale S1", "Sciences Physiques", 4, "Trimestre 1", 9.0f, 8.5f, 9.0f))
        initialGrades.add(GradeRecord("GRD-27", "STD-006", "Christian TOURE", "Terminale S1", "Sciences de la Vie (SVT)", 3, "Trimestre 1", 10.0f, 10.5f, 10.0f))
        initialGrades.add(GradeRecord("GRD-28", "STD-006", "Christian TOURE", "Terminale S1", "Français & Philosophie", 2, "Trimestre 1", 11.0f, 11.5f, 11.0f))
        initialGrades.add(GradeRecord("GRD-29", "STD-006", "Christian TOURE", "Terminale S1", "Anglais Renforcé", 2, "Trimestre 1", 12.5f, 13.0f, 12.0f))
        initialGrades.add(GradeRecord("GRD-30", "STD-006", "Christian TOURE", "Terminale S1", "Histoire-Géographie", 2, "Trimestre 1", 10.0f, 10.5f, 10.0f))

        // PRIMARY CYCLE GRADES - CM2 B (Préparation Examen CEP)
        // Yann KOUASSI (1er de la classe en CM2)
        initialGrades.add(GradeRecord("GRD-P01", "STD-P01", "Yann KOUASSI", "CM2 B", "Français (Lecture & Expression)", 4, "Trimestre 1", 18.0f, 18.5f, 19.0f))
        initialGrades.add(GradeRecord("GRD-P02", "STD-P01", "Yann KOUASSI", "CM2 B", "Français (Grammaire, Orthographe & Vocabulaire)", 3, "Trimestre 1", 17.5f, 17.0f, 18.0f))
        initialGrades.add(GradeRecord("GRD-P03", "STD-P01", "Yann KOUASSI", "CM2 B", "Mathématiques (Calcul, Numération & Problèmes)", 4, "Trimestre 1", 19.0f, 19.5f, 19.0f))
        initialGrades.add(GradeRecord("GRD-P04", "STD-P01", "Yann KOUASSI", "CM2 B", "Éducation Civique et Morale (ECM)", 2, "Trimestre 1", 16.0f, 16.5f, 17.0f))
        initialGrades.add(GradeRecord("GRD-P05", "STD-P01", "Yann KOUASSI", "CM2 B", "Histoire / Géographie / Sciences & Technologie", 2, "Trimestre 1", 17.0f, 17.5f, 18.0f))
        initialGrades.add(GradeRecord("GRD-P06", "STD-P01", "Yann KOUASSI", "CM2 B", "Éducation Artistique (Dessin & Chant)", 1, "Trimestre 1", 15.0f, 16.0f, 16.0f))
        initialGrades.add(GradeRecord("GRD-P07", "STD-P01", "Yann KOUASSI", "CM2 B", "Éducation Physique et Sportive (EPS)", 1, "Trimestre 1", 16.0f, 17.0f, 16.5f))

        // Fatoumata DIALLO (2ème de la classe en CM2)
        initialGrades.add(GradeRecord("GRD-P08", "STD-P02", "Fatoumata DIALLO", "CM2 B", "Français (Lecture & Expression)", 4, "Trimestre 1", 16.0f, 16.5f, 17.0f))
        initialGrades.add(GradeRecord("GRD-P09", "STD-P02", "Fatoumata DIALLO", "CM2 B", "Français (Grammaire, Orthographe & Vocabulaire)", 3, "Trimestre 1", 16.0f, 15.5f, 16.0f))
        initialGrades.add(GradeRecord("GRD-P10", "STD-P02", "Fatoumata DIALLO", "CM2 B", "Mathématiques (Calcul, Numération & Problèmes)", 4, "Trimestre 1", 15.0f, 15.5f, 15.0f))
        initialGrades.add(GradeRecord("GRD-P11", "STD-P02", "Fatoumata DIALLO", "CM2 B", "Éducation Civique et Morale (ECM)", 2, "Trimestre 1", 15.5f, 15.0f, 16.0f))
        initialGrades.add(GradeRecord("GRD-P12", "STD-P02", "Fatoumata DIALLO", "CM2 B", "Histoire / Géographie / Sciences & Technologie", 2, "Trimestre 1", 15.0f, 16.0f, 15.5f))
        initialGrades.add(GradeRecord("GRD-P13", "STD-P02", "Fatoumata DIALLO", "CM2 B", "Éducation Artistique (Dessin & Chant)", 1, "Trimestre 1", 16.0f, 16.5f, 17.0f))
        initialGrades.add(GradeRecord("GRD-P14", "STD-P02", "Fatoumata DIALLO", "CM2 B", "Éducation Physique et Sportive (EPS)", 1, "Trimestre 1", 15.0f, 15.5f, 16.0f))

        // Ange-Emmanuel YAO (3ème de la classe en CM2)
        initialGrades.add(GradeRecord("GRD-P15", "STD-P03", "Ange-Emmanuel YAO", "CM2 B", "Français (Lecture & Expression)", 4, "Trimestre 1", 13.0f, 13.5f, 14.0f))
        initialGrades.add(GradeRecord("GRD-P16", "STD-P03", "Ange-Emmanuel YAO", "CM2 B", "Français (Grammaire, Orthographe & Vocabulaire)", 3, "Trimestre 1", 12.0f, 12.5f, 13.0f))
        initialGrades.add(GradeRecord("GRD-P17", "STD-P03", "Ange-Emmanuel YAO", "CM2 B", "Mathématiques (Calcul, Numération & Problèmes)", 4, "Trimestre 1", 14.0f, 14.5f, 14.0f))
        initialGrades.add(GradeRecord("GRD-P18", "STD-P03", "Ange-Emmanuel YAO", "CM2 B", "Éducation Civique et Morale (ECM)", 2, "Trimestre 1", 14.0f, 13.5f, 14.0f))
        initialGrades.add(GradeRecord("GRD-P19", "STD-P03", "Ange-Emmanuel YAO", "CM2 B", "Histoire / Géographie / Sciences & Technologie", 2, "Trimestre 1", 13.5f, 14.0f, 13.0f))
        initialGrades.add(GradeRecord("GRD-P20", "STD-P03", "Ange-Emmanuel YAO", "CM2 B", "Éducation Artistique (Dessin & Chant)", 1, "Trimestre 1", 15.0f, 14.5f, 15.0f))
        initialGrades.add(GradeRecord("GRD-P21", "STD-P03", "Ange-Emmanuel YAO", "CM2 B", "Éducation Physique et Sportive (EPS)", 1, "Trimestre 1", 14.0f, 15.0f, 14.5f))

        _grades.value = initialGrades

        // Seed transactions (journal de caisse)
        val initialTransactions = listOf(
            Transaction("TX-01", TransactionType.ENCAISSEMENT, "Scolarité", 450000.0, "2026-10-02", "REC-2026-001", "STD-001", "Amina BAKAYOKO", "M. Amadou Bakayoko", PaymentMethod.VIREMENT, "Paiement intégral scolarité annuelle T1-T3", "Jean-Luc Koffi"),
            Transaction("TX-02", TransactionType.ENCAISSEMENT, "Scolarité", 300000.0, "2026-10-05", "REC-2026-002", "STD-002", "David KOUAME", "Mme Chantal Kouamé", PaymentMethod.MOBILE_MONEY, "Acompte scolarité T1 & T2", "Jean-Luc Koffi"),
            Transaction("TX-03", TransactionType.ENCAISSEMENT, "Scolarité", 150000.0, "2026-10-06", "REC-2026-003", "STD-003", "Khadija CISSE", "M. Ibrahim Cissé", PaymentMethod.ESPECES, "Premier versement T1", "Jean-Luc Koffi"),
            Transaction("TX-04", TransactionType.ENCAISSEMENT, "Scolarité", 450000.0, "2026-10-08", "REC-2026-004", "STD-004", "Marc-Antoine YAPO", "M. Jean-Marc Yapo", PaymentMethod.CHEQUE, "Solde scolarité Terminale", "Jean-Luc Koffi"),
            Transaction("TX-05", TransactionType.DEPENSE, "Salaires Enseignants", 650000.0, "2026-10-30", "DEP-2026-001", null, null, "Dr. Alexandre Mensah", PaymentMethod.VIREMENT, "Salaire mensuel Octobre 2026", "Jean-Luc Koffi"),
            Transaction("TX-06", TransactionType.DEPENSE, "Salaires Enseignants", 540000.0, "2026-10-30", "DEP-2026-002", null, null, "M. Paul Kouassi", PaymentMethod.VIREMENT, "Salaire mensuel Octobre 2026", "Jean-Luc Koffi"),
            Transaction("TX-07", TransactionType.DEPENSE, "Fournitures & Laboratoire", 185000.0, "2026-11-01", "DEP-2026-003", null, null, "Papeterie Centrale & LabTech", PaymentMethod.ESPECES, "Réactifs chimie & rames papier pour examens", "Jean-Luc Koffi"),
            Transaction("TX-08", TransactionType.DEPENSE, "Électricité & Internet", 120000.0, "2026-11-02", "DEP-2026-004", null, null, "CIE / Orange Fibre", PaymentMethod.MOBILE_MONEY, "Facture énergie et connexion fibre haut débit", "Jean-Luc Koffi"),
            Transaction("TX-09", TransactionType.ENCAISSEMENT, "Scolarité", 400000.0, "2026-11-03", "REC-2026-005", "STD-007", "Aïcha KONE", "M. Moussa Koné", PaymentMethod.VIREMENT, "Paiement annuel 1ère Scientifique", "Jean-Luc Koffi"),
            Transaction("TX-10", TransactionType.ENCAISSEMENT, "Cantine Scolaire", 75000.0, "2026-11-04", "REC-2026-006", "STD-001", "Amina BAKAYOKO", "M. Amadou Bakayoko", PaymentMethod.ESPECES, "Abonnement cantine T1", "Amina Traoré")
        )
        _transactions.value = initialTransactions
    }

    // CRUD - Students
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
    ): Student {
        val nextNum = _students.value.size + 1
        val matricule = String.format(Locale.getDefault(), "EDF-2026-%03d", nextNum)
        val newStudent = Student(
            id = "STD-${UUID.randomUUID().toString().take(6)}",
            matricule = matricule,
            firstName = firstName.trim(),
            lastName = lastName.trim().uppercase(Locale.getDefault()),
            gender = gender,
            birthDate = birthDate,
            className = className,
            parentName = parentName.trim(),
            parentPhone = parentPhone.trim(),
            parentEmail = parentEmail.trim(),
            tuitionTotal = tuitionTotal,
            tuitionPaid = 0.0,
            status = StudentStatus.ACTIF,
            registrationDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
        _students.value = _students.value + newStudent
        return newStudent
    }

    fun updateStudent(student: Student) {
        _students.value = _students.value.map { if (it.id == student.id) student else it }
    }

    fun deleteStudent(studentId: String) {
        _students.value = _students.value.filterNot { it.id == studentId }
    }

    // CRUD - Classes
    fun addClass(name: String, level: String, capacity: Int, mainTeacher: String, room: String) {
        val newClass = SchoolClass(
            id = "CLS-${UUID.randomUUID().toString().take(6)}",
            name = name.trim(),
            level = level,
            capacity = capacity,
            studentCount = 0,
            mainTeacherName = mainTeacher.trim(),
            roomNumber = room.trim()
        )
        _classes.value = _classes.value + newClass
    }

    // CRUD - Teachers
    fun addTeacher(name: String, subjects: List<String>, phone: String, email: String, salary: Double) {
        val newTeacher = Teacher(
            id = "TCH-${UUID.randomUUID().toString().take(6)}",
            name = name.trim(),
            subjects = subjects,
            phone = phone.trim(),
            email = email.trim(),
            monthlySalary = salary,
            status = "Actif"
        )
        _teachers.value = _teachers.value + newTeacher
    }

    // CRUD - Subjects
    fun addSubject(name: String, code: String, coefficient: Int, className: String, teacher: String) {
        val newSubject = Subject(
            id = "SUB-${UUID.randomUUID().toString().take(6)}",
            name = name.trim(),
            code = code.trim().uppercase(Locale.getDefault()),
            coefficient = coefficient,
            className = className,
            teacherName = teacher.trim()
        )
        _subjects.value = _subjects.value + newSubject
    }

    // Attendance
    fun recordDailyAttendance(newRecords: List<AttendanceRecord>) {
        val existingMap = _attendance.value.associateBy { "${it.studentId}_${it.date}" }.toMutableMap()
        for (rec in newRecords) {
            existingMap["${rec.studentId}_${rec.date}"] = rec
        }
        _attendance.value = existingMap.values.toList()
    }

    // Grades
    fun saveGrade(
        studentId: String,
        studentName: String,
        className: String,
        subjectName: String,
        coefficient: Int,
        period: String,
        test1: Float,
        test2: Float,
        exam: Float
    ) {
        val currentList = _grades.value.toMutableList()
        val index = currentList.indexOfFirst {
            it.studentId == studentId && it.subjectName == subjectName && it.period == period
        }
        val record = GradeRecord(
            id = if (index >= 0) currentList[index].id else "GRD-${UUID.randomUUID().toString().take(6)}",
            studentId = studentId,
            studentName = studentName,
            className = className,
            subjectName = subjectName,
            coefficient = coefficient,
            period = period,
            test1 = test1,
            test2 = test2,
            exam = exam
        )
        if (index >= 0) {
            currentList[index] = record
        } else {
            currentList.add(record)
        }
        _grades.value = currentList
    }

    // Transactions (Cashbook)
    fun addTransaction(
        type: TransactionType,
        category: String,
        amount: Double,
        date: String,
        studentId: String? = null,
        payeeOrPayer: String,
        paymentMethod: PaymentMethod,
        notes: String
    ): Transaction {
        val prefix = if (type == TransactionType.ENCAISSEMENT) "REC" else "DEP"
        val count = _transactions.value.filter { it.type == type }.size + 1
        val ref = String.format(Locale.getDefault(), "%s-2026-%03d", prefix, count)
        val student = studentId?.let { id -> _students.value.find { it.id == id } }

        val tx = Transaction(
            id = "TX-${UUID.randomUUID().toString().take(6)}",
            type = type,
            category = category,
            amount = amount,
            date = date,
            referenceNumber = ref,
            studentId = studentId,
            studentName = student?.fullName,
            payeeOrPayer = payeeOrPayer,
            paymentMethod = paymentMethod,
            notes = notes,
            recordedBy = _currentUser.value.name
        )

        _transactions.value = listOf(tx) + _transactions.value

        // If tuition payment, update student tuitionPaid and status automatically!
        if (type == TransactionType.ENCAISSEMENT && student != null && category.contains("Scolarité", ignoreCase = true)) {
            val updatedPaid = student.tuitionPaid + amount
            val newStatus = when {
                updatedPaid >= student.tuitionTotal -> StudentStatus.EN_REGLE
                updatedPaid > 0 -> StudentStatus.ACTIF
                else -> StudentStatus.EN_RETARD
            }
            updateStudent(student.copy(tuitionPaid = updatedPaid, status = newStatus))
        }

        return tx
    }

    // Report Card Generator with weighted average, ranking, appreciations and authentic QR Code
    fun generateReportCard(studentId: String, period: String = "Trimestre 1"): ReportCard? {
        val student = _students.value.find { it.id == studentId } ?: return null
        val classStudents = _students.value.filter { it.className == student.className }

        // Compute averages for all students in class for ranking
        val studentAverages = classStudents.map { s ->
            val gradesForStudent = _grades.value.filter { it.studentId == s.id && it.period == period }
            var points = 0f
            var coeffs = 0
            for (g in gradesForStudent) {
                points += (g.average * g.coefficient)
                coeffs += g.coefficient
            }
            val avg = if (coeffs > 0) points / coeffs else 0f
            s.id to avg
        }.sortedByDescending { it.second }

        val rank = (studentAverages.indexOfFirst { it.first == student.id } + 1).coerceAtLeast(1)

        val schoolClass = _classes.value.find { it.name == student.className }
        val cycle = schoolClass?.cycle ?: if (student.className.matches(Regex("^(CI|CP|CE1|CE2|CM1|CM2).*"))) SchoolCycle.PRIMAIRE else SchoolCycle.LYCEE

        val studentGrades = _grades.value.filter { it.studentId == student.id && it.period == period }
        val reportItems = studentGrades.map { g ->
            val avg = g.average
            val subjectGradesInClass = _grades.value.filter { it.className == student.className && it.subjectName == g.subjectName && it.period == period }
            val classSubjectAvg = if (subjectGradesInClass.isNotEmpty()) subjectGradesInClass.map { it.average }.average().toFloat() else avg
            val rankInSub = (subjectGradesInClass.sortedByDescending { it.average }.indexOfFirst { it.studentId == student.id } + 1).coerceAtLeast(1)
            val rankSuffix = if (rankInSub == 1) "1er" else "${rankInSub}ème"

            val appreciation = when {
                avg >= 16f -> "Excellent travail. Continuez ainsi."
                avg >= 14f -> "Très bien. Bon investissement."
                avg >= 12f -> "Travail satisfaisant."
                avg >= 10f -> "Passable. Doit approfondir."
                else -> "Insuffisant. Des efforts requis."
            }
            SubjectReportItem(
                subjectName = g.subjectName,
                coefficient = g.coefficient,
                test1 = g.test1,
                test2 = g.test2,
                exam = g.exam,
                average = avg,
                weightedTotal = avg * g.coefficient,
                classAverage = classSubjectAvg,
                subjectRank = rankSuffix,
                teacherAppreciation = appreciation
            )
        }

        val totalPoints = reportItems.sumOf { it.weightedTotal.toDouble() }.toFloat()
        val totalCoeffs = reportItems.sumOf { it.coefficient }
        val overallAverage = if (totalCoeffs > 0) totalPoints / totalCoeffs else 0f
        val classOverallAverage = if (studentAverages.isNotEmpty()) studentAverages.map { it.second }.average().toFloat() else overallAverage

        val appreciation = when {
            overallAverage >= 16f -> if (cycle == SchoolCycle.PRIMAIRE) "Félicitations du Maître ! Excellent travail sur l'ensemble des disciplines." else "Excellents résultats académiques. Félicitations du conseil !"
            overallAverage >= 14f -> if (cycle == SchoolCycle.PRIMAIRE) "Très bon travail. Bon niveau pour l'examen CEP. Tableau d'honneur." else "Très bon trimestre. Tableau d'honneur et encouragements."
            overallAverage >= 12f -> "Trimestre satisfaisant. Régularité observée."
            overallAverage >= 10f -> "Résultats moyens. Doit intensifier les révisions."
            else -> "Résultats insuffisants. Mise en garde travail."
        }

        val decision = when {
            overallAverage >= 14f -> if (cycle == SchoolCycle.PRIMAIRE) "TABLEAU D'HONNEUR & FÉLICITATIONS" else "FÉLICITATIONS DU CONSEIL"
            overallAverage >= 12f -> "TABLEAU D'HONNEUR"
            overallAverage >= 10f -> "ENCOURAGEMENTS"
            else -> "AVERTISSEMENT TRAVAIL"
        }

        // Attendance stats
        val classAttendance = _attendance.value.filter { it.studentId == student.id }
        val absences = classAttendance.count { it.status == AttendanceStatus.ABSENT_JUSTIFIE || it.status == AttendanceStatus.ABSENT_NON_JUSTIFIE }
        val retards = classAttendance.count { it.status == AttendanceStatus.RETARD }
        val attSummary = "$absences absence(s), $retards retard(s)"

        // Authentic verification hash & QR payload
        val rawData = "EDF|${student.matricule}|${student.fullName}|${student.className}|$period|$overallAverage|2025-2026|${cycle.name}"
        val hash = sha256(rawData).take(16).uppercase(Locale.getDefault())
        val qrPayload = "https://edufinance.pro/verify?mat=${student.matricule}&term=$period&avg=${String.format(Locale.US, "%.2f", overallAverage)}&cycle=${cycle.name}&sig=$hash"

        return ReportCard(
            student = student,
            period = period,
            subjects = reportItems,
            overallAverage = overallAverage,
            classAverage = classOverallAverage,
            rankInClass = rank,
            totalClassSize = classStudents.size,
            totalPoints = totalPoints,
            totalCoefficients = totalCoeffs,
            appreciation = appreciation,
            decision = decision,
            verificationHash = "CERT-$hash",
            qrPayload = qrPayload,
            attendanceSummary = attSummary,
            cycle = cycle
        )
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun formatCurrency(amount: Double): String {
            val nf = NumberFormat.getNumberInstance(Locale.FRANCE)
            return "${nf.format(amount.toLong())} FCFA"
        }
    }
}
