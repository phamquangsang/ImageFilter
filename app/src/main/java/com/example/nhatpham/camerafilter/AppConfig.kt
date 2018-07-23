package com.example.nhatpham.camerafilter

import com.example.nhatpham.camerafilter.models.Config

internal var STORAGE_DIR_NAME: String = ""

internal val NONE_CONFIG = Config("None", "")

internal val EFFECT_CONFIGS = ArrayList<Config>()

internal var PREVIEW_TYPE : PreviewType = PreviewType.Both