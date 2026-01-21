package com.settle.tracker.components.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.settle.tracker.components.MultiDigitTextField
import com.settle.tracker.utils.parsePaymentMethod
import kotlinx.coroutines.launch

data class PaymentOption(val name: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodPickerModal(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    previousPaymentMethods: List<String>,
    initialPaidFrom: String = "",
    onPaymentMethodSelected: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val partialHeight = screenHeight * 0.4f

    val options = listOf(
        PaymentOption("Card", Icons.Filled.CreditCard),
        PaymentOption("Bank A/C", Icons.Filled.AccountBalance),
        PaymentOption("Wallet", Icons.Filled.Wallet)
    )

    val paymentPages = remember(previousPaymentMethods) {
        previousPaymentMethods.chunked(2)
    }
    val pagerState = rememberPagerState(
        pageCount = { paymentPages.size }
    )

    val initialInfo = remember(initialPaidFrom) {
        parsePaymentMethod(initialPaidFrom)
    }

    var selectedIndex by remember { mutableIntStateOf(initialInfo.selectedIndex) }
    var lastFourDigits by remember { mutableStateOf(initialInfo.lastFourDigits) }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            val currentInfo = parsePaymentMethod(initialPaidFrom)
            selectedIndex = currentInfo.selectedIndex
            lastFourDigits = currentInfo.lastFourDigits
            onDismissRequest()
        },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = partialHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.95f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                (pagerState.currentPage - 1).coerceAtLeast(0)
                            )
                        }
                    },
                    enabled = pagerState.currentPage > 0
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Previous"
                    )
                }

                HorizontalPager(
                    modifier = Modifier.weight(1f),
                    state = pagerState,
                ) { pageIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.weight(1f)
                        ) {
                            paymentPages[pageIndex].forEachIndexed { index, label ->
                                SegmentedButton(
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = paymentPages[pageIndex].size
                                    ),
                                    selected = initialPaidFrom == label,
                                    onClick = {
                                        onPaymentMethodSelected(label)
                                        onDismissRequest()
                                    }
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                (pagerState.currentPage + 1).coerceAtMost(
                                    pagerState.pageCount - 1
                                )
                            )
                        }
                    },
                    enabled = pagerState.currentPage < pagerState.pageCount - 1
                ) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            label = {
                                Text(
                                    text = label.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = label.icon,
                                    contentDescription = label.name
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size
                            ),
                            selected = selectedIndex == index,
                            onClick = {
                                selectedIndex = index
                                lastFourDigits = ""
                            }
                        )
                    }
                }

                MultiDigitTextField(
                    numberOfDigits = 4,
                    lastFourDigits,
                    onValueChange = { lastFourDigits = it },
                    isVisible = selectedIndex in 0..1,
                    helperText = if (selectedIndex in 0..1) {
                        "Enter the last 4 digits of your ${options[selectedIndex].name}"
                    } else {
                        ""
                    }
                )
            }

            Button(
                onClick = {
                    val paymentMethod = if (selectedIndex in 0..1) {
                        "${options[selectedIndex].name} $lastFourDigits"
                    } else if (selectedIndex == 2) {
                        options[selectedIndex].name
                    } else {
                        ""
                    }
                    onPaymentMethodSelected(paymentMethod)
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(25),
                enabled = if (selectedIndex in 0..1) {
                    lastFourDigits.length == 4
                } else {
                    selectedIndex != -1
                }
            ) {
                Text(
                    "Confirm",
                    style = MaterialTheme.typography.bodyLarge,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
