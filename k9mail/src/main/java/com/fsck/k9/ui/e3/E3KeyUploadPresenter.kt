package com.fsck.k9.ui.e3

import android.app.PendingIntent
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mail.TransportProvider
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.openintents.openpgp.OpenPgpApiManager
import timber.log.Timber

class E3KeyUploadPresenter internal constructor(
        lifecycleOwner: LifecycleOwner,
        private val context: Context,
        private val openPgpApiManager: OpenPgpApiManager,
        private val transportProvider: TransportProvider,
        private val preferences: Preferences,
        private val viewModel: E3KeyUploadViewModel,
        private val view: E3KeyUploadActivity
) {

    private lateinit var account: Account
    private lateinit var showTransferCodePi: PendingIntent

    init {
        viewModel.e3KeyUploadSetupMessageLiveEvent.observe(lifecycleOwner, Observer { msg -> msg?.let { onEventE3KeyUploadSetupMessage(it) } })

        // Actually send the message now
        //viewModel.autocryptSetupTransferLiveEvent.observe(lifecycleOwner, Observer { pi -> onLoadedAutocryptSetupTransfer(pi) })
        // TODO: remove when the rest is implemented
        view.sceneBegin()
    }

    fun initFromIntent(accountUuid: String?) {
        if (accountUuid == null) {
            view.finishWithInvalidAccountError()
            return
        }

        account = preferences.getAccount(accountUuid)

        openPgpApiManager.setOpenPgpProvider(account.e3Provider, object : OpenPgpApiManager.OpenPgpApiManagerCallback {
            override fun onOpenPgpProviderStatusChanged() {
                if (openPgpApiManager.openPgpProviderState == OpenPgpApiManager.OpenPgpProviderState.UI_REQUIRED) {
                    view.finishWithProviderConnectError(openPgpApiManager.readableOpenPgpProviderName)
                }
            }

            override fun onOpenPgpProviderError(error: OpenPgpApiManager.OpenPgpProviderError) {
                view.finishWithProviderConnectError(openPgpApiManager.readableOpenPgpProviderName)
            }
        })

        view.setAddress(account.identities[0].email)

        //viewModel.autocryptSetupTransferLiveEvent.recall()
    }

    fun onClickHome() {
        view.finishAsCancelled()
    }

    fun onClickUpload() {
        view.sceneGeneratingAndUploading()

        launch(UI) {
            view.uxDelay()
            view.setLoadingStateGenerating()

            viewModel.e3KeyUploadSetupMessageLiveEvent.loadE3KeyUploadMessageAsync(openPgpApiManager.openPgpApi, account)
        }
    }

    private fun onEventE3KeyUploadSetupMessage(setupMsg: E3KeyUploadMessage) {
        view.setLoadingStateSending()
        view.sceneGeneratingAndUploading()

        //val transport = transportProvider.getTransport(context, account)
        //viewModel.autocryptSetupTransferLiveEvent.sendMessageAsync(transport, setupMsg)
    }
/*
    private fun onLoadedAutocryptSetupTransfer(result: AutocryptSetupTransferResult?) {
        when (result) {
            null -> view.sceneBegin()
            is AutocryptSetupTransferResult.Success -> {
                showTransferCodePi = result.showTransferCodePi
                view.setLoadingStateFinished()
                view.sceneFinished()
            }
            is AutocryptSetupTransferResult.Failure -> {
                Timber.e(result.exception, "Error sending setup message")
                view.setLoadingStateSendingFailed()
                view.sceneSendError()
            }
        }
    }
    */
}