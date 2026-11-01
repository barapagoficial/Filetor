package com.filetor.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

@Composable
fun UsageBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 8.dp, color: Color = MaterialTheme.colorScheme.primary) {
    var target by remember { mutableStateOf(0f) }
    LaunchedEffect(fraction) { target = fraction }
    val anim by animateFloatAsState(target, tween(900), label = "usage")
    val warn = fraction > 0.9f
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))) {
        Box(Modifier.fillMaxWidth(anim.coerceIn(0.005f, 1f)).fillMaxHeight().clip(CircleShape)
            .background(if (warn) MaterialTheme.colorScheme.error else color))
    }
}

@Composable
fun SegmentBar(segments: List<Pair<Float, Color>>, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    Row(modifier.fillMaxWidth().height(height).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        segments.filter { it.first > 0.004f }.forEach { (f, c) ->
            Box(Modifier.weight(f).fillMaxHeight().background(c))
        }
        val rest = 1f - segments.sumOf { it.first.toDouble() }.toFloat()
        if (rest > 0.004f) Spacer(Modifier.weight(rest))
    }
}

@Composable
fun Donut(segments: List<Pair<Float, Color>>, modifier: Modifier = Modifier, stroke: Dp = 18.dp) {
    var target by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) { target = 1f }
    val progress by animateFloatAsState(target, tween(1100), label = "donut")
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    Canvas(modifier) {
        val sw = stroke.toPx()
        val d = size.minDimension - sw
        val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
        drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
        var start = -90f
        segments.forEach { (f, c) ->
            val sweep = 360f * f * progress
            if (sweep > 0.5f) drawArc(c, start + 1f, (sweep - 2f).coerceAtLeast(0.5f), false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Butt))
            start += sweep
        }
    }
}

@Composable
fun IconTile(icon: ImageVector, color: Color, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) { Spacer(Modifier.width(12.dp)); trailing() }
    }
}

@Composable
fun SwitchRow(icon: ImageVector, title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(icon, title, subtitle, onClick = { onChange(!checked) }) { Switch(checked, onChange) }
}

@Composable
fun InfoLine(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, overflow = TextOverflow.Ellipsis, maxLines = 4)
    }
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, color: Color = MaterialTheme.colorScheme.surfaceContainerLow, content: @Composable () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(24.dp)).background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) { content() }
}
