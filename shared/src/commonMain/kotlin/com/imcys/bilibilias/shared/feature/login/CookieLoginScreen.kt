package com.imcys.bilibilias.shared.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import bilibilias.shared.generated.resources.Res
import bilibilias.shared.generated.resources.cd_back
import bilibilias.shared.generated.resources.login_confirm_account
import bilibilias.shared.generated.resources.login_cookie_input_label
import bilibilias.shared.generated.resources.login_cookie_title
import bilibilias.shared.generated.resources.login_cookie_warning
import bilibilias.shared.generated.resources.login_level_format
import bilibilias.shared.generated.resources.login_saving_info
import bilibilias.shared.generated.resources.login_saving_progress
import com.imcys.bilibilias.network.ApiStatus
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.data.model.BILILoginUserModel
import com.imcys.bilibilias.shared.platform.firebase.FirebaseExt
import com.imcys.bilibilias.ui.component.ASAlertDialog
import com.imcys.bilibilias.ui.component.ASAsyncImage
import com.imcys.bilibilias.ui.component.ASIconButton
import com.imcys.bilibilias.ui.component.ASTopAppBar
import com.imcys.bilibilias.ui.component.BILIBILIASTopAppBarStyle
import com.imcys.bilibilias.ui.component.shimmer.shimmer
import com.imcys.bilibilias.shared.ui.component.ASAgreePrivacyPolicy
import com.imcys.bilibilias.shared.ui.component.AsAutoError
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel


@Serializable
data object CookieLoginRoute : NavKey

@Composable
fun CookieLoginScreen(cookieLoginRoute: CookieLoginRoute, onToBack: () -> Unit, onFinish: () -> Unit) {
    val vm = koinViewModel<CookieLoginViewModel>()
    val userInfo by vm.loginUserInfoState.collectAsStateWithLifecycle()
    CookieLoginScaffold(onToBack) { paddingValues ->
        CookieLoginContent(
            userInfo = userInfo,
            onCheckCookies = vm::checkCookies,
            onSaveLoginCookie = vm::saveLoginCookie,
            paddingValues = paddingValues,
            onFinish = onFinish
        )
    }
}

@Composable
fun CookieLoginContent(
    userInfo: NetWorkResult<BILILoginUserModel?>,
    onCheckCookies: (String) -> Unit,
    onSaveLoginCookie: suspend () -> Unit,
    paddingValues: PaddingValues,
    onFinish: () -> Unit
) {
    var cookiesString by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var agreePrivacyPolicy by remember { mutableStateOf(false) }
    var showSavingDialog by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(paddingValues)
            .padding(vertical = 10.dp)
            .padding(horizontal = 10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            enabled = agreePrivacyPolicy,
            modifier = Modifier.fillMaxWidth(),
            value = cookiesString,
            onValueChange = {
                cookiesString = it
                onCheckCookies(it)
            },
            label = { Text(stringResource(Res.string.login_cookie_input_label)) },
            maxLines = 4,
            minLines = 4
        )

        Spacer(Modifier.height(10.dp))

        ASAgreePrivacyPolicy(agreePrivacyPolicy, onClick = {
            agreePrivacyPolicy = !agreePrivacyPolicy
        })

        Spacer(Modifier.height(10.dp))
        // 用户卡片显示
        AsAutoError(
            userInfo,
            onDefaultContent = {},
            onSuccessContent = {
                if (!agreePrivacyPolicy) return@AsAutoError
                Column {
                    UserCard(
                        Modifier.shimmer(userInfo.status == ApiStatus.LOADING),
                        mid = userInfo.data?.mid ?: 0L,
                        name = userInfo.data?.name ?: "",
                        level = userInfo.data?.level ?: 0,
                        face = userInfo.data?.face ?: ""
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CardDefaults.shape, onClick = {
                            showSavingDialog = true
                            scope.launch {
                                FirebaseExt.logLogin("Cookie")
                                onSaveLoginCookie()
                                showSavingDialog = false
                                onFinish()
                            }
                        }) {
                        Text(stringResource(Res.string.login_confirm_account))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        )

        // 提示
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(stringResource(Res.string.login_cookie_warning))
        }

        SavingDialog(showSavingDialog)

    }


}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SavingDialog(show: Boolean) {
    ASAlertDialog(showState = show, title = {
        Text(stringResource(Res.string.login_saving_info))
    }, text = {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ContainedLoadingIndicator()
            Text(text = stringResource(Res.string.login_saving_progress))
        }
    }, confirmButton = {})
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UserCard(modifier: Modifier = Modifier, mid: Long, name: String, level: Int, face: String) {
    Surface(
        modifier = modifier.height(IntrinsicSize.Min), shape = CardDefaults.shape
    ) {
        Row(Modifier.padding(10.dp)) {
            Column(
                Modifier.weight(0.2f)

            ) {
                ASAsyncImage(
                    model = face,
                    shape = MaterialShapes.Cookie12Sided.toShape(),
                    contentDescription = "头像",
                    modifier = Modifier
                        .aspectRatio(1f)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(
                Modifier
                    .fillMaxHeight()
                    .weight(0.8f)
            ) {
                Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(stringResource(Res.string.login_level_format, level))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CookieLoginScaffold(onToBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column {
                ASTopAppBar(
                    style = BILIBILIASTopAppBarStyle.Small,
                    title = {
                        Text(stringResource(Res.string.login_cookie_title))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    navigationIcon = {
                        ASIconButton(onClick = {
                            onToBack.invoke()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(Res.string.cd_back)
                            )
                        }
                    }
                )
            }
        },
    ) {
        content.invoke(it)
    }
}
