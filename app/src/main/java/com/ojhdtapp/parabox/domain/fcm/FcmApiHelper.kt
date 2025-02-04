package com.ojhdtapp.parabox.domain.fcm

import android.content.Context
import android.util.Log
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

class FcmApiHelper @Inject constructor(
    val context: Context,
    private val fcmService: FcmService
) {
    suspend fun getVersion(): Response<FcmConnectionResponse>? = coroutineScope {
        val url =
            if (context.dataStore.data.first()[DataStoreKeys.SETTINGS_ENABLE_FCM_CUSTOM_URL] == true) {
                context.dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_URL] ?: ""
            } else {
                context.dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_OFFICIAL_URL] ?: ""
            }
        if (url.isNotBlank()) {
            val httpUrl = "http://${url}/"
            try {
                fcmService.getVersion(httpUrl).also {
                    if (it.isSuccessful) {
                        Log.d("parabox", "check connection success")
                    } else {
                        Log.d("parabox", "check connection failed")
                    }
                }
            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
                null
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        } else null
    }

    suspend fun pushReceiveDto(
        receiveMessageDto: ReceiveMessageDto,
    ) = coroutineScope {
        val url =
            if (context.dataStore.data.first()[DataStoreKeys.SETTINGS_ENABLE_FCM_CUSTOM_URL] == true) {
                context.dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_URL] ?: ""
            } else {
                context.dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_OFFICIAL_URL] ?: ""
            }
        val tokensSet =
            context.dataStore.data.first()[DataStoreKeys.FCM_TARGET_TOKENS] ?: emptySet()
        if (url.isNotBlank() && tokensSet.isNotEmpty()) {
            val httpUrl = "http://${url}/receive/"
            try {
                fcmService.pushReceiveDto(
                    httpUrl,
                    FcmReceiveModel(
                        receiveMessageDto,
                        receiveMessageDto.getFcmNotification(),
                        tokensSet
                    )
                )
            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
                null
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        } else null
    }

    suspend fun pushSendDto(
        sendMessageDto: SendMessageDto
    ) = coroutineScope {
        val url =
            if (context.dataStore.data.first()[DataStoreKeys.SETTINGS_ENABLE_FCM_CUSTOM_URL] == true) {
                context.dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_URL] ?: ""
            } else {
                context.dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_OFFICIAL_URL] ?: ""
            }
        val loopbackToken =
            context.dataStore.data.first()[DataStoreKeys.FCM_LOOPBACK_TOKEN] ?: ""
        if (url.isNotBlank() && loopbackToken.isNotBlank()) {
            val httpUrl = "http://${url}/send/"
            try {
                fcmService.pushSendDto(httpUrl, FcmSendModel(sendMessageDto, loopbackToken))
            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
                null
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        } else null
    }
}