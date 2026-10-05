package com.subarudl.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.subarudl.app.ui.theme.C
import com.subarudl.app.ui.theme.T

/** Título grande de uma tela, com a frase de apoio embaixo. */
@Composable
fun ScreenTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = T.headlineLg)
        Text(subtitle, style = T.bodyMd, color = C.Muted)
    }
}

/** Campo de link (ou vários, um por linha) no estilo Material 3. */
@Composable
fun LinkField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 3,
    onClear: (() -> Unit)? = null,
    onGo: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Link, null) },
        trailingIcon = { if (value.isNotEmpty()) IconButton(onClick = { onChange(""); onClear?.invoke() }) { Icon(Icons.Filled.Close, "Limpar") } },
        maxLines = maxLines,
        textStyle = T.bodyMd,
        shape = RoundedCornerShape(16.dp),
        colors = textFieldColors(),
        keyboardOptions = KeyboardOptions(imeAction = if (onGo != null) ImeAction.Go else ImeAction.Default),
        keyboardActions = KeyboardActions(onGo = { onGo?.invoke() }),
    )
}
