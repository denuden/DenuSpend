package com.gmail.vondenuelle.denuspend.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.gmail.vondenuelle.denuspend.R
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Coil Image Loader with cache
 */
class CoilUtils(@ApplicationContext val applicationContext: Context) :
    SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): coil3.ImageLoader {
        return coil3.ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context = context, percent = 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(applicationContext.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
    }
}


/**
 * Async Image With error handlers
 * For Avatar
 * Image is Set to Crop Initially
 */
@Composable
fun AsyncImageAvatarWithErrorHandler(
    modifier: Modifier = Modifier, model: Any?, onEnlargeImage: () -> Unit = {},
    contentDescription: String? = "Avatar",
    contentScale: ContentScale = ContentScale.Crop,
    shouldShowEnlargeButton: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        AsyncImage(
            model = model,
            placeholder = painterResource(R.drawable.baseline_account_circle_24),
            contentDescription = contentDescription,
            contentScale = contentScale,
            error = painterResource(R.drawable.baseline_account_circle_24),
            modifier = Modifier.matchParentSize()
        )

        if (shouldShowEnlargeButton) {
            IconButton(
                onClick = onEnlargeImage,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(
                        alpha = 0.4f
                    )
                ),
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Enlarge Image",
                    tint = Color.White,
                )
            }
        }
    }
}


/**
 * Async Image With error handlers
 *
 * Image is Set to Crop Initially
 */
@Composable
fun AsyncImageWithErrorHandler(
    modifier: Modifier = Modifier, model: Any?, onEnlargeImage: () -> Unit = {},
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = "Image",
    shouldShowEnlargeButton: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        AsyncImage(
            model = model,
            placeholder = painterResource(R.drawable.baseline_image_24),
            contentDescription = contentDescription,
            contentScale = contentScale,
            error = painterResource(R.drawable.baseline_image_not_supported_24),
            modifier = Modifier.matchParentSize()
        )

        if (shouldShowEnlargeButton) {
            IconButton(
                onClick = onEnlargeImage,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(
                        alpha = 0.4f
                    )
                ),
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Enlarge Image",
                    tint = Color.White,
                )
            }
        }
    }
}

/**
 * Async Image that takes dominont color of image
 * and make it the background color
 *
 * Image is set to Fit Initially
 */
@SuppressLint("ResourceAsColor")
@Composable
fun AsyncImageWithBackgroundPalette(
    modifier: Modifier = Modifier,
    model: Any?,
    contentDescription: String? = "Image with Background Pallette",
    onEnlargeImage: () -> Unit = {},
    enlargeImageIcon : ImageVector = Icons.Default.OpenInFull,
    shouldShowEnlargeButton: Boolean = true,
    contentScale: ContentScale = ContentScale.Fit,
    onPaletteBuilderSuccess: (color: Int) -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(model)
                .allowHardware(false)
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.baseline_image_24),
            contentDescription = contentDescription,
            contentScale = contentScale,
            error = painterResource(R.drawable.baseline_image_not_supported_24),
            onSuccess = { result ->
                val bitmap = result.result.image.toBitmap()
                Palette.Builder(bitmap).generate { palette ->
                    // Consume the palette.
                    val backgroundColor =
                        palette?.getDominantColor(R.color.black) ?: R.color.black

                    //if you wanna do something more outside of this composable with the generated color
                    onPaletteBuilderSuccess(backgroundColor)
                }
            },
            modifier = Modifier.matchParentSize()
        )

        if (shouldShowEnlargeButton) {
            IconButton(
                onClick = onEnlargeImage,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(
                        alpha = 0.4f
                    )
                ),
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = enlargeImageIcon,
                    contentDescription = "Enlarge Image",
                    tint = Color.White,
                )
            }
        }
    }
}

