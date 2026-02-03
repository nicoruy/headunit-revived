package com.andrerinas.headunitrevived.view

import android.view.View
import com.andrerinas.headunitrevived.App
import com.andrerinas.headunitrevived.utils.AppLog
import com.andrerinas.headunitrevived.utils.HeadUnitScreenConfig

object ProjectionViewScaler {

    fun updateScale(view: View, videoWidth: Int, videoHeight: Int) {
        if (videoWidth == 0 || videoHeight == 0 || view.width == 0 || view.height == 0) {
            return
        }

        val displayMetrics = view.resources.displayMetrics
        HeadUnitScreenConfig.init(view.context, displayMetrics, App.provide(view.context).settings)
        
        // Ensure touch logic uses the same base dimensions as the video scaling
        HeadUnitScreenConfig.setActualUsableArea(view.width, view.height)

        val screenWidth = view.width.toFloat()
        val screenHeight = view.height.toFloat()

        // Calculate scale to fit video into screen while maintaining aspect ratio
        val scale = Math.min(screenWidth / videoWidth, screenHeight / videoHeight)
        
        val finalScaleX = (videoWidth * scale) / screenWidth
        val finalScaleY = (videoHeight * scale) / screenHeight

        view.scaleX = finalScaleX
        view.scaleY = finalScaleY

        AppLog.i("ProjectionViewScaler: Video: ${videoWidth}x$videoHeight, View: ${view.width}x${view.height}, Scale: $finalScaleX x $finalScaleY")
    }
}
