package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EduFinanceRepository
import com.example.model.*
import com.example.ui.components.StudentStatusPill
import com.example.ui.theme.*

@Composable
fun AcademicScreen(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    students: List<Student>,
    classes: List<SchoolClass>,
    teachers: List<Teacher>,
    subjects: List<Subject>,
    currentUser: UserSession,
    onOpenNewStudent: () -> Unit,
    onViewReportCard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCycleFilter by remember { mutableStateOf<SchoolCycle?>(null) }
    var selectedClassFilter by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("academic_screen")
    ) {
        // Tab selector
        TabRow(
            selectedTabIndex = currentTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Blue600,
            modifier = Modifier.testTag("academic_tabs")
        ) {
            val tabs = listOf("Élèves (${students.size})", "Classes (${classes.size})", "Enseignants (${teachers.size})", "Matières (${subjects.size})")
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
                // Students Tab
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search & Filter header
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("student_search_input"),
                                    placeholder = { Text("Rechercher élève, matricule, parent...", fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = Slate400)
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Effacer")
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                if (currentUser.role.canEditStudents) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledIconButton(
                                        onClick = onOpenNewStudent,
                                        modifier = Modifier.testTag("add_student_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Blue600)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Ajouter Élève", tint = Color.White)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Cycle filter chips (Primaire, Collège, Lycée)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    FilterChip(
                                        selected = selectedCycleFilter == null,
                                        onClick = {
                                            selectedCycleFilter = null
                                            selectedClassFilter = null
                                        },
                                        label = { Text("Tous les cycles", fontSize = 11.sp, fontWeight = if (selectedCycleFilter == null) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                                items(SchoolCycle.values()) { cycle ->
                                    val isSelected = selectedCycleFilter == cycle
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedCycleFilter = if (isSelected) null else cycle
                                            selectedClassFilter = null
                                        },
                                        label = { Text(cycle.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Class filter chips (filtered by selected cycle)
                            val visibleClasses = if (selectedCycleFilter == null) classes else classes.filter { it.cycle == selectedCycleFilter }
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedClassFilter == null,
                                        onClick = { selectedClassFilter = null },
                                        label = { Text("Toutes les classes", fontSize = 11.sp) }
                                    )
                                }
                                items(visibleClasses) { cl ->
                                    FilterChip(
                                        selected = selectedClassFilter == cl.name,
                                        onClick = {
                                            selectedClassFilter = if (selectedClassFilter == cl.name) null else cl.name
                                        },
                                        label = { Text(cl.name, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // Filtered students list
                    val filteredStudents = students.filter { s ->
                        val studentClass = classes.find { it.name == s.className }
                        val matchesCycle = selectedCycleFilter == null || studentClass?.cycle == selectedCycleFilter
                        val matchesSearch = searchQuery.isBlank() ||
                                s.fullName.contains(searchQuery, ignoreCase = true) ||
                                s.matricule.contains(searchQuery, ignoreCase = true) ||
                                s.parentName.contains(searchQuery, ignoreCase = true)
                        val matchesClass = selectedClassFilter == null || s.className == selectedClassFilter
                        matchesCycle && matchesSearch && matchesClass
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (filteredStudents.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Aucun élève ne correspond aux critères.", color = Slate400)
                                }
                            }
                        } else {
                            items(filteredStudents) { student ->
                                StudentCard(
                                    student = student,
                                    onViewReportCard = { onViewReportCard(student.id) }
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // Classes Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(classes) { cl ->
                        ClassCard(schoolClass = cl)
                    }
                }
            }

            2 -> {
                // Teachers Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(teachers) { teacher ->
                        TeacherCard(teacher = teacher)
                    }
                }
            }

            3 -> {
                // Subjects Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(subjects) { subject ->
                        SubjectCard(subject = subject)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCard(
    student: Student,
    onViewReportCard: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_card_${student.matricule}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (student.gender == "F") Rose100 else Blue100),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${student.lastName.take(1)}${student.firstName.take(1)}",
                            fontWeight = FontWeight.Bold,
                            color = if (student.gender == "F") Rose600 else Blue600,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${student.matricule} • ${student.className}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }

                StudentStatusPill(status = student.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Slate200, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Parent & Financial status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Parent : ${student.parentName}", style = MaterialTheme.typography.labelSmall, color = Slate600)
                    Text(text = "Tél : ${student.parentPhone}", style = MaterialTheme.typography.labelSmall, color = Slate400)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Payé : ${EduFinanceRepository.formatCurrency(student.tuitionPaid)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (student.isFullyPaid) Emerald600 else Amber500
                    )
                    if (!student.isFullyPaid) {
                        Text(
                            text = "Reste : ${EduFinanceRepository.formatCurrency(student.tuitionBalance)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Rose600,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action button: Bulletin de notes
            OutlinedButton(
                onClick = onViewReportCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_bulletin_${student.matricule}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue600)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Générer & Consulter Bulletin (avec QR)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ClassCard(schoolClass: SchoolClass) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = schoolClass.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${schoolClass.level} (${schoolClass.cycle.shortName}) • ${schoolClass.roomNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Blue50)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${schoolClass.studentCount} / ${schoolClass.capacity} élèves",
                        color = Blue600,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val fillRatio = (schoolClass.studentCount.toFloat() / schoolClass.capacity.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { fillRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (fillRatio > 0.9f) Amber500 else Blue600,
                trackColor = Slate100
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Professeur Principal : ${schoolClass.mainTeacherName}",
                style = MaterialTheme.typography.labelSmall,
                color = Slate600
            )
        }
    }
}

@Composable
fun TeacherCard(teacher: Teacher) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = teacher.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = teacher.subjects.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Blue600,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Emerald100)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = teacher.status,
                        color = Emerald700,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Slate200, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Tél : ${teacher.phone}", style = MaterialTheme.typography.labelSmall, color = Slate600)
                    Text(text = "Email : ${teacher.email}", style = MaterialTheme.typography.labelSmall, color = Slate400)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Charge salariale :",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                    Text(
                        text = EduFinanceRepository.formatCurrency(teacher.monthlySalary),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectCard(subject: Subject) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Classe : ${subject.className} • Enseignant : ${subject.teacherName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate900)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Coeff ${subject.coefficient}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
    }
}
