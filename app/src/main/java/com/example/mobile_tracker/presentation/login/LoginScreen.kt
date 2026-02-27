package com.example.mobile_tracker.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.StateCard
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LoginEffect.NavigateToContextSelection ->
                    onLoginSuccess()
            }
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer
                .copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surface,
        ),
    )

    AppScreenScaffold(
        snackbarMessage = state.error,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(gradient)
                .imePadding(),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme
                            .primaryContainer,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Watch,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme
                        .onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography
                    .headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme
                    .onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme
                    .onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme
                        .colorScheme
                        .surfaceContainerLowest,
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = {
                            viewModel.onIntent(
                                LoginIntent.EmailChanged(
                                    it,
                                ),
                            )
                        },
                        label = { Text(stringResource(R.string.login_email_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            keyboardType =
                                KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusManager.moveFocus(
                                    FocusDirection.Down,
                                )
                            },
                        ),
                        enabled = !state.isLoading,
                    )

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = {
                            viewModel.onIntent(
                                LoginIntent.PasswordChanged(
                                    it,
                                ),
                            )
                        },
                        label = { Text(stringResource(R.string.login_password_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape =
                            MaterialTheme.shapes.small,
                        visualTransformation =
                            if (state.isPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    viewModel.onIntent(
                                        LoginIntent
                                            .TogglePasswordVisibility,
                                    )
                                },
                            ) {
                                Icon(
                                    imageVector =
                                        if (state
                                                .isPasswordVisible
                                        ) {
                                            Icons.Default
                                                .VisibilityOff
                                        } else {
                                            Icons.Default
                                                .Visibility
                                        },
                                    contentDescription =
                                        if (state
                                                .isPasswordVisible
                                        ) {
                                            stringResource(R.string.login_hide_password)
                                        } else {
                                            stringResource(R.string.login_show_password)
                                        },
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType =
                                KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.onIntent(
                                    LoginIntent.LoginClicked,
                                )
                            },
                        ),
                        enabled = !state.isLoading,
                    )

                    if (state.error != null) {
                        StateCard(message = state.error!!)
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.onIntent(
                                LoginIntent.LoginClicked,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !state.isLoading,
                        shape =
                            MaterialTheme.shapes.small,
                        elevation =
                            ButtonDefaults
                                .buttonElevation(
                                    defaultElevation =
                                        2.dp,
                                ),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(22.dp),
                                color = MaterialTheme
                                    .colorScheme
                                    .onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.login_button),
                                style = MaterialTheme
                                    .typography
                                    .titleMedium,
                                fontWeight =
                                    FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1.2f))
        }
    }
    }
}
