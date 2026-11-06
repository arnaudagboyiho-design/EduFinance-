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
import com.example.model.Transaction
import com.example.model.TransactionType
import com.example.model.UserSession
import com.example.ui.components.CashflowBarComparison
import com.example.ui.theme.*

@Composable
fun FinanceScreen(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    transactions: List<Transaction>,
    currentUser: UserSession,
    onOpenNewPayment: () -> Unit,
    onOpenNewExpense: () -> Unit,
    onExportReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var typeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val totalIncome = transactions.filter { it.type == TransactionType.ENCAISSEMENT }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.DEPENSE }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    val filteredTransactions = transactions.filter { tx ->
        val matchesType = typeFilter == null || tx.type == typeFilter
        val matchesSearch = searchQuery.isBlank() ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.referenceNumber.contains(searchQuery, ignoreCase = true) ||
                tx.payeeOrPayer.contains(searchQuery, ignoreCase = true) ||
                (tx.studentName?.contains(searchQuery, ignoreCase = true) == true)
        matchesType && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("finance_screen")
    ) {
        TabRow(
            selectedTabIndex = currentTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Emerald600,
            modifier = Modifier.testTag("finance_tabs")
        ) {
            val tabs = listOf("Journal de Caisse (${transactions.size})", "Grand Livre & Bilan")
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
                // Journal de caisse
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Action & Summary Banner
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Slate900)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Trésorerie & Journal de Caisse", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(text = "Rapprochement chronologique recettes & dépenses", color = Slate400, fontSize = 11.sp)
                                    }

                                    IconButton(
                                        onClick = onExportReport,
                                        modifier = Modifier.testTag("export_finance_button")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Exporter Bilan", tint = Emerald400)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "Recettes (Crédit)", color = Slate400, fontSize = 10.sp)
                                        Text(
                                            text = "+ " + EduFinanceRepository.formatCurrency(totalIncome),
                                            color = Emerald400,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Column {
                                        Text(text = "Dépenses (Débit)", color = Slate400, fontSize = 10.sp)
                                        Text(
                                            text = "- " + EduFinanceRepository.formatCurrency(totalExpense),
                                            color = Rose400,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "Solde Net", color = Slate400, fontSize = 10.sp)
                                        Text(
                                            text = EduFinanceRepository.formatCurrency(netBalance),
                                            color = if (netBalance >= 0) Color.White else Rose400,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                if (currentUser.role.canManageFinances) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = onOpenNewPayment,
                                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Encaissement", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        FilledTonalButton(
                                            onClick = onOpenNewExpense,
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = Slate800,
                                                contentColor = Rose400
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Dépense", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Search and Filter controls
                    item {
                        Column {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Rechercher motif, référence, tiers...", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = typeFilter == null,
                                    onClick = { typeFilter = null },
                                    label = { Text("Tous les flux", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = typeFilter == TransactionType.ENCAISSEMENT,
                                    onClick = { typeFilter = if (typeFilter == TransactionType.ENCAISSEMENT) null else TransactionType.ENCAISSEMENT },
                                    label = { Text("Recettes uniquement", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = typeFilter == TransactionType.DEPENSE,
                                    onClick = { typeFilter = if (typeFilter == TransactionType.DEPENSE) null else TransactionType.DEPENSE },
                                    label = { Text("Dépenses", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Transaction items list
                    if (filteredTransactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Aucun mouvement financier trouvé.", color = Slate400)
                            }
                        }
                    } else {
                        items(filteredTransactions) { tx ->
                            TransactionRowCard(tx = tx)
                        }
                    }
                }
            }

            1 -> {
                // Grand Livre & Bilan Financier
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        CashflowBarComparison(
                            revenue = totalIncome,
                            expenses = totalExpense,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Ventilation des Postes de Dépenses",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val expensesByCategory = transactions
                                    .filter { it.type == TransactionType.DEPENSE }
                                    .groupBy { it.category }

                                expensesByCategory.forEach { (cat, list) ->
                                    val sum = list.sumOf { it.amount }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = cat, style = MaterialTheme.typography.bodyMedium, color = Slate700)
                                        Text(
                                            text = EduFinanceRepository.formatCurrency(sum),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Rose600
                                        )
                                    }
                                    Divider(color = Slate100, thickness = 0.5.dp)
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = onExportReport,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aperçu & Impression du Rapport Financier Complet", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowCard(tx: Transaction) {
    val isIncome = tx.type == TransactionType.ENCAISSEMENT

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isIncome) Emerald100 else Rose100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.AddCircle else Icons.Default.RemoveCircle,
                    contentDescription = null,
                    tint = if (isIncome) Emerald600 else Rose600,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tx.category,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${tx.referenceNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = if (tx.studentName != null) "Élève: ${tx.studentName}" else "Bénéficiaire: ${tx.payeeOrPayer}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate600
                )

                if (tx.notes.isNotBlank()) {
                    Text(
                        text = tx.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontSize = 10.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isIncome) "+ " else "- ") + EduFinanceRepository.formatCurrency(tx.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isIncome) Emerald600 else Rose600
                )
                Text(
                    text = "${tx.date} • ${tx.paymentMethod.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontSize = 10.sp
                )
            }
        }
    }
}
