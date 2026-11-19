package com.gmail.vondenuelle.denuspend.ui.auth.components

import android.graphics.BlurMaskFilter
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R

// Shared pieces of the Login and Register screens, so both stay identical.

/** Logo, title and subtitle at the top of the auth screens. */
@Composable
fun AuthHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Placeholder until the app has a real logo: swap this Box for an Image(painterResource(R.drawable.<logo>))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 8.dp)
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Primary full-width button with the soft primary-colored glow behind it. */
@Composable
fun AuthGlowButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colorPrimary = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val glowRadius = 50.dp.toPx()
                val spread = 12.dp.toPx()

                val paint = Paint().asFrameworkPaint().apply {
                    color = colorPrimary.toArgb()
                    maskFilter = BlurMaskFilter(
                        glowRadius,
                        BlurMaskFilter.Blur.NORMAL
                    )
                }

                drawIntoCanvas { canvas ->
                    val rect = RectF(
                        spread,
                        spread,
                        size.width - spread,
                        size.height - spread
                    )

                    canvas.nativeCanvas.drawRoundRect(
                        rect,
                        32.dp.toPx(), // corner radius X
                        32.dp.toPx(), // corner radius Y
                        paint
                    )
                }
            }
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = ButtonDefaults.buttonElevation(0.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text)
        }
    }
}

/**
 * "Or continue with" divider plus one row of icon-only Google, Facebook and phone buttons.
 * They are not wired yet, so tapping one shows a "Coming soon" toast.
 */
@Composable
fun SocialSignInButtons() {
    val context = LocalContext.current
    val comingSoon = stringResource(R.string.lbl_coming_soon)
    val onClick = { Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.lbl_other_signin_methods),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SocialButton(
                description = stringResource(R.string.lbl_google),
                painter = painterResource(R.drawable.ic_google_neutral_rd_na),
                onClick = onClick
            )
            SocialButton(
                description = stringResource(R.string.lbl_facebook),
                painter = painterResource(R.drawable.ic_facebook_logo),
                onClick = onClick
            )
            SocialButton(
                description = stringResource(R.string.lbl_contact_number),
                imageVector = Icons.Filled.PhoneAndroid,
                onClick = onClick
            )
        }
    }
}

// Brand logos (painter) are the official assets drawn as-is, since they already are 40dp circles.
// The phone icon (imageVector) gets a tonal 40dp circle so all three match.
@Composable
private fun SocialButton(
    description: String,
    onClick: () -> Unit,
    painter: Painter? = null,
    imageVector: ImageVector? = null,
) {
    if (painter != null) {
        IconButton(onClick = onClick) {
            Icon(painter = painter, contentDescription = description, modifier = Modifier.size(40.dp), tint = Color.Unspecified)
        }
    } else if (imageVector != null) {
        FilledTonalIconButton(onClick = onClick) {
            Icon(imageVector = imageVector, contentDescription = description, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
