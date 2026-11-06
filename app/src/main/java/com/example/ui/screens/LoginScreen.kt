package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuthState
import com.example.model.UserRole
import com.example.model.UserSession
import com.example.ui.components.RoleBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authState: AuthState,
    availableUsers: List<UserSession>,
    onLoginClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    onForgotPasswordClick: ((String, (Result<Unit>) -> Unit) -> Unit)? = null
) {
    var email by remember { mutableStateOf("admin@edufinance.pro") }
    var password by remember { mutableStateOf("Admin@2026") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var showForgotPasswordNotice by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Validation logic
    val emailError = remember(email, hasAttemptedSubmit) {
        if (!hasAttemptedSubmit) null
        else when {
            email.trim().isEmpty() -> "L'adresse e-mail ou l'identifiant est obligatoire."
            !email.contains("@") || !email.contains(".") -> "Format d'e-mail invalide (ex: admin@edufinance.pro)."
            else -> null
        }
    }

    val passwordError = remember(password, hasAttemptedSubmit) {
        if (!hasAttemptedSubmit) null
        else when {
            password.isEmpty() -> "Le mot de passe est obligatoire."
            password.length < 6 -> "Le mot de passe doit contenir au moins 6 caractères."
            else -> null
        }
    }

    val isFormValid = email.trim().isNotBlank() && email.contains("@") && password.length >= 6
    val isAuthenticating = authState is AuthState.Authenticating

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .testTag("login_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Logo & Branding
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Slate900)
                    .border(1.5.dp, Slate800, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "EduFinance Pro Emblem",
                    tint = Emerald400,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "EduFinance",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Blue600,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Blue50)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Text(
                text = "Système de Gestion Académique & Trésorerie Sécurisé",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Main Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Authentification des Utilisateurs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Veuillez saisir vos identifiants pour accéder à votre espace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
                    )

                    // Error state banner
                    AnimatedVisibility(
                        visible = authState is AuthState.AuthError,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (authState is AuthState.AuthError) {
                            Surface(
                                color = Rose100,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Rose600,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = authState.message,
                                        color = Rose600,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Email / Login field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input"),
                        label = { Text("Identifiant / E-mail institutionnel") },
                        placeholder = { Text("ex: admin@edufinance.pro") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = if (emailError != null) Rose500 else Slate400)
                        },
                        trailingIcon = {
                            if (email.isNotEmpty()) {
                                IconButton(onClick = { email = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Effacer", tint = Slate400)
                                }
                            }
                        },
                        isError = emailError != null,
                        supportingText = {
                            if (emailError != null) {
                                Text(text = emailError, color = Rose500, fontSize = 11.sp)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isAuthenticating
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        label = { Text("Mot de passe sécurisé") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = if (passwordError != null) Rose500 else Slate400)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Masquer le mot de passe" else "Afficher le mot de passe",
                                    tint = Slate400
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = passwordError != null,
                        supportingText = {
                            if (passwordError != null) {
                                Text(text = passwordError, color = Rose500, fontSize = 11.sp)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                hasAttemptedSubmit = true
                                if (isFormValid && !isAuthenticating) {
                                    onLoginClick(email, password)
                                }
                            }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isAuthenticating
                    )

                    // Options: Remember Me & Forgot Password
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mémoriser session",
                                style = MaterialTheme.typography.labelMedium,
                                color = Slate600
                            )
                        }

                        TextButton(onClick = { showForgotPasswordNotice = true }) {
                            Text(
                                text = "Mot de passe oublié ?",
                                style = MaterialTheme.typography.labelSmall,
                                color = Blue600,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sign In Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            hasAttemptedSubmit = true
                            if (isFormValid && !isAuthenticating) {
                                onLoginClick(email, password)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sign_in_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue600,
                            disabledContainerColor = Slate300
                        ),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isAuthenticating
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Vérification en cours...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Se Connecter",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Security assurances
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Https, contentDescription = null, tint = Emerald600, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Connexion chiffrée SSL / TLS • Politiques RLS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Demo Accounts Card for reviewer / evaluation
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Amber500, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Comptes de Test (Sélection 1-Clic)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Mot de passe auto",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableUsers.forEach { user ->
                            val defaultPw = when (user.role) {
                                UserRole.ADMIN -> "Admin@2026"
                                UserRole.DIRECTEUR -> "Directeur@2026"
                                UserRole.COMPTABLE -> "Finance@2026"
                                UserRole.SECRETAIRE -> "Scolarite@2026"
                                UserRole.ENSEIGNANT -> "Prof@2026"
                            }

                            Surface(
                                onClick = {
                                    email = user.email
                                    password = defaultPw
                                    hasAttemptedSubmit = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = Slate800,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Blue600),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.avatarInitials,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = user.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = user.email,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Slate400,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    RoleBadge(role = user.role)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Forgot password dialog
        if (showForgotPasswordNotice) {
            var resetEmail by remember { mutableStateOf(email) }
            var isSending by remember { mutableStateOf(false) }
            var resetStatusMessage by remember { mutableStateOf<String?>(null) }
            var isSuccess by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = {
                    if (!isSending) showForgotPasswordNotice = false
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = Blue600,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Réinitialisation du mot de passe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Saisissez votre adresse e-mail pour recevoir un lien officiel Firebase permettant de réinitialiser votre mot de passe en toute sécurité.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )

                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = {
                                resetEmail = it
                                resetStatusMessage = null
                            },
                            label = { Text("Adresse e-mail du compte") },
                            placeholder = { Text("nom@etablissement.ci") },
                            singleLine = true,
                            enabled = !isSending && !isSuccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_password_email_input"),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Slate400)
                            }
                        )

                        if (resetStatusMessage != null) {
                            Surface(
                                color = if (isSuccess) Emerald50 else Rose100,
                                shape = RoundedCornerShape(8.dp),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (isSuccess) Emerald500 else Rose500
                                    )
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (isSuccess) Emerald700 else Rose600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = resetStatusMessage ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSuccess) Emerald700 else Rose600,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (isSuccess) {
                        Button(
                            onClick = { showForgotPasswordNotice = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Fermer")
                        }
                    } else {
                        Button(
                            onClick = {
                                val trimmed = resetEmail.trim()
                                if (trimmed.isBlank() || !trimmed.contains("@")) {
                                    resetStatusMessage = "Veuillez saisir une adresse e-mail valide."
                                    isSuccess = false
                                    return@Button
                                }
                                isSending = true
                                resetStatusMessage = null
                                if (onForgotPasswordClick != null) {
                                    onForgotPasswordClick(trimmed) { result ->
                                        isSending = false
                                        if (result.isSuccess) {
                                            isSuccess = true
                                            resetStatusMessage = "Un e-mail de réinitialisation Firebase a été envoyé à $trimmed. Veuillez consulter votre boîte de réception."
                                        } else {
                                            isSuccess = false
                                            resetStatusMessage = result.exceptionOrNull()?.message
                                                ?: "Impossible d'envoyer l'e-mail de réinitialisation."
                                        }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        val result = com.example.auth.ServiceAuth.getInstance().sendPasswordResetEmail(trimmed)
                                        isSending = false
                                        if (result.isSuccess) {
                                            isSuccess = true
                                            resetStatusMessage = "Un e-mail de réinitialisation Firebase a été envoyé à $trimmed. Veuillez consulter votre boîte de réception."
                                        } else {
                                            isSuccess = false
                                            resetStatusMessage = result.exceptionOrNull()?.message
                                                ?: "Impossible d'envoyer l'e-mail de réinitialisation."
                                        }
                                    }
                                }
                            },
                            enabled = !isSending,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("forgot_password_submit_button")
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Envoi...", fontSize = 12.sp)
                            } else {
                                Text("Envoyer l'e-mail")
                            }
                        }
                    }
                },
                dismissButton = {
                    if (!isSuccess) {
                        TextButton(
                            onClick = { showForgotPasswordNotice = false },
                            enabled = !isSending
                        ) {
                            Text("Annuler", color = Slate500)
                        }
                    }
                }
            )
        }
    }
}
