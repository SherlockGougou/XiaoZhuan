package com.xigong.xiaozhuan.page.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xigong.xiaozhuan.config.ApkConfig
import com.xigong.xiaozhuan.style.AppColors
import com.xigong.xiaozhuan.style.AppShapes


@Composable
fun ApkSelector(apks: List<ApkConfig>, current: ApkConfig, onSelected: (ApkConfig) -> Unit) {
    var showApkMenu by remember { mutableStateOf(false) }
    Column {
        val width = 180.dp
        val source = remember { MutableInteractionSource() }
        val hovered = source.collectIsHoveredAsState().value
        val textColor = if (hovered || showApkMenu) AppColors.primary else AppColors.fontBlack
        val borderColor = if (hovered || showApkMenu) AppColors.primary else AppColors.border
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(AppShapes.roundButton)
                .width(width)
                .hoverable(source)
                .border(1.dp, borderColor, AppShapes.roundButton)
                .clickable {
                    showApkMenu = true
                }
                .padding(12.dp)
        ) {
            Text(current.name, fontSize = 14.sp, color = textColor)
            Spacer(Modifier.weight(1f))
            Image(
                painterResource("arrow_down.png"),
                contentDescription = null,
                colorFilter = ColorFilter.tint(AppColors.border),
                modifier = Modifier.size(16.dp)
            )

        }
        if (showApkMenu) {
            DropdownMenu(
                true,
                onDismissRequest = {
                    showApkMenu = false
                }, modifier = Modifier.width(width)
                    .padding(horizontal = 8.dp)
                    .heightIn(max = 400.dp)
            ) {
                apks.forEach { apk ->
                    key(apk.applicationId) {
                        val background = if (apk.applicationId == current.applicationId) {
                            AppColors.auxiliary
                        } else {
                            Color.Transparent
                        }
                        item(apk.name, modifier = Modifier.background(background)) {
                            onSelected(apk)
                            showApkMenu = false
                        }
                    }

                }
            }
        }
    }
}

@Composable
private fun item(
    title: String,
    color: Color = AppColors.fontBlack,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(contentAlignment = Alignment.CenterStart, modifier = Modifier
        .clip(AppShapes.roundButton)
        .clickable { onClick() }
        .then(modifier)
        .fillMaxWidth()
        .padding(vertical = 14.dp, horizontal = 12.dp)

    ) {
        Text(
            text = title,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.W400,
        )
    }
}