package com.xigong.xiaozhuan.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun TwoPage(
    leftPage: @Composable ColumnScope.() -> Unit,
    rightPage: @Composable ColumnScope.() -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxHeight().weight(4.0f)
                .padding(20.dp),
            content = leftPage
        )
        Column(
            modifier = Modifier.fillMaxHeight().weight(6.0f)
                .padding(20.dp),
            content = rightPage
        )
    }
}