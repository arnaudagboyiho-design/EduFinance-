package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceStatus
import com.example.model.StudentStatus
import com.example.model.TransactionType
import com.example.model.UserRole
import com.example.ui.theme.*

@Composable
fun StatKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeColor: Color = Emerald500
) {
    Card(
        modifier = modifier.testTag("kpi_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RoleBadge(
    role: UserRole,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (role) {
        UserRole.ADMIN -> Blue100 to Blue700
        UserRole.DIRECTEUR -> Violet100 to Violet600
        UserRole.COMPTABLE -> Emerald100 to Emerald700
        UserRole.SECRETAIRE -> Amber100 to Amber500
        UserRole.ENSEIGNANT -> Slate200 to Slate800
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = role.title,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StudentStatusPill(status: StudentStatus) {
    val (bg, fg) = when (status) {
        StudentStatus.EN_REGLE -> Emerald100 to Emerald700
        StudentStatus.ACTIF -> Blue100 to Blue700
        StudentStatus.EN_RETARD -> Rose100 to Rose600
        StudentStatus.ARCHIVE -> Slate200 to Slate600
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AttendanceStatusPill(status: AttendanceStatus, onClick: (() -> Unit)? = null) {
    val (bg, fg) = when (status) {
        AttendanceStatus.PRESENT -> Emerald100 to Emerald700
        AttendanceStatus.ABSENT_JUSTIFIE -> Blue100 to Blue700
        AttendanceStatus.ABSENT_NON_JUSTIFIE -> Rose100 to Rose600
        AttendanceStatus.RETARD -> Amber100 to Amber500
    }

    val mod = if (onClick != null) {
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    } else {
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    }

    Box(modifier = mod) {
        Text(
            text = status.label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CashflowBarComparison(
    revenue: Double,
    expenses: Double,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(revenue, expenses, 1.0)
    val revPercent = (revenue / maxVal).toFloat().coerceIn(0.05f, 1f)
    val expPercent = (expenses / maxVal).toFloat().coerceIn(0.05f, 1f)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aperçu Trésorerie (Flux Net)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ce Trimestre",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Revenue bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recettes (Encaissements)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                    Text(
                        text = com.example.data.EduFinanceRepository.formatCurrency(revenue),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Emerald600
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Slate100)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(revPercent)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Emerald500)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Expenses bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Dépenses (Décaissements)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                    Text(
                        text = com.example.data.EduFinanceRepository.formatCurrency(expenses),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Rose600
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Slate100)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(expPercent)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Rose500)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Net balance badge
            val net = revenue - expenses
            val isPositive = net >= 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isPositive) Emerald50 else Rose100)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Solde Net Disponible :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPositive) Emerald700 else Rose600
                )
                Text(
                    text = com.example.data.EduFinanceRepository.formatCurrency(net),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isPositive) Emerald700 else Rose600
                )
            }
        }
    }
}
