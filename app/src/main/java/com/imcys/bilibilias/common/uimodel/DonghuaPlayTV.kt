package com.imcys.bilibilias.common.uimodel

import androidx.annotation.IdRes
import com.imcys.bilibilias.R

data class DonghuaPlayTV(
    val name: String,
    val officialUrl: String? = null,
    @field:IdRes val iconResId: Int? = null,
)

val playTVList = listOf(
    DonghuaPlayTV(
        name = "TOKYO MX",
        iconResId = R.drawable.ic_tokyo_mx_logo
    ),
    DonghuaPlayTV(
        name = "BS11",
        iconResId = R.drawable.ic_bs11_logo
    ),
    DonghuaPlayTV(
        name = "KBS京都",
        iconResId = R.drawable.ic_kbs_logo
    ),
    DonghuaPlayTV(
        name = "BS日テレ",
        iconResId = R.drawable.ic_bs_nihon_logo
    ),
    DonghuaPlayTV(
        name = "TBS",
        iconResId = R.drawable.ic_tbs_logo
    ),
    DonghuaPlayTV(
        name = "MBS",
        iconResId = R.drawable.ic_mbs_news_logo
    ),
    DonghuaPlayTV(
        name = "BS-TBS",
        iconResId = R.drawable.ic_bs_tbs_logo
    ),
    DonghuaPlayTV(
        name = "日本テレビ系",
        iconResId = R.drawable.ic_nippon_television_holdings_logo
    ),
    DonghuaPlayTV(
        name = "関西テレビ",
        iconResId = R.drawable.ic_jp_ktv_logo
    ),
    DonghuaPlayTV(
        name = "テレビ西日本",
        iconResId = R.drawable.ic_tnc_logo
    ),
    DonghuaPlayTV(
        name = "テレ東",
        iconResId = R.drawable.ic_tv_tokyo_logo
    ),
    DonghuaPlayTV(
        name = "テレビ東京",
        iconResId = R.drawable.ic_tv_tokyo_logo
    ),
)

val playProgramList = listOf(
    DonghuaPlayTV(
        name = "アニメイズム",
        iconResId = R.drawable.ic_animeism_new_logo
    ),
)