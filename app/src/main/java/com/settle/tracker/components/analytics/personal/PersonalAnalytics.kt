package com.settle.tracker.components.analytics.personal

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.LineType
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.settle.tracker.scheme.ExpenseScheme

@Composable
fun PersonalAnalytics() {
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser

    var expenses by remember { mutableStateOf(emptyList<ExpenseScheme>()) }
    val monthlyChart = remember(expenses) { prepareMonthlyData(expenses) }

    LaunchedEffect(Unit) {
        if (currentUser == null) return@LaunchedEffect

        db
            .collection("users")
            .document(currentUser.uid)
            .collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                expenses = snapshot.toObjects(ExpenseScheme::class.java)
            }
    }

    Column {
        val pointsData: List<Point> =
            listOf(Point(0f, 40f), Point(1f, 90f), Point(2f, 0f), Point(3f, 60f), Point(4f, 10f))

        val xAxisData = AxisData.Builder()
            .axisStepSize(100.dp)
            .axisLabelColor(MaterialTheme.colorScheme.onSurface)
            .steps(pointsData.size - 1)
            .labelData { i -> i.toString() }
            .labelAndAxisLinePadding(15.dp)
            .build()

        val yAxisData = AxisData.Builder()
            .steps(pointsData.size - 1)
            .axisLabelColor(MaterialTheme.colorScheme.onSurface)
            .labelAndAxisLinePadding(20.dp)
            .labelData { i ->
                val yScale = 100 / (pointsData.size - 1)
                (i * yScale).toString()
            }.build()

        val lineChartData = LineChartData(
            linePlotData = LinePlotData(
                lines = listOf(
                    Line(
                        dataPoints = pointsData,
                        LineStyle(
                            lineType = LineType.Straight(),
                            color = MaterialTheme.colorScheme.primary
                        ),
                        IntersectionPoint(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        SelectionHighlightPoint(),
                        ShadowUnderLine(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        SelectionHighlightPopUp()
                    )
                ),
            ),
            xAxisData = xAxisData,
            yAxisData = yAxisData,
            backgroundColor = MaterialTheme.colorScheme.surface
        )

        LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            lineChartData = lineChartData
        )
    }
}
