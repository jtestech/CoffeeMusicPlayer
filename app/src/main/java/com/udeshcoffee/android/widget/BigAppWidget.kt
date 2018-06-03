package com.udeshcoffee.android.widget

import android.appwidget.AppWidgetManager
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Environment
import android.view.View
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.udeshcoffee.android.R
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.ArtworkURI

/**
 * Implementation of App Widget functionality.
 */
class BigAppWidget : BaseAppWidget() {

    override val widgetTheme = WIDGET_THEME
    override val widgetButtons = WIDGET_BUTTONS
    override val widgetAlpha = WIDGET_ALPHA

    override val layoutId = R.layout.widget_big

    override fun onMetadataChange(context: Context, appWidgetManager: AppWidgetManager, id: Int, views: RemoteViews, song: Song) {
        super.onMetadataChange(context, appWidgetManager, id, views, song)
        val titleName = song.title
        val albumName = song.albumName
        val artistName = song.artistName
        var errorState: CharSequence? = null

        // Format title string with track number, or show SD card message
        val status = Environment.getExternalStorageState()
        if (status == Environment.MEDIA_SHARED || status == Environment.MEDIA_UNMOUNTED) {
            errorState = if (Environment.isExternalStorageRemovable()) {
                "SD Card Busy"
            } else {
                "No SD Card"
            }
        } else if (status == Environment.MEDIA_REMOVED) {
            errorState = if (Environment.isExternalStorageRemovable()) {
                "No Title"
            } else {
                "No SD Card"
            }
        }

        if (errorState != null) {
            // Show error state to user
            views.setViewVisibility(R.id.text1, View.GONE)
            views.setViewVisibility(R.id.text2, View.GONE)
            views.setTextViewText(R.id.text3, errorState)

        } else {
            // No error, so show normal titles
            views.setViewVisibility(R.id.text1, View.VISIBLE)
            views.setViewVisibility(R.id.text2, View.VISIBLE)
            views.setTextViewText(R.id.text1, titleName)
            views.setTextViewText(R.id.text2, albumName)
            views.setTextViewText(R.id.text3, artistName)
        }

        views.setImageViewResource(R.id.album_art, R.drawable.default_art)

        val uri = ContentUris.withAppendedId(ArtworkURI, song.albumId)

        Glide.with(context).asBitmap().load(uri)
                .into(object : SimpleTarget<Bitmap>(180, 180) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        views.setImageViewBitmap(R.id.album_art, resource)
                        setStyles(context, views, id)
                        appWidgetManager.updateAppWidget(id, views)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        views.setImageViewResource(R.id.album_art, R.drawable.default_art)
                        setStyles(context, views, id)
                        appWidgetManager.updateAppWidget(id, views)
                    }

                })
    }

    companion object {
        const val WIDGET_THEME = "bigwidgettheme"
        const val WIDGET_BUTTONS = "bigwidgetbuttons"
        const val WIDGET_ALPHA = "bigwidgetalpha"

        private val mInstance: BigAppWidget = BigAppWidget()

        @Synchronized
        fun getInstance(): BigAppWidget {
            return mInstance
        }
    }
}

