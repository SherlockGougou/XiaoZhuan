package com.xigong.xiaozhuan.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xigong.xiaozhuan.style.AppColors

@Composable
fun UpdateDescView(updateDesc: MutableState<String>) {
    val textSize = 14.sp
    val interactionSource = remember { MutableInteractionSource() }
    val clearVisible by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)

    ) {
        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(
            value = updateDesc.value,
            placeholder = {
                Text(
                    "请填写更新描述",
                    color = AppColors.fontGray,
                    fontSize = textSize
                )
            },
            onValueChange = { updateDesc.value = it },
            textStyle = TextStyle(fontSize = textSize),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = AppColors.primary,
                backgroundColor = Color.White
            ),
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth()
                .height(200.dp)
        )


        AnimatedVisibility(
            clearVisible && updateDesc.value.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {

            Image(
                painter = painterResource("input_clear.png"),
                contentDescription = "清空",
                modifier = Modifier
                    .padding(10.dp)
                    .clip(CircleShape)
                    .size(22.dp)
                    .clickable {
                        updateDesc.value = ""
                        focusRequester.requestFocus()
                    }
            )
        }

    }
}