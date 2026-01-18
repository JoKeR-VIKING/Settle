package com.settle.tracker.components.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.settle.tracker.utils.dashedBorder

@Composable
fun AddMemberButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = modifier
            .fillMaxWidth()
            .dashedBorder(
                2.dp,
                MaterialTheme.colorScheme.primary,
                cornerRadius = 10.dp,
                dashLength = 30f,
                gapLength = 15f
            ),
        contentPadding = PaddingValues(vertical = 14.dp),
        border = BorderStroke(
            width = 0.dp,
            color = Color.Transparent
        ),
        shape = RoundedCornerShape(10),
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.Filled.Add,
            contentDescription = "Add New Member",
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            "Add Member",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
