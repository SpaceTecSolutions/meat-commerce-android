package com.spacetecsolutions.meatapp.feature.admin.banner

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.BannerContentAlignment
import com.spacetecsolutions.meatapp.core.model.PromotionBanner

@Composable
internal fun BannerFormScreen(form: BannerForm, busy: Boolean, actions: BannerManagementViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { actions.update { value -> value.copy(selectedImage = it.toString()) } }
    }
    Scaffold(
        topBar = { AppBackTopBar(if (form.id == null) "Add Home Banner" else "Edit Home Banner", actions::closeForm) },
        bottomBar = { PrimaryButton(
            text = if (busy) "Saving…" else "Save Banner", onClick = actions::save, enabled = !busy,
            modifier = Modifier.padding(AppSpacing.medium),
        ) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .widthIn(max = AppDimensions.formMaxWidth).wrapContentWidth(Alignment.CenterHorizontally)
                .padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_customer_preview_afd0ea0), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(180.dp)) {
                AppImage(form.selectedImage ?: form.existingImage, null, Modifier.fillMaxSize(), cornerRadius = 14.dp)
                PositionedBannerContent(form.preview(), onMoveText = { position -> actions.update { it.copy(textPlacement = position) } },
                    onMoveSubtitle = { position -> actions.update { it.copy(subtitlePlacement = position) } },
                    onMoveButton = { position -> actions.update { it.copy(buttonPlacement = position) } })
            }
            if (form.textEnabled) Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_drag_title_message_and_button_dc0da25), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(AppIcons.Camera, null); Spacer(Modifier.width(AppSpacing.small))
                Text(if (form.existingImage == null && form.selectedImage == null) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_choose_banner_image_069e607) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_change_image_62a7b48))
            }
            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_use_a_landscape_image_the_d5770cb),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_banner_display_text), fontWeight = FontWeight.SemiBold)
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_show_banner_title_and_message),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(form.textEnabled, { value -> actions.update { it.copy(textEnabled = value) } },
                    enabled = !busy, colors = appSwitchColors())
            }
            if (form.textEnabled) {
                OutlinedTextField(form.title, { value -> actions.update { it.copy(title = value.take(80)) } },
                    Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_title_768e0c1)) }, singleLine = true, enabled = !busy)
                OutlinedTextField(form.subtitle, { value -> actions.update { it.copy(subtitle = value.take(180)) } },
                    Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_message_68f4145)) }, minLines = 2, maxLines = 3, enabled = !busy)
            }
            if (form.textEnabled || form.buttonEnabled) BannerPlacementControls(form, actions)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_show_action_button_745ef4f), fontWeight = FontWeight.SemiBold)
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_opens_the_selected_product_c03cf6f), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Switch(form.buttonEnabled, { value -> actions.update { it.copy(buttonEnabled = value) } },
                    enabled = !busy, colors = appSwitchColors())
            }
            if (form.buttonEnabled) {
                OutlinedTextField(form.buttonText, { value -> actions.update { it.copy(buttonText = value.take(24)) } },
                    Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_button_text_2c1bff4)) }, singleLine = true, enabled = !busy)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(form.buttonColor == "RED", { actions.update { it.copy(buttonColor = "RED") } },
                        label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_red_button_498f910)) })
                    FilterChip(form.buttonColor == "WHITE", { actions.update { it.copy(buttonColor = "WHITE") } },
                        label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_white_button_07f5a4f)) })
                }
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_button_text_color_e0174b5), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("WHITE", "RED", "BLACK").forEach { color ->
                FilterChip(form.buttonTextColor == color,
                            { actions.update { it.copy(buttonTextColor = color) } },
                            label = { Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(14.dp).background(
                                    when (color) { "RED" -> MaterialTheme.colorScheme.primary; "BLACK" -> Color.Black; else -> Color.White },
                                    androidx.compose.foundation.shape.CircleShape,
                                ))
                                Text(androidx.compose.ui.res.stringResource(when (color) {
                                    "RED" -> com.spacetecsolutions.meatapp.feature.admin.R.string.ui_text_color_red
                                    "BLACK" -> com.spacetecsolutions.meatapp.feature.admin.R.string.ui_text_color_black
                                    else -> com.spacetecsolutions.meatapp.feature.admin.R.string.ui_text_color_white
                                }))
                            } })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(form.buttonShape == "ROUNDED", { actions.update { it.copy(buttonShape = "ROUNDED") } },
                        label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_rounded_c5aa340)) })
                    FilterChip(form.buttonShape == "CAPSULE", { actions.update { it.copy(buttonShape = "CAPSULE") } },
                        label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_capsule_c81cf89)) })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_show_arrow_in_button_335bc5c), Modifier.weight(1f))
                    Switch(form.buttonArrow, { value -> actions.update { it.copy(buttonArrow = value) } },
                        colors = appSwitchColors())
                }
                BannerDestinationPicker(form, actions)
            }
            OutlinedTextField(form.sortOrder, { value -> actions.update { it.copy(sortOrder = value.filter(Char::isDigit)) } },
                Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_display_order_0_first_0c9ddb2)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !busy)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_display_in_customer_app_7b4ddbe), fontWeight = FontWeight.SemiBold)
                    Text(if (form.active) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_visible_1fe5939) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_hidden_d4c2792), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Switch(form.active, { value -> actions.update { it.copy(active = value) } },
                    enabled = !busy, colors = appSwitchColors())
            }
            Spacer(Modifier.height(AppSpacing.large))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlignmentSelector(value: BannerContentAlignment, busy: Boolean, select: (BannerContentAlignment) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { if (!busy) open = it }) {
        OutlinedTextField(value.label(), {}, Modifier.fillMaxWidth().menuAnchor(), readOnly = true,
            label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_text_and_button_alignment_dc242af)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) })
        ExposedDropdownMenu(open, { open = false }) { BannerContentAlignment.entries.forEach { alignment ->
            DropdownMenuItem(text = { Text(alignment.label()) }, onClick = { select(alignment); open = false })
        } }
    }
}

@Composable
internal fun BannerPreview(banner: PromotionBanner, modifier: Modifier, overrideImage: String? = null) {
    Box(modifier) {
        AppImage(overrideImage ?: banner.imageUrl, banner.title, Modifier.fillMaxSize(), cornerRadius = 14.dp)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
            Color.Black.copy(alpha = .10f), Color.Black.copy(alpha = .62f),
        )), AppShapes.medium))
        if (banner.textPlacement != null) PositionedBannerContent(banner)
        else Column(
            Modifier.align(banner.contentAlignment.boxAlignment()).fillMaxWidth(.72f).padding(AppSpacing.medium),
            horizontalAlignment = banner.contentAlignment.horizontal(),
        ) {
            if (banner.textEnabled && banner.title.isNotBlank()) Text(banner.title, color = Color.White, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, textAlign = banner.contentAlignment.textAlign())
            if (banner.textEnabled && banner.subtitle.isNotBlank()) Text(banner.subtitle, color = Color.White.copy(alpha = .9f),
                style = MaterialTheme.typography.bodySmall, textAlign = banner.contentAlignment.textAlign())
            if (banner.buttonEnabled) Surface(
                color = if (banner.buttonColor == "WHITE") Color.White else MaterialTheme.colorScheme.primary,
                shape = if (banner.buttonShape == "CAPSULE") androidx.compose.foundation.shape.RoundedCornerShape(50) else AppShapes.extraSmall,
                modifier = Modifier.padding(top = AppSpacing.small)) {
                Text(banner.buttonText + if (banner.buttonArrow) "  →" else "",
                    Modifier.padding(horizontal = AppSpacing.compact, vertical = 7.dp),
                    color = when (banner.buttonTextColor) { "RED" -> MaterialTheme.colorScheme.primary
                        "BLACK" -> Color.Black; else -> Color.White }, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun BannerForm.preview() = PromotionBanner(
    id = id.orEmpty(), title = title, subtitle = subtitle, textEnabled = textEnabled,
    imageUrl = existingImage, contentAlignment = alignment, buttonEnabled = buttonEnabled,
    buttonText = buttonText.ifBlank { "Shop Now" }, active = active,
    textPlacement = textPlacement, buttonPlacement = buttonPlacement,
    subtitlePlacement = subtitlePlacement, buttonColor = buttonColor, buttonTextColor = buttonTextColor,
    buttonShape = buttonShape, buttonArrow = buttonArrow,
)
private fun BannerContentAlignment.boxAlignment() = when {
    name.startsWith("TOP") -> if (name.endsWith("START")) Alignment.TopStart else if (name.endsWith("END")) Alignment.TopEnd else Alignment.TopCenter
    name.startsWith("BOTTOM") -> if (name.endsWith("START")) Alignment.BottomStart else if (name.endsWith("END")) Alignment.BottomEnd else Alignment.BottomCenter
    else -> if (name.endsWith("START")) Alignment.CenterStart else if (name.endsWith("END")) Alignment.CenterEnd else Alignment.Center
}
private fun BannerContentAlignment.horizontal() = when {
    name.endsWith("START") -> Alignment.Start
    name.endsWith("END") -> Alignment.End
    else -> Alignment.CenterHorizontally
}
private fun BannerContentAlignment.textAlign() = when {
    name.endsWith("START") -> TextAlign.Start
    name.endsWith("END") -> TextAlign.End
    else -> TextAlign.Center
}
