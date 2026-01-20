package com.settle.tracker.components.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.settle.tracker.scheme.UserScheme
import com.settle.tracker.utils.formatCurrency

@Composable
fun EqualSplitMemberRow(
    modifier: Modifier = Modifier,
    member: UserScheme,
    isSelected: Boolean,
    splitAmount: Double,
    onSelectionChange: () -> Unit,
    minSelectionCount: Int = -1
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = {
                    if (minSelectionCount == 1 && isSelected) {
                        return@clickable
                    }
                    onSelectionChange()
                }
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(25.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = member.photoUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    member.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 13.sp,
                    maxLines = 2,
                )

                Text(
                    text = formatCurrency(splitAmount),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (!isSelected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = null
        )
    }
}
