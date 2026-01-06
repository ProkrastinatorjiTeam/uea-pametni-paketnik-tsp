package com.prokrastinatorji.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.prokrastinatorji.core.TSP

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTextApi::class)
@Composable
fun TSPVisualizer(tour: TSP.Tour?) {
    var scale by remember { mutableStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offset += dragAmount / scale
                }
            }
            .onPointerEvent(PointerEventType.Scroll) {
                val scrollDelta = it.changes.first().scrollDelta.y
                val zoomFactor = 1.1f
                val newScale = if (scrollDelta > 0) scale / zoomFactor else scale * zoomFactor
                scale = newScale.coerceIn(0.1f, 20.0f)
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (tour == null || tour.path.size < 2) return@Canvas

            translate(left = offset.x, top = offset.y) {
                scale(scale = scale) {
                    drawTSP(tour, scale, textMeasurer)
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawTSP(
    tour: TSP.Tour,
    scale: Float,
    textMeasurer: TextMeasurer
) {
    val cities = tour.path
    val padding = 40f

    val canvasWidth = (size.width - 2 * padding).coerceAtLeast(0f)
    val canvasHeight = (size.height - 2 * padding).coerceAtLeast(0f)
    if (canvasWidth == 0f || canvasHeight == 0f) return

    val minX = cities.minOf { it.x }
    val maxX = cities.maxOf { it.x }
    val minY = cities.minOf { it.y }
    val maxY = cities.maxOf { it.y }

    val worldWidth = (maxX - minX).takeIf { it > 0 } ?: 1.0
    val worldHeight = (maxY - minY).takeIf { it > 0 } ?: 1.0

    val scaleToFitX = canvasWidth / worldWidth
    val scaleToFitY = canvasHeight / worldHeight
    val scaleToFit = minOf(scaleToFitX, scaleToFitY).toFloat()

    fun transformToCanvas(city: TSP.City): Offset {
        val canvasX = padding + ((city.x - minX) * scaleToFit).toFloat()
        val canvasY = padding + ((city.y - minY) * scaleToFit).toFloat()
        return Offset(canvasX, canvasY)
    }

    for (i in cities.indices) {
        val from = cities[i]
        val to = cities[(i + 1) % cities.size]
        drawLine(
            color = Color.Blue,
            start = transformToCanvas(from),
            end = transformToCanvas(to),
            strokeWidth = (1.5f / scale).coerceAtLeast(0.5f)
        )
    }

    cities.forEach { city ->
        val center = transformToCanvas(city)
        drawCircle(
            color = Color.Red,
            radius = (4f / scale).coerceAtLeast(1f),
            center = center
        )

        if (scale > 1.5f) {
            drawText(
                textMeasurer = textMeasurer,
                text = city.id.toString(),
                topLeft = center + Offset(6f / scale, -20f / scale),
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.DarkGray,
                    fontSize = (12 / scale).sp
                )
            )
        }
    }
}