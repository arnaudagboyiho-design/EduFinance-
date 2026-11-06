package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.AttendanceStatusPill
import com.example.ui.components.QrCodeView
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PedagogyScreen(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    students: List<Student>,
    classes: List<SchoolClass>,
    subjects: List<Subject>,
    grades: List<GradeRecord>,
    attendance: List<AttendanceRecord>,
    currentUser: UserSession,
    onGenerateReportCard: (String) -> Unit,
    onOpenNewGrade: () -> Unit,
    onUpdateAttendance: (String, AttendanceStatus, String) -> Unit,
    onVerifyQrClick: () -> Unit,
    selectedReportCard: ReportCard?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("pedagogy_screen")
    ) {
        TabRow(
            selectedTabIndex = currentTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Blue600,
            modifier = Modifier.testTag("pedagogy_tabs")
        ) {
            val tabs = listOf("Bulletins & QR Code", "Saisie des Notes", "Fiche de Présence")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = currentTab == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (currentTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        when (currentTab) {
            0 -> {
                // Report Cards & Official Bulletin viewer tab
                ReportCardsTab(
                    students = students,
                    classes = classes,
                    selectedReportCard = selectedReportCard,
                    onGenerateReportCard = onGenerateReportCard,
                    onVerifyQrClick = onVerifyQrClick
                )
            }

            1 -> {
                // Grades Entry Tab
                GradesEntryTab(
                    students = students,
                    subjects = subjects,
                    grades = grades,
                    currentUser = currentUser,
                    onOpenNewGrade = onOpenNewGrade
                )
            }

            2 -> {
                // Daily Attendance Sheet Tab
                AttendanceSheetTab(
                    students = students,
                    classes = classes,
                    attendance = attendance,
                    currentUser = currentUser,
                    onUpdateAttendance = onUpdateAttendance
                )
            }
        }
    }
}

@Composable
fun ReportCardsTab(
    students: List<Student>,
    classes: List<SchoolClass>,
    selectedReportCard: ReportCard?,
    onGenerateReportCard: (String) -> Unit,
    onVerifyQrClick: () -> Unit
) {
    var selectedCycleFilter by remember { mutableStateOf<SchoolCycle?>(null) }
    val classMap = remember(classes) { classes.associateBy { it.name } }

    val filteredStudents = remember(students, classMap, selectedCycleFilter) {
        if (selectedCycleFilter == null) students
        else {
            students.filter { st ->
                val cl = classMap[st.className]
                if (selectedCycleFilter == SchoolCycle.PRIMAIRE) {
                    cl?.cycle == SchoolCycle.PRIMAIRE || st.className.startsWith("CI") || st.className.startsWith("CP") || st.className.startsWith("CE") || st.className.startsWith("CM")
                } else {
                    cl?.cycle != SchoolCycle.PRIMAIRE
                }
            }
        }
    }

    var selectedStudentId by remember {
        mutableStateOf(students.firstOrNull()?.id ?: "")
    }

    // Automatically select first in filtered if current is not in filtered
    LaunchedEffect(filteredStudents) {
        if (filteredStudents.none { it.id == selectedStudentId } && filteredStudents.isNotEmpty()) {
            selectedStudentId = filteredStudents.first().id
        }
    }

    LaunchedEffect(selectedStudentId) {
        if (selectedStudentId.isNotEmpty()) {
            onGenerateReportCard(selectedStudentId)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Student Selector Carousel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Sélectionner le cycle & l'élève pour le bulletin :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Cycle Filter Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedCycleFilter == null,
                            onClick = { selectedCycleFilter = null },
                            label = { Text("Tous (${students.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedCycleFilter == SchoolCycle.PRIMAIRE,
                            onClick = { selectedCycleFilter = SchoolCycle.PRIMAIRE },
                            label = { Text("Primaire (CI - CM2)", fontSize = 11.sp) },
                            leadingIcon = {
                                if (selectedCycleFilter == SchoolCycle.PRIMAIRE) {
                                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                        FilterChip(
                            selected = selectedCycleFilter == SchoolCycle.COLLEGE,
                            onClick = { selectedCycleFilter = SchoolCycle.COLLEGE },
                            label = { Text("Secondaire", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredStudents) { student ->
                            val isSelected = student.id == selectedStudentId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedStudentId = student.id
                                    onGenerateReportCard(student.id)
                                },
                                label = {
                                    Text(
                                        text = "${student.lastName} ${student.firstName.take(1)}. (${student.className})",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Official Report Card Sheet
        if (selectedReportCard != null) {
            item {
                OfficialReportCardView(
                    report = selectedReportCard,
                    onVerifyQrClick = onVerifyQrClick
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sélectionnez un élève pour afficher son bulletin officiel.", color = Slate400)
                    }
                }
            }
        }
    }
}

@Composable
fun OfficialReportCardView(
    report: ReportCard,
    onVerifyQrClick: () -> Unit
) {
    val isPrimary = report.cycle == SchoolCycle.PRIMAIRE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("official_report_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Republic / School
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPrimary) "GROUPE SCOLAIRE EXCELLENCE • CYCLE PRIMAIRE (CI - CM2)" else "ÉTABLISSEMENT SCOLAIRE EXCELLENCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Slate900,
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (isPrimary) "Inspection de l'Enseignement Préscolaire et Primaire (IEPP)" else "Ministère de l'Éducation Nationale",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPrimary) Emerald700 else Slate900)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isPrimary) "BULLETIN TRIMESTRIEL - PRIMAIRE" else "BULLETIN OFFICIEL",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Slate300, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Student identity summary banner
            Surface(
                color = Slate50,
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = report.student.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Matricule : ${report.student.matricule}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Blue600,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Classe : ${report.student.className} • Cycle : ${if (isPrimary) "Primaire" else "Secondaire"} • Né(e) le : ${report.student.birthDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate600
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = report.period,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "Année 2025-2026",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                        Text(
                            text = "Assiduité : ${report.attendanceSummary}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Emerald600,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subjects and Grades Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Slate800)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discipline / Matière",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(2.0f)
                )
                Text(
                    text = "Coef",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.6f)
                )
                Text(
                    text = "Note",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    text = "Moy. Cl.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    text = "Rang",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.7f)
                )
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    text = if (isPrimary) "Appréciation du Maître" else "Appréciation",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(2.0f)
                )
            }

            // Grades Rows
            report.subjects.forEachIndexed { idx, sub ->
                val bg = if (idx % 2 == 0) Color.White else Slate50
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg)
                        .padding(horizontal = 6.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sub.subjectName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(2.0f)
                    )
                    Text(
                        text = "${sub.coefficient}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600,
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(0.6f)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f", sub.average),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (sub.average >= 10f) Blue700 else Rose600,
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f", sub.classAverage),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        text = sub.subjectRank,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Amber600,
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(0.7f)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f", sub.weightedTotal),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700,
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        text = sub.teacherAppreciation,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(2.0f)
                    )
                }
                Divider(color = Slate100, thickness = 0.5.dp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Overall Summary Box (Moyenne élève, Moyenne classe, Rang, Décision)
            Surface(
                color = Slate900,
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Moyenne Élève", color = Slate400, fontSize = 10.sp)
                        Text(
                            text = "${String.format(Locale.US, "%.2f", report.overallAverage)} / 20",
                            color = if (report.overallAverage >= 10f) Emerald400 else Rose400,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Points : ${String.format(Locale.US, "%.1f", report.totalPoints)} / ${report.totalCoefficients * 20}",
                            color = Slate400,
                            fontSize = 10.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Moyenne Classe", color = Slate400, fontSize = 10.sp)
                        Text(
                            text = "${String.format(Locale.US, "%.2f", report.classAverage)} / 20",
                            color = Blue400,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rang : ${report.rankInClass}${if (report.rankInClass == 1) "er" else "ème"} / ${report.totalClassSize}",
                            color = Amber400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = if (isPrimary) "Mention & Décision" else "Avis du Conseil", color = Slate400, fontSize = 10.sp)
                        Text(
                            text = report.decision,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        )
                        if (isPrimary && report.student.className.startsWith("CM2")) {
                            Text(
                                text = "Candidat Examen CEP 2026",
                                color = Emerald400,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Verification & QR Code Security Zone
            Surface(
                color = Slate50,
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // QR Code View
                    QrCodeView(
                        data = report.qrPayload,
                        size = 80.dp,
                        modifier = Modifier.clickable(onClick = onVerifyQrClick)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Authentification Cryptographique",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                        Text(
                            text = "Signature numérique anti-fraude générée par EduFinance Pro.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                            fontSize = 10.sp
                        )
                        Text(
                            text = report.verificationHash,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Blue600,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        FilledTonalButton(
                            onClick = onVerifyQrClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Vérifier QR Code", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradesEntryTab(
    students: List<Student>,
    subjects: List<Subject>,
    grades: List<GradeRecord>,
    currentUser: UserSession,
    onOpenNewGrade: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Registre des Notes & Évaluations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${grades.size} notes saisies au Trimestre 1",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }

                    if (currentUser.role.canInputGrades) {
                        Button(
                            onClick = onOpenNewGrade,
                            colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Saisir Note", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        items(grades) { g ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = g.studentName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${g.subjectName} (Coeff ${g.coefficient}) • ${g.className}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                        Text(
                            text = "Devoir 1: ${g.test1}/20 | Devoir 2: ${g.test2}/20 | Examen: ${g.exam}/20",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate600
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", g.average)} / 20",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (g.average >= 10f) Blue700 else Rose600
                        )
                        Text(
                            text = "Moy. pondérée",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceSheetTab(
    students: List<Student>,
    classes: List<SchoolClass>,
    attendance: List<AttendanceRecord>,
    currentUser: UserSession,
    onUpdateAttendance: (String, AttendanceStatus, String) -> Unit
) {
    var selectedClass by remember {
        mutableStateOf(classes.firstOrNull()?.name ?: "Terminale S1")
    }

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val classStudents = students.filter { it.className == selectedClass }

    // Computed attendance stats for class today
    val classAttendanceToday = attendance.filter { it.className == selectedClass && it.date == today }
    val presentCount = classAttendanceToday.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = classAttendanceToday.count { it.status == AttendanceStatus.ABSENT_JUSTIFIE || it.status == AttendanceStatus.ABSENT_NON_JUSTIFIE }
    val lateCount = classAttendanceToday.count { it.status == AttendanceStatus.RETARD }
    val totalChecked = classAttendanceToday.size
    val rate = if (totalChecked > 0) (presentCount.toFloat() / totalChecked.toFloat() * 100) else 90f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Fiche d'Appel Quotidienne",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Date : $today • Année Scolaire 2025-2026",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Emerald100)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", rate)} % Présence",
                                color = Emerald700,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Class selector chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(classes) { cl ->
                            FilterChip(
                                selected = selectedClass == cl.name,
                                onClick = { selectedClass = cl.name },
                                label = { Text(cl.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(text = "Présents : $presentCount", color = Emerald600, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Absents : $absentCount", color = Rose600, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Retards : $lateCount", color = Amber500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(classStudents) { student ->
            val record = attendance.find { it.studentId == student.id && it.date == today }
            val currentStatus = record?.status ?: AttendanceStatus.PRESENT

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = student.fullName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = student.matricule,
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500
                            )
                        }

                        AttendanceStatusPill(status = currentStatus)
                    }

                    if (currentUser.role.canTakeAttendance) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AttendanceOptionButton(
                                label = "Présent",
                                isSelected = currentStatus == AttendanceStatus.PRESENT,
                                activeColor = Emerald600,
                                modifier = Modifier.weight(1f),
                                onClick = { onUpdateAttendance(student.id, AttendanceStatus.PRESENT, "") }
                            )
                            AttendanceOptionButton(
                                label = "Retard",
                                isSelected = currentStatus == AttendanceStatus.RETARD,
                                activeColor = Amber500,
                                modifier = Modifier.weight(1f),
                                onClick = { onUpdateAttendance(student.id, AttendanceStatus.RETARD, "Retard matinal") }
                            )
                            AttendanceOptionButton(
                                label = "Abs. Justifiée",
                                isSelected = currentStatus == AttendanceStatus.ABSENT_JUSTIFIE,
                                activeColor = Blue600,
                                modifier = Modifier.weight(1.2f),
                                onClick = { onUpdateAttendance(student.id, AttendanceStatus.ABSENT_JUSTIFIE, "Motif fourni") }
                            )
                            AttendanceOptionButton(
                                label = "Abs. Non Justifiée",
                                isSelected = currentStatus == AttendanceStatus.ABSENT_NON_JUSTIFIE,
                                activeColor = Rose600,
                                modifier = Modifier.weight(1.3f),
                                onClick = { onUpdateAttendance(student.id, AttendanceStatus.ABSENT_NON_JUSTIFIE, "Non justifié") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceOptionButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor else Slate100,
        contentColor = if (isSelected) Color.White else Slate700
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
