package com.andrerinas.headunitrevived.utils

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.andrerinas.headunitrevived.aap.protocol.proto.Control
import kotlin.math.roundToInt

object HeadUnitScreenConfig {

    private var screenWidthPx: Int = 0
    private var screenHeightPx: Int = 0
    private var density: Float = 1.0f
    private var densityDpi: Int = 240
    private var isInitialized: Boolean = false

    var negotiatedResolutionType: Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType? = null
    private lateinit var currentSettings: Settings

    // System Insets (Bars/Cutouts)
    var systemInsetLeft: Int = 0
        private set
    var systemInsetTop: Int = 0
        private set
    var systemInsetRight: Int = 0
        private set
    var systemInsetBottom: Int = 0
        private set

    // Raw Screen Dimensions (Full Display)
    private var realScreenWidthPx: Int = 0
    private var realScreenHeightPx: Int = 0

    fun init(context: Context, displayMetrics: DisplayMetrics, settings: Settings) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val (screenWidth, screenHeight) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val size = android.graphics.Point()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(size)
            size.x to size.y
        }

        if (isInitialized && realScreenWidthPx == screenWidth && realScreenHeightPx == screenHeight &&
            this::currentSettings.isInitialized && currentSettings == settings) {
            return
        }

        isInitialized = true
        currentSettings = settings
        realScreenWidthPx = screenWidth
        realScreenHeightPx = screenHeight
        density = displayMetrics.density
        densityDpi = displayMetrics.densityDpi

        recalculate()
    }

    /**
     * Updates the usable screen area based on the actual measured view dimensions.
     * This ensures scaling and touch mapping are perfectly synchronized.
     */
    fun setActualUsableArea(width: Int, height: Int) {
        if (screenWidthPx == width && screenHeightPx == height) return
        AppLog.i("HeadUnitScreenConfig: Updating usable area to actual view size: ${width}x$height")
        screenWidthPx = width
        screenHeightPx = height
        // Trigger a recalculation of the negotiated resolution when the view size changes
        recalculate()
    }

    fun updateInsets(left: Int, top: Int, right: Int, bottom: Int) {
        if (systemInsetLeft == left && systemInsetTop == top && systemInsetRight == right && systemInsetBottom == bottom) {
            return
        }
        
        systemInsetLeft = left
        systemInsetTop = top
        systemInsetRight = right
        systemInsetBottom = bottom
        
        if (isInitialized) {
            recalculate()
        }
    }

    private fun recalculate() {
        // If we haven't been explicitly told the view size, calculate it based on screen and insets
        if (screenWidthPx == 0 || screenHeightPx == 0) {
            screenWidthPx = realScreenWidthPx - systemInsetLeft - systemInsetRight
            screenHeightPx = realScreenHeightPx - systemInsetTop - systemInsetBottom
        }

        if (screenWidthPx <= 0 || screenHeightPx <= 0) {
            screenWidthPx = realScreenWidthPx
            screenHeightPx = realScreenHeightPx
        }
        
        AppLog.i("HeadUnitScreenConfig: Calculated usable area: ${screenWidthPx}x$screenHeightPx")

        val selectedRes = Settings.Resolution.fromId(currentSettings.resolutionId)

        if (selectedRes == Settings.Resolution.AUTO || selectedRes == null) {
            negotiatedResolutionType = selectBestResolution(screenWidthPx, screenHeightPx)
        } else {
            negotiatedResolutionType = selectedRes.codec
        }

        AppLog.i("HeadUnitScreenConfig: Negotiated resolution: $negotiatedResolutionType (${getNegotiatedWidth()}x${getNegotiatedHeight()})")
    }

    private fun selectBestResolution(width: Int, height: Int): Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType {
        val isPortrait = height > width
        return if (isPortrait) {
            when {
                width >= 1440 && height >= 2560 -> Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._1440x2560
                width >= 1080 && height >= 1920 -> Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._1080x1920
                else -> Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._720x1280
            }
        } else {
            when {
                width >= 2560 && height >= 1440 -> Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._2560x1440
                width >= 1920 && height >= 1080 -> Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._1920x1080
                width >= 1280 && height >= 720 -> Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._1280x720
                else -> Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._800x480
            }
        }
    }

    fun getNegotiatedWidth(): Int = when (negotiatedResolutionType) {
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._800x480 -> 800
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._1280x720 -> 1280
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._1920x1080 -> 1920
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._2560x1440 -> 2560
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._720x1280 -> 720
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._1080x1920 -> 1080
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._1440x2560 -> 1440
        else -> 800
    }

    fun getNegotiatedHeight(): Int = when (negotiatedResolutionType) {
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._800x480 -> 480
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._1280x720 -> 720
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._1920x1080 -> 1080
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._2560x1440 -> 1440
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._720x1280 -> 1280
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._1080x1920 -> 1920
        Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._1440x2560 -> 2560
        else -> 480
    }

    /**
     * Calculates the width margin to send to the phone.
     * This helps the phone's UI fill the entire available screen width.
     */
    fun getWidthMargin(): Int {
        val nWidth = getNegotiatedWidth()
        val nHeight = getNegotiatedHeight()
        val sWidth = screenWidthPx
        val sHeight = screenHeightPx

        val scale = Math.min(sWidth.toFloat() / nWidth, sHeight.toFloat() / nHeight)
        val displayedWidth = (nWidth * scale).toInt()

        // If we are pillarboxing (bars on sides), we should tell the phone to use a wider area
        if (displayedWidth < sWidth) {
            val neededNWidth = (sWidth / scale).toInt()
            return ((neededNWidth - nWidth) / 2).coerceAtLeast(0)
        }
        return 0
    }

    /**
     * Calculates the height margin to send to the phone.
     * This helps the phone's UI fill the entire available screen height.
     */
    fun getHeightMargin(): Int {
        val nWidth = getNegotiatedWidth()
        val nHeight = getNegotiatedHeight()
        val sWidth = screenWidthPx
        val sHeight = screenHeightPx

        val scale = Math.min(sWidth.toFloat() / nWidth, sHeight.toFloat() / nHeight)
        val displayedHeight = (nHeight * scale).toInt()

        // If we are letterboxing (bars on top/bottom), we should tell the phone to use a taller area
        if (displayedHeight < sHeight) {
            val neededNHeight = (sHeight / scale).toInt()
            return ((neededNHeight - nHeight) / 2).coerceAtLeast(0)
        }
        return 0
    }

    fun getScaleX(): Float {
        val nWidth = getNegotiatedWidth().toDouble()
        val nHeight = getNegotiatedHeight().toDouble()
        val sWidth = screenWidthPx.toDouble()
        val sHeight = screenHeightPx.toDouble()

        if (nWidth == 0.0 || nHeight == 0.0 || sWidth == 0.0 || sHeight == 0.0) return 1.0f

        val scale = Math.min(sWidth / nWidth, sHeight / nHeight)
        return ((nWidth * scale) / sWidth).toFloat()
    }

    fun getScaleY(): Float {
        val nWidth = getNegotiatedWidth().toDouble()
        val nHeight = getNegotiatedHeight().toDouble()
        val sWidth = screenWidthPx.toDouble()
        val sHeight = screenHeightPx.toDouble()

        if (nWidth == 0.0 || nHeight == 0.0 || sWidth == 0.0 || sHeight == 0.0) return 1.0f

        val scale = Math.min(sWidth / nWidth, sHeight / nHeight)
        return ((nHeight * scale) / sHeight).toFloat()
    }

    fun getTouchX(rawX: Float): Int {
        val nWidth = getNegotiatedWidth().toDouble()
        val nHeight = getNegotiatedHeight().toDouble()
        val sWidth = screenWidthPx.toDouble()
        val sHeight = screenHeightPx.toDouble()

        if (nWidth == 0.0 || nHeight == 0.0 || sWidth == 0.0 || sHeight == 0.0) return 0

        val scale = Math.min(sWidth / nWidth, sHeight / nHeight)
        val displayedWidth = nWidth * scale
        val offsetX = (sWidth - displayedWidth) / 2.0

        val correctedX = Math.round((rawX - offsetX) * (nWidth / displayedWidth)).toInt()
        return correctedX.coerceIn(0, nWidth.toInt() - 1)
    }

    fun getTouchY(rawY: Float): Int {
        val nWidth = getNegotiatedWidth().toDouble()
        val nHeight = getNegotiatedHeight().toDouble()
        val sWidth = screenWidthPx.toDouble()
        val sHeight = screenHeightPx.toDouble()

        if (nWidth == 0.0 || nHeight == 0.0 || sWidth == 0.0 || sHeight == 0.0) return 0

        val scale = Math.min(sWidth / nWidth, sHeight / nHeight)
        val displayedHeight = nHeight * scale
        val offsetY = (sHeight - displayedHeight) / 2.0

        val correctedY = Math.round((rawY - offsetY) * (nHeight / displayedHeight)).toInt()
        return correctedY.coerceIn(0, nHeight.toInt() - 1)
    }

    fun getDensityDpi(): Int {
        return if (this::currentSettings.isInitialized && currentSettings.dpiPixelDensity != 0) {
            currentSettings.dpiPixelDensity
        } else {
            densityDpi
        }
    }
}
