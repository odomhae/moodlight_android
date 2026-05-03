package com.odom.moodlight.ui.component

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odom.moodlight.ui.theme.AppColors
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val snapBehavior = rememberSnapFlingBehavior(listState)

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index -> onSelectedIndexChange(index) }
    }

    Box(modifier = modifier.height(120.dp), contentAlignment = Alignment.Center) {
        HorizontalDivider(
            modifier = Modifier.offset(y = (-20).dp),
            color = AppColors.Border
        )
        HorizontalDivider(
            modifier = Modifier.offset(y = 20.dp),
            color = AppColors.Border
        )
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val isSelected = listState.firstVisibleItemIndex == index
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        fontSize = if (isSelected) 22.sp else 16.sp,
                        color = if (isSelected) AppColors.TextPrimary else AppColors.TextDim
                    )
                }
            }
        }
    }
}
