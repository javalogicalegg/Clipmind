package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lightweight, zero-overhead vector icon set replacing `material-icons-extended`
 * so the APK avoids bundling thousands of unused icon classes.
 */
object AppIcons {

    private fun buildStrokeIcon(
        name: String,
        strokeWidth: Float = 2.0f,
        block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
    ): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block
            )
        }.build()
    }

    private fun buildFilledIcon(
        name: String,
        block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
    ): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathBuilder = block
            )
        }.build()
    }

    val PrecisionManufacturing: ImageVector by lazy {
        buildStrokeIcon("PrecisionManufacturing", 2.1f) {
            // Base platform
            moveTo(3f, 20f)
            lineTo(21f, 20f)
            // Robotic arm pedestal & joints
            moveTo(6f, 20f)
            lineTo(6f, 13f)
            lineTo(12f, 7f)
            lineTo(17f, 10f)
            // Gripper / Paperclip die
            moveTo(17f, 7.5f)
            lineTo(20f, 9f)
            lineTo(17f, 12.5f)
            // Pivot circle
            moveTo(7.5f, 13f)
            arcToRelative(1.5f, 1.5f, 0f, true, true, -3f, 0f)
            arcToRelative(1.5f, 1.5f, 0f, true, true, 3f, 0f)
        }
    }

    val Analytics: ImageVector by lazy {
        buildStrokeIcon("Analytics", 2.1f) {
            // Outer telemetry frame
            moveTo(4f, 5f)
            lineTo(20f, 5f)
            lineTo(20f, 19f)
            lineTo(4f, 19f)
            close()
            // Bars
            moveTo(8f, 15f)
            lineTo(8f, 12f)
            moveTo(12f, 15f)
            lineTo(12f, 9f)
            moveTo(16f, 15f)
            lineTo(16f, 11f)
        }
    }

    val Science: ImageVector by lazy {
        buildStrokeIcon("Science", 2.0f) {
            moveTo(9f, 3f)
            lineTo(15f, 3f)
            moveTo(10f, 3f)
            lineTo(10f, 8.5f)
            lineTo(5f, 17.5f)
            lineTo(5f, 20f)
            lineTo(19f, 20f)
            lineTo(19f, 17.5f)
            lineTo(14f, 8.5f)
            lineTo(14f, 3f)
            moveTo(7.5f, 14.5f)
            lineTo(16.5f, 14.5f)
        }
    }

    val Public: ImageVector by lazy {
        buildStrokeIcon("Public", 2.0f) {
            // Globe circle
            moveTo(21f, 12f)
            arcToRelative(9f, 9f, 0f, true, true, -18f, 0f)
            arcToRelative(9f, 9f, 0f, true, true, 18f, 0f)
            // Latitude lines
            moveTo(3.6f, 9f)
            lineTo(20.4f, 9f)
            moveTo(3.6f, 15f)
            lineTo(20.4f, 15f)
            // Meridian ellipse
            moveTo(12f, 3f)
            curveTo(9f, 7f, 9f, 17f, 12f, 21f)
            moveTo(12f, 3f)
            curveTo(15f, 7f, 15f, 17f, 12f, 21f)
        }
    }

    val Tune: ImageVector by lazy {
        buildStrokeIcon("Tune", 2.0f) {
            moveTo(4f, 6f)
            lineTo(14f, 6f)
            moveTo(18f, 6f)
            lineTo(20f, 6f)
            moveTo(16f, 4f)
            lineTo(16f, 8f)

            moveTo(4f, 12f)
            lineTo(6f, 12f)
            moveTo(10f, 12f)
            lineTo(20f, 12f)
            moveTo(8f, 10f)
            lineTo(8f, 14f)

            moveTo(4f, 18f)
            lineTo(12f, 18f)
            moveTo(16f, 18f)
            lineTo(20f, 18f)
            moveTo(14f, 16f)
            lineTo(14f, 20f)
        }
    }

    val DeveloperBoard: ImageVector by lazy {
        buildStrokeIcon("DeveloperBoard", 2.0f) {
            moveTo(5f, 5f)
            lineTo(17f, 5f)
            lineTo(17f, 19f)
            lineTo(5f, 19f)
            close()
            // Pins
            moveTo(17f, 8f)
            lineTo(20f, 8f)
            moveTo(17f, 12f)
            lineTo(20f, 12f)
            moveTo(17f, 16f)
            lineTo(20f, 16f)
            // Inner core
            moveTo(8.5f, 9f)
            lineTo(13.5f, 9f)
            lineTo(13.5f, 15f)
            lineTo(8.5f, 15f)
            close()
        }
    }

    val Memory: ImageVector by lazy {
        buildStrokeIcon("Memory", 2.0f) {
            moveTo(7f, 7f)
            lineTo(17f, 7f)
            lineTo(17f, 17f)
            lineTo(7f, 17f)
            close()
            moveTo(10f, 10f)
            lineTo(14f, 10f)
            lineTo(14f, 14f)
            lineTo(10f, 14f)
            close()
            // Pins top/bottom/left/right
            moveTo(9f, 4f)
            lineTo(9f, 7f)
            moveTo(15f, 4f)
            lineTo(15f, 7f)
            moveTo(9f, 17f)
            lineTo(9f, 20f)
            moveTo(15f, 17f)
            lineTo(15f, 20f)
            moveTo(4f, 9f)
            lineTo(7f, 9f)
            moveTo(4f, 15f)
            lineTo(7f, 15f)
            moveTo(17f, 9f)
            lineTo(20f, 9f)
            moveTo(17f, 15f)
            lineTo(20f, 15f)
        }
    }

    val Cable: ImageVector by lazy {
        buildStrokeIcon("Cable", 2.1f) {
            // Paperclip / wire loop geometry
            moveTo(8f, 15f)
            lineTo(8f, 7f)
            curveTo(8f, 4.5f, 14f, 4.5f, 14f, 7f)
            lineTo(14f, 17f)
            curveTo(14f, 20f, 6f, 20f, 6f, 17f)
            lineTo(6f, 9f)
            curveTo(6f, 6.5f, 17f, 6.5f, 17f, 10f)
            lineTo(17f, 16f)
        }
    }

    val Vibration: ImageVector by lazy {
        buildStrokeIcon("Vibration", 2.0f) {
            // Center device
            moveTo(8f, 5f)
            lineTo(16f, 5f)
            lineTo(16f, 19f)
            lineTo(8f, 19f)
            close()
            // Haptic wave lines
            moveTo(4.5f, 8f)
            lineTo(4.5f, 16f)
            moveTo(2f, 10f)
            lineTo(2f, 14f)
            moveTo(19.5f, 8f)
            lineTo(19.5f, 16f)
            moveTo(22f, 10f)
            lineTo(22f, 14f)
        }
    }

    val Bolt: ImageVector by lazy {
        buildFilledIcon("Bolt") {
            moveTo(13f, 2f)
            lineTo(4.5f, 13.5f)
            lineTo(11f, 13.5f)
            lineTo(9.5f, 22f)
            lineTo(19.5f, 10f)
            lineTo(13f, 10f)
            close()
        }
    }

    val AutoAwesome: ImageVector by lazy {
        buildStrokeIcon("AutoAwesome", 2.0f) {
            moveTo(12f, 3f)
            lineTo(14f, 9f)
            lineTo(20f, 11f)
            lineTo(14f, 13f)
            lineTo(12f, 19f)
            lineTo(10f, 13f)
            lineTo(4f, 11f)
            lineTo(10f, 9f)
            close()
            moveTo(19f, 3f)
            lineTo(19f, 6f)
            moveTo(17.5f, 4.5f)
            lineTo(20.5f, 4.5f)
        }
    }

    val AutoMode: ImageVector by lazy {
        buildStrokeIcon("AutoMode", 2.0f) {
            // Circular sync arrows
            moveTo(4.5f, 10f)
            curveTo(5.5f, 6f, 9f, 4f, 13f, 4.5f)
            curveTo(16.5f, 5f, 19f, 7.5f, 19.5f, 11f)
            moveTo(17f, 8.5f)
            lineTo(19.5f, 11.5f)
            lineTo(21.5f, 8.5f)

            moveTo(19.5f, 14f)
            curveTo(18.5f, 18f, 15f, 20f, 11f, 19.5f)
            curveTo(7.5f, 19f, 5f, 16.5f, 4.5f, 13f)
            moveTo(7f, 15.5f)
            lineTo(4.5f, 12.5f)
            lineTo(2.5f, 15.5f)
        }
    }

    val Campaign: ImageVector by lazy {
        buildStrokeIcon("Campaign", 2.0f) {
            // Megaphone
            moveTo(4f, 10f)
            lineTo(9f, 10f)
            lineTo(15f, 6f)
            lineTo(15f, 18f)
            lineTo(9f, 14f)
            lineTo(4f, 14f)
            close()
            moveTo(6.5f, 14f)
            lineTo(7.5f, 19f)
            moveTo(18.5f, 9.5f)
            lineTo(21f, 8f)
            moveTo(18.5f, 12f)
            lineTo(21.5f, 12f)
            moveTo(18.5f, 14.5f)
            lineTo(21f, 16f)
        }
    }

    val TrendingUp: ImageVector by lazy {
        buildStrokeIcon("TrendingUp", 2.2f) {
            moveTo(3f, 17f)
            lineTo(9f, 11f)
            lineTo(13f, 15f)
            lineTo(21f, 7f)
            moveTo(15f, 7f)
            lineTo(21f, 7f)
            lineTo(21f, 13f)
        }
    }

    val Speed: ImageVector by lazy {
        buildStrokeIcon("Speed", 2.0f) {
            moveTo(4.5f, 18f)
            curveTo(3f, 14f, 4.5f, 8.5f, 8.5f, 6.5f)
            curveTo(12.5f, 4.5f, 17.5f, 6f, 19.5f, 10f)
            curveTo(20.5f, 12.5f, 20.5f, 15.5f, 19.5f, 18f)
            moveTo(12f, 14f)
            lineTo(16.5f, 9.5f)
        }
    }

    val Terminal: ImageVector by lazy {
        buildStrokeIcon("Terminal", 2.0f) {
            moveTo(3f, 5f)
            lineTo(21f, 5f)
            lineTo(21f, 19f)
            lineTo(3f, 19f)
            close()
            moveTo(7f, 9f)
            lineTo(10.5f, 12f)
            lineTo(7f, 15f)
            moveTo(12.5f, 15f)
            lineTo(17f, 15f)
        }
    }

    val Palette: ImageVector by lazy {
        buildStrokeIcon("Palette", 2.0f) {
            moveTo(12f, 3f)
            curveTo(7f, 3f, 3f, 7f, 3f, 12f)
            curveTo(3f, 17f, 7f, 21f, 12f, 21f)
            curveTo(13.5f, 21f, 14.5f, 20f, 14.5f, 18.5f)
            curveTo(14.5f, 17.2f, 13.8f, 16.5f, 13.8f, 15.5f)
            curveTo(13.8f, 14.5f, 14.8f, 13.8f, 16f, 13.8f)
            lineTo(17.5f, 13.8f)
            curveTo(19.5f, 13.8f, 21f, 12.2f, 21f, 10f)
            curveTo(21f, 6f, 17f, 3f, 12f, 3f)
            close()
            moveTo(7.5f, 10.5f)
            lineTo(7.6f, 10.5f)
            moveTo(10.5f, 7.5f)
            lineTo(10.6f, 7.5f)
            moveTo(14.5f, 7.5f)
            lineTo(14.6f, 7.5f)
        }
    }

    val FastForward: ImageVector by lazy {
        buildFilledIcon("FastForward") {
            moveTo(4f, 18f)
            lineTo(12.5f, 12f)
            lineTo(4f, 6f)
            close()
            moveTo(13f, 18f)
            lineTo(21.5f, 12f)
            lineTo(13f, 6f)
            close()
        }
    }

    val SkipNext: ImageVector by lazy {
        buildFilledIcon("SkipNext") {
            moveTo(6f, 18f)
            lineTo(14.5f, 12f)
            lineTo(6f, 6f)
            close()
            moveTo(16f, 6f)
            lineTo(18f, 6f)
            lineTo(18f, 18f)
            lineTo(16f, 18f)
            close()
        }
    }

    val Pause: ImageVector by lazy {
        buildFilledIcon("Pause") {
            moveTo(6f, 5f)
            lineTo(10f, 5f)
            lineTo(10f, 19f)
            lineTo(6f, 19f)
            close()
            moveTo(14f, 5f)
            lineTo(18f, 5f)
            lineTo(18f, 19f)
            lineTo(14f, 19f)
            close()
        }
    }

    val RestartAlt: ImageVector by lazy {
        buildStrokeIcon("RestartAlt", 2.1f) {
            moveTo(12f, 5f)
            curveTo(16f, 5f, 19f, 8f, 19f, 12f)
            curveTo(19f, 16f, 16f, 19f, 12f, 19f)
            curveTo(8.5f, 19f, 5.5f, 16.5f, 5f, 13f)
            moveTo(12f, 2.5f)
            lineTo(9f, 5f)
            lineTo(12f, 7.5f)
        }
    }

    val BookmarkAdd: ImageVector by lazy {
        buildStrokeIcon("BookmarkAdd", 2.0f) {
            moveTo(6f, 4f)
            lineTo(15f, 4f)
            moveTo(18f, 11f)
            lineTo(18f, 20f)
            lineTo(12f, 16.5f)
            lineTo(6f, 20f)
            close()
            moveTo(18f, 3f)
            lineTo(18f, 9f)
            moveTo(15f, 6f)
            lineTo(21f, 6f)
        }
    }

    val Remove: ImageVector by lazy {
        buildStrokeIcon("Remove", 2.4f) {
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }
}
