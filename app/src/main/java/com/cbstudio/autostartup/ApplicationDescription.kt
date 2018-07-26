package com.cbstudio.autostartup

import android.content.Intent
import android.graphics.drawable.Drawable


/**
 * Created by ImL1s on 2018/7/25.
 * Description:
 */
data class ApplicationDescription(
        val name: String,
        val packageName: String,
        val icon: Drawable,
        val intent: Intent?
)