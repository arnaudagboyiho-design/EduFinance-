package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.EduFinanceRepository
import com.example.model.*
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun NewStudentDialog(
    classes: List<SchoolClass>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, String, String, Double) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") }
    var birthDate by remember { mutableStateOf("2008-05-15") }
    var selectedClass by remember { mutableStateOf(classes.firstOrNull()?.name ?: "Terminale S1") }
    var parentName by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var parentEmail by remember { mutableStateOf("") }
    var tuitionTotalStr by remember { mutableStateOf("450000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Inscription d'un Nouvel Élève",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Nom de famille *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("student_lastname_input")
                )
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Prénom(s) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("student_firstname_input")
                )

                // Gender radio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Sexe :", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 12.dp))
                    FilterChip(
                        selected = gender == "M",
                        onClick = { gender = "M" },
                        label = { Text("Masculin (M)") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = gender == "F",
                        onClick = { gender = "F" },
                        label = { Text("Féminin (F)") }
                    )
                }

                // Class selector organized by cycle (CI, CP, CE1, CE2, CM1, CM2...)
                Text(text = "Cycle & Classe attribuée :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate700)
                
                Text(text = "Cycle Primaire :", style = MaterialTheme.typography.labelSmall, color = Emerald600, fontSize = 10.sp)
                val primaryClasses = classes.filter { it.cycle == SchoolCycle.PRIMAIRE }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(primaryClasses) { cl ->
                        FilterChip(
                            selected = selectedClass == cl.name,
                            onClick = { 
                                selectedClass = cl.name
                                tuitionTotalStr = "250000"
                            },
                            label = { Text("${cl.name} (${cl.studentCount}/${cl.capacity})", fontSize = 11.sp) }
                        )
                    }
                }

                Text(text = "Secondaire (Collège & Lycée) :", style = MaterialTheme.typography.labelSmall, color = Blue600, fontSize = 10.sp)
                val secondaryClasses = classes.filter { it.cycle != SchoolCycle.PRIMAIRE }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(secondaryClasses) { cl ->
                        FilterChip(
                            selected = selectedClass == cl.name,
                            onClick = { 
                                selectedClass = cl.name
                                tuitionTotalStr = "450000"
                            },
                            label = { Text("${cl.name} (${cl.studentCount}/${cl.capacity})", fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = parentName,
                    onValueChange = { parentName = it },
                    label = { Text("Nom du tuteur / parent *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = parentPhone,
                    onValueChange = { parentPhone = it },
                    label = { Text("Téléphone parent *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = parentEmail,
                    onValueChange = { parentEmail = it },
                    label = { Text("Email parent") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tuitionTotalStr,
                    onValueChange = { tuitionTotalStr = it },
                    label = { Text("Frais de scolarité annuels (FCFA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = tuitionTotalStr.toDoubleOrNull() ?: 450000.0
                    if (lastName.isNotBlank() && firstName.isNotBlank() && parentName.isNotBlank()) {
                        onConfirm(firstName, lastName, gender, birthDate, selectedClass, parentName, parentPhone, parentEmail, amount)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                modifier = Modifier.testTag("confirm_add_student")
            ) {
                Text("Enregistrer l'inscription")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun NewPaymentDialog(
    students: List<Student>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, PaymentMethod, String) -> Unit
) {
    var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
    var category by remember { mutableStateOf("Scolarité") }
    var amountStr by remember { mutableStateOf("150000") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.ESPECES) }
    var notes by remember { mutableStateOf("Versement scolarité") }

    val categories = listOf("Scolarité", "Cantine Scolaire", "Transport Scolaire", "Tenue Scolaire & Fournitures")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Encaisser un Paiement (Reçu)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Élève bénéficiaire :", style = MaterialTheme.typography.labelSmall, color = Slate600)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    students.take(4).forEach { st ->
                        val isSelected = st.id == selectedStudentId
                        Surface(
                            onClick = { selectedStudentId = st.id },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Blue50 else Slate50,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) Blue600 else Slate200)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSelected, onClick = { selectedStudentId = st.id })
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(text = st.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "${st.matricule} • ${st.className}", style = MaterialTheme.typography.labelSmall, color = Slate500)
                                }
                            }
                        }
                    }
                }

                Text(text = "Motif d'encaissement :", style = MaterialTheme.typography.labelSmall, color = Slate600)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.take(2).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Montant en FCFA *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )

                Text(text = "Mode de règlement :", style = MaterialTheme.typography.labelSmall, color = Slate600)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(PaymentMethod.ESPECES, PaymentMethod.MOBILE_MONEY, PaymentMethod.VIREMENT).forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.label, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Référence de reçu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && selectedStudentId.isNotBlank()) {
                        onConfirm(selectedStudentId, category, amount, selectedMethod, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text("Valider & Émettre Reçu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun NewExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String, PaymentMethod) -> Unit
) {
    var category by remember { mutableStateOf("Fournitures & Matériel") }
    var description by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("75000") }
    var payee by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PaymentMethod.ESPECES) }

    val categories = listOf("Salaires Enseignants", "Fournitures & Matériel", "Électricité & Eau", "Maintenance Bâtiment", "Autre")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Enregistrer une Dépense (Décaissement)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Poste de dépense :", style = MaterialTheme.typography.labelSmall, color = Slate600)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Justificatif *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Montant décaissé (FCFA) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = payee,
                    onValueChange = { payee = it },
                    label = { Text("Bénéficiaire / Prestataire *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(PaymentMethod.ESPECES, PaymentMethod.VIREMENT, PaymentMethod.CHEQUE).forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m.label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && description.isNotBlank() && payee.isNotBlank()) {
                        onConfirm(category, description, amount, payee, method)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Rose600)
            ) {
                Text("Enregistrer la dépense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun NewGradeDialog(
    students: List<Student>,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String, Float, Float, Float) -> Unit
) {
    var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
    var selectedSubjectName by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "Mathématiques") }
    var period by remember { mutableStateOf("Trimestre 1") }
    var test1Str by remember { mutableStateOf("14.5") }
    var test2Str by remember { mutableStateOf("15.0") }
    var examStr by remember { mutableStateOf("16.0") }

    val selectedSubject = subjects.find { it.name == selectedSubjectName }
    val coeff = selectedSubject?.coefficient ?: 4

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saisir des Notes d'Évaluation", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Élève :", style = MaterialTheme.typography.labelSmall, color = Slate600)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    students.take(3).forEach { st ->
                        val isSelected = st.id == selectedStudentId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStudentId = st.id },
                            label = { Text(st.fullName, fontSize = 11.sp) }
                        )
                    }
                }

                Text(text = "Matière :", style = MaterialTheme.typography.labelSmall, color = Slate600)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    subjects.take(3).forEach { sub ->
                        FilterChip(
                            selected = selectedSubjectName == sub.name,
                            onClick = { selectedSubjectName = sub.name },
                            label = { Text(sub.name, fontSize = 11.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = test1Str,
                        onValueChange = { test1Str = it },
                        label = { Text("Devoir 1 /20") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = test2Str,
                        onValueChange = { test2Str = it },
                        label = { Text("Devoir 2 /20") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = examStr,
                    onValueChange = { examStr = it },
                    label = { Text("Examen / Composition /20") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Preview average
                val t1 = test1Str.toFloatOrNull() ?: 0f
                val t2 = test2Str.toFloatOrNull() ?: 0f
                val ex = examStr.toFloatOrNull() ?: 0f
                val computedAvg = (t1 + t2 + 2f * ex) / 4f

                Surface(
                    color = Blue50,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Moyenne calculée (Coeff $coeff) :", color = Slate700, fontSize = 11.sp)
                        Text(
                            text = "${String.format(Locale.US, "%.2f", computedAvg)} / 20",
                            color = Blue700,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val t1 = test1Str.toFloatOrNull() ?: 0f
                    val t2 = test2Str.toFloatOrNull() ?: 0f
                    val ex = examStr.toFloatOrNull() ?: 0f
                    onConfirm(selectedStudentId, selectedSubjectName, coeff, period, t1, t2, ex)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Blue600)
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun QrVerificationModal(
    report: ReportCard,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Emerald100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Emerald600,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Certificat d'Authenticité Numérique",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Ce bulletin scolaire a été vérifié cryptographiquement par le système EduFinance Pro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code
                QrCodeView(
                    data = report.qrPayload,
                    size = 140.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = Slate50,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CertRow("Élève :", report.student.fullName)
                        CertRow("Matricule :", report.student.matricule)
                        CertRow("Classe :", report.student.className)
                        CertRow("Moyenne générale :", "${String.format(Locale.US, "%.2f", report.overallAverage)} / 20")
                        CertRow("Rang officiel :", "${report.rankInClass} / ${report.totalClassSize}")
                        CertRow("Année Scolaire :", "2025-2026")
                        CertRow("Signature SHA :", report.verificationHash)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Fermer la vérification")
                }
            }
        }
    }
}

@Composable
private fun CertRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate500)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate900)
    }
}

@Composable
fun RoleSwitcherModal(
    currentUser: UserSession,
    availableUsers: List<UserSession>,
    onSelectUser: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Changer de Profil & Rôle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sélectionnez un compte pour tester ses habilitations et son interface dédiée :",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )

                Spacer(modifier = Modifier.height(14.dp))

                availableUsers.forEach { user ->
                    val isSelected = user.id == currentUser.id
                    Surface(
                        onClick = { onSelectUser(user.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Blue50 else Slate50,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) Blue600 else Slate200)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Blue600 else Slate300),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.avatarInitials,
                                    color = if (isSelected) Color.White else Slate800,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = user.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(text = user.role.title, style = MaterialTheme.typography.labelSmall, color = Slate500)
                            }
                            RoleBadge(role = user.role)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Annuler")
                }
            }
        }
    }
}
