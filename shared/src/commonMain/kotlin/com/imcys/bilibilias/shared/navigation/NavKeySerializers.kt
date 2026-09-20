package com.imcys.bilibilias.shared.navigation

import androidx.navigation3.runtime.NavKey
import com.imcys.bilibilias.shared.feature.analysis.navigation.AnalysisRoute
import com.imcys.bilibilias.shared.feature.analysis.videocodeing.VideoCodingInfoRoute
import com.imcys.bilibilias.shared.feature.download.navigation.DownloadRoute
import com.imcys.bilibilias.shared.feature.event.playvoucher.navigation.PlayVoucherErrorRoute
import com.imcys.bilibilias.shared.feature.event.requestFrequent.RequestFrequentRoute
import com.imcys.bilibilias.shared.feature.home.navigation.HomeRoute
import com.imcys.bilibilias.shared.feature.login.CookieLoginRoute
import com.imcys.bilibilias.shared.feature.login.navigation.LoginRoute
import com.imcys.bilibilias.shared.feature.login.navigation.QRCodeLoginRoute
import com.imcys.bilibilias.shared.feature.setting.about.AboutRoute
import com.imcys.bilibilias.shared.feature.setting.complaint.ComplaintRoute
import com.imcys.bilibilias.shared.feature.setting.contract.NamingConventionRoute
import com.imcys.bilibilias.shared.feature.setting.developer.LineConfigRoute
import com.imcys.bilibilias.shared.feature.setting.download.DownloadConfigRoute
import com.imcys.bilibilias.shared.feature.setting.expand.SystemExpandRoute
import com.imcys.bilibilias.shared.feature.setting.layout.LayoutTypesetRoute
import com.imcys.bilibilias.shared.feature.setting.navigation.RoamRoute
import com.imcys.bilibilias.shared.feature.setting.navigation.SettingRoute
import com.imcys.bilibilias.shared.feature.setting.platform.ParsePlatformRoute
import com.imcys.bilibilias.shared.feature.setting.storage.StorageManagementRoute
import com.imcys.bilibilias.shared.feature.setting.version.AppVersionInfoRoute
import com.imcys.bilibilias.shared.feature.tools.calendar.CalendarRoute
import com.imcys.bilibilias.shared.feature.tools.calendar.detail.SubjectDetailRoute
import com.imcys.bilibilias.shared.feature.tools.donate.DonateRoute
import com.imcys.bilibilias.shared.feature.tools.parser.WebParserRoute
import com.imcys.bilibilias.shared.feature.user.bangumifollow.BangumiFollowRoute
import com.imcys.bilibilias.shared.feature.user.folder.UserFolderRoute
import com.imcys.bilibilias.shared.feature.user.history.UserPlayHistoryRoute
import com.imcys.bilibilias.shared.feature.user.like.LikeVideoRoute
import com.imcys.bilibilias.shared.feature.user.navigation.UserRoute
import com.imcys.bilibilias.shared.feature.user.work.WorkListRoute
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

internal val navKeySerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        navKey(AboutRoute.serializer())
        navKey(AnalysisRoute.serializer())
        navKey(AppVersionInfoRoute.serializer())
        navKey(BangumiFollowRoute.serializer())
        navKey(CalendarRoute.serializer())
        navKey(ComplaintRoute.serializer())
        navKey(CookieLoginRoute.serializer())
        navKey(DownloadConfigRoute.serializer())
        navKey(DownloadRoute.serializer())
        navKey(DonateRoute.serializer())
        navKey(HomeRoute.serializer())
        navKey(LayoutTypesetRoute.serializer())
        navKey(LikeVideoRoute.serializer())
        navKey(LineConfigRoute.serializer())
        navKey(LoginRoute.serializer())
        navKey(NamingConventionRoute.serializer())
        navKey(ParsePlatformRoute.serializer())
        navKey(PlayVoucherErrorRoute.serializer())
        navKey(QRCodeLoginRoute.serializer())
        navKey(RequestFrequentRoute.serializer())
        navKey(RoamRoute.serializer())
        navKey(SettingRoute.serializer())
        navKey(StorageManagementRoute.serializer())
        navKey(SubjectDetailRoute.serializer())
        navKey(SystemExpandRoute.serializer())
        navKey(UserFolderRoute.serializer())
        navKey(UserPlayHistoryRoute.serializer())
        navKey(UserRoute.serializer())
        navKey(VideoCodingInfoRoute.serializer())
        navKey(WebParserRoute.serializer())
        navKey(WorkListRoute.serializer())
    }
}

private inline fun <reified T> PolymorphicModuleBuilder<NavKey>.navKey(
    serializer: KSerializer<T>
) where T : NavKey {
    subclass(serializer)
}
