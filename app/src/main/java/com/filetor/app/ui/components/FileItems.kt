package com.filetor.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.filetor.app.data.FileEntry
import com.filetor.app.data.Prefs
import com.filetor.app.util.FileActions
import com.filetor.app.util.FileType
import com.filetor.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun TypeIconBox(type: FileType, size: Dp, modifier: Modifier = Modifier, corner: Dp = size * 0.3f) {
    val tint = if (type == FileType.FOLDER) MaterialTheme.colorScheme.primary else type.color
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(type.icon, null, tint = tint, modifier = Modifier.size(size * 0.54f))
    }
}

@Composable
fun FileThumb(entry: FileEntry, size: Dp, modifier: Modifier = Modifier) {
    val type = entry.type
    val corner = size * 0.28f
    val context = LocalContext.current
    when {
        Prefs.showThumbnails && (type == FileType.IMAGE || type == FileType.VIDEO) -> {
            Box(modifier.size(size).clip(RoundedCornerShape(corner)).background(type.color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(type.icon, null, tint = type.color, modifier = Modifier.size(size * 0.5f))
                AsyncImage(
                    model = ImageRequest.Builder(context).data(File(entry.path)).size(256).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (type == FileType.VIDEO) {
                    Box(Modifier.size(size * 0.42f).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(size * 0.3f))
                    }
                }
            }
        }
        Prefs.showThumbnails && type == FileType.APK -> {
            val icon by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, entry.path) {
                value = withContext(Dispatchers.IO) {
                    runCatching { FileActions.apkIcon(context, File(entry.path))?.toBitmap(128, 128)?.asImageBitmap() }.getOrNull()
                }
            }
            val bmp = icon
            if (bmp != null) Box(modifier.size(size), contentAlignment = Alignment.Center) {
                Image(bmp, null, modifier = Modifier.size(size * 0.9f))
            } else TypeIconBox(type, size, modifier, corner)
        }
        else -> TypeIconBox(type, size, modifier, corner)
    }
}

@Composable
fun SelectableThumb(entry: FileEntry, size: Dp, selected: Boolean) {
    val scale by animateFloatAsState(if (selected) 0.86f else 1f, label = "thumbScale")
    Box(contentAlignment = Alignment.Center) {
        FileThumb(entry, size, Modifier.scale(scale))
        AnimatedVisibility(selected, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Box(
                Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(size * 0.5f)) }
        }
    }
}

fun subtitleFor(entry: FileEntry): String = if (entry.isDirectory) {
    val c = if (entry.childCount >= 0) Format.items(entry.childCount) + " · " else ""
    c + Format.date(entry.lastModified)
} else Format.size(entry.size) + " · " + Format.date(entry.lastModified)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    entry: FileEntry,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = subtitleFor(entry),
    bookmarked: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent, label = "rowBg")
    val compact = Prefs.compactList
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = if (compact) 6.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectableThumb(entry, if (compact) 38.dp else 46.dp, selected)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (entry.isHidden) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (bookmarked) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.Star, null, tint = Color(0xFFF5B400), modifier = Modifier.size(14.dp))
                }
            }
            if (!compact || subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing?.invoke()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    entry: FileEntry,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow, label = "gridBg")
    Column(
        modifier
            .padding(5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            val media = entry.type == FileType.IMAGE || entry.type == FileType.VIDEO
            if (media && Prefs.showThumbnails) {
                FileThumb(entry, 200.dp, Modifier.fillMaxSize())
            } else {
                FileThumb(entry, 64.dp)
            }
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp)) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(entry.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 2,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(
            if (entry.isDirectory) (if (entry.childCount >= 0) Format.items(entry.childCount) else "") else Format.size(entry.size),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
        )
    }
}

@Composable
fun LockBadge() {
    Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String = "", modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
