package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.model.UserSession
import com.example.ui.components.RoleBadge
import com.example.ui.theme.*

@Composable
fun SecurityScreen(
    currentUser: UserSession,
    availableUsers: List<UserSession>,
    onSwitchUser: (String) -> Unit,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("security_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active User Session Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Session Utilisateur Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Gestion des rôles et contrôle d'accès RBAC",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Blue600),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Slate800,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser.avatarInitials,
                                    color = Slate950,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = currentUser.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                RoleBadge(role = currentUser.role)
                            }
                        }
                    }

                    if (onLogout != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lock_session_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Rose400
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Rose500.copy(alpha = 0.5f))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verrouiller la Session & Se Déconnecter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Granular Role Switcher selector
        item {
            Text(
                text = "Basculer de Rôle & Profil de Test",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Simulez instantanément les différents niveaux d'accès de l'établissement :",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
            )
        }

        items(availableUsers) { user ->
            val isCurrent = user.id == currentUser.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSwitchUser(user.id) }
                    .border(
                        width = if (isCurrent) 2.dp else 1.dp,
                        color = if (isCurrent) Blue600 else Slate200,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) Blue50 else MaterialTheme.colorScheme.surface
                )
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
                            .background(if (isCurrent) Blue600 else Slate200),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.avatarInitials,
                            color = if (isCurrent) Color.White else Slate700,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ACTIF",
                                    color = Blue600,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Text(
                            text = user.role.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }

                    RoleBadge(role = user.role)
                }
            }
        }

        // Granular Permissions Matrix Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Matrice des Habilitations (RBAC)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionItem("Gestion Trésorerie & Encaissements", currentUser.role.canManageFinances)
                    PermissionItem("Inscription & Édition des Élèves", currentUser.role.canEditStudents)
                    PermissionItem("Saisie & Modification des Notes", currentUser.role.canInputGrades)
                    PermissionItem("Émargement des Présences du Jour", currentUser.role.canTakeAttendance)
                    PermissionItem("Génération des Bulletins & Bilans", currentUser.role.canViewFullReports)
                }
            }
        }

        // Row Level Security (RLS) & Offline Resilience Architecture Card
        item {
            var showFullSchema by remember { mutableStateOf(false) }
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            var copied by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("supabase_sql_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = Emerald400, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Schéma SQL Supabase & RLS (Cycle Primaire)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tables CI, CP, CE1, CE2, CM1, CM2 & Politiques RLS",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(com.example.data.SupabaseSchema.PRIMARY_CYCLE_SQL))
                                copied = true
                            }
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copier le script SQL",
                                tint = if (copied) Emerald400 else Slate300,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Script DDL PostgreSQL complet prêt pour l'éditeur SQL de Supabase (énumérations des cycles, tables 'classes', 'subjects', 'students', 'grades', coefficients primaires et contraintes RLS) :",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate300,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Slate950,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (showFullSchema) {
                                    com.example.data.SupabaseSchema.PRIMARY_CYCLE_SQL
                                } else {
                                    "-- Extrait DDL Supabase (Cycle Primaire CI - CM2)\n" +
                                    "CREATE TYPE school_cycle_enum AS ENUM ('primaire', 'college', 'lycee');\n" +
                                    "ALTER TABLE classes ADD COLUMN cycle school_cycle_enum DEFAULT 'primaire';\n" +
                                    "ALTER TABLE subjects ADD COLUMN cycle school_cycle_enum DEFAULT 'primaire';\n" +
                                    "-- RLS Policy sécurisée :\n" +
                                    "CREATE POLICY \"teachers_grades_primary\" ON grades\n" +
                                    "  FOR INSERT WITH CHECK (auth.uid() = teacher_id);\n" +
                                    "..."
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Emerald400,
                                lineHeight = 14.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = { showFullSchema = !showFullSchema },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (showFullSchema) "Réduire le script SQL ▲" else "Afficher tout le script SQL (${com.example.data.SupabaseSchema.PRIMARY_CYCLE_SQL.lines().size} lignes) ▼",
                                    fontSize = 10.sp,
                                    color = Blue400,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Blue400, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (copied) "Copié dans le presse-papiers !" else "Mode Hybride : Compatible stockage local autonome & Cloud Supabase.",
                            color = if (copied) Emerald400 else Blue100,
                            fontSize = 11.sp,
                            fontWeight = if (copied) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionItem(name: String, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = if (granted) Slate900 else Slate400
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (granted) Emerald100 else Rose100)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (granted) "Autorisé" else "Refusé",
                color = if (granted) Emerald700 else Rose600,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Divider(color = Slate100, thickness = 0.5.dp)
}
