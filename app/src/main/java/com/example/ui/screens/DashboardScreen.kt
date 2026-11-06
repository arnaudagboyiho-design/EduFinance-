package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EduFinanceRepository
import com.example.model.Transaction
import com.example.model.TransactionType
import com.example.model.UserRole
import com.example.model.UserSession
import com.example.ui.AppScreen
import com.example.ui.DashboardKpis
import com.example.ui.components.CashflowBarComparison
import com.example.ui.components.StatKpiCard
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    kpis: DashboardKpis,
    currentUser: UserSession,
    recentTransactions: List<Transaction>,
    onNavigate: (AppScreen) -> Unit,
    onOpenNewStudent: () -> Unit,
    onOpenNewPayment: () -> Unit,
    onOpenNewExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Bonjour, ${currentUser.name}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Espace ${currentUser.role.title} • Établissement Scolaire Excellence",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate400
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Blue600),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentUser.role.canEditStudents) {
                            Button(
                                onClick = onOpenNewStudent,
                                modifier = Modifier.weight(1f).testTag("quick_add_student_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Élève", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (currentUser.role.canManageFinances) {
                            Button(
                                onClick = onOpenNewPayment,
                                modifier = Modifier.weight(1f).testTag("quick_add_payment_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Encaisser", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = onOpenNewExpense,
                                modifier = Modifier.weight(1f).testTag("quick_add_expense_button"),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Slate800,
                                    contentColor = Rose400
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Dépense", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section KPIs
        item {
            Text(
                text = "Indicateurs Clés (BI & Performance)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Row of 2 KPI cards: Solde net & Recouvrement
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatKpiCard(
                    title = "Solde Net Trésorerie",
                    value = EduFinanceRepository.formatCurrency(kpis.netBalance),
                    subtitle = "Recettes - Dépenses",
                    icon = Icons.Default.AccountBalance,
                    iconTint = if (kpis.netBalance >= 0) Emerald600 else Rose600,
                    iconBgColor = if (kpis.netBalance >= 0) Emerald100 else Rose100,
                    modifier = Modifier.weight(1f),
                    badgeText = if (kpis.netBalance >= 0) "Positif" else "Déficit",
                    badgeColor = if (kpis.netBalance >= 0) Emerald500 else Rose500
                )

                StatKpiCard(
                    title = "Recouvrement Scolarité",
                    value = "${String.format(java.util.Locale.US, "%.1f", kpis.recoveryRate)} %",
                    subtitle = "Taux d'encaissement",
                    icon = Icons.Default.PieChart,
                    iconTint = Blue600,
                    iconBgColor = Blue100,
                    modifier = Modifier.weight(1f),
                    badgeText = "Scolarités",
                    badgeColor = Blue600
                )
            }
        }

        // Row of 2 KPI cards: Élèves actifs & Présence
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatKpiCard(
                    title = "Élèves Inscrits",
                    value = "${kpis.activeStudents}",
                    subtitle = "Répartis en ${kpis.totalClasses} classes",
                    icon = Icons.Default.School,
                    iconTint = Indigo500,
                    iconBgColor = Violet100,
                    modifier = Modifier.weight(1f)
                )

                StatKpiCard(
                    title = "Taux de Présence",
                    value = "${String.format(java.util.Locale.US, "%.1f", kpis.todayAttendanceRate)} %",
                    subtitle = "Fiche d'appel du jour",
                    icon = Icons.Default.HowToReg,
                    iconTint = Emerald600,
                    iconBgColor = Emerald100,
                    modifier = Modifier.weight(1f),
                    badgeText = "Aujourd'hui",
                    badgeColor = Emerald500
                )
            }
        }

        // Cashflow Chart Comparison
        item {
            CashflowBarComparison(
                revenue = kpis.totalRevenue,
                expenses = kpis.totalExpenses,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Recent Activity Feed (Transactions & Events)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Flux d'Activité Récente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = { onNavigate(AppScreen.FINANCE) }
                ) {
                    Text("Voir Tout", color = Blue600, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucune activité récente enregistrée.", color = Slate400)
                    }
                }
            }
        } else {
            items(recentTransactions.take(5)) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isIncome = tx.type == TransactionType.ENCAISSEMENT
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isIncome) Emerald100 else Rose100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isIncome) Emerald600 else Rose600,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (tx.studentName != null) "${tx.studentName} • ${tx.paymentMethod.label}" else "${tx.payeeOrPayer} • ${tx.paymentMethod.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = (if (isIncome) "+ " else "- ") + EduFinanceRepository.formatCurrency(tx.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isIncome) Emerald600 else Rose600
                            )
                            Text(
                                text = tx.date,
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
}
