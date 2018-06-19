package com.fsck.k9.ui.e3

import org.koin.android.architecture.ext.viewModel
import org.koin.dsl.module.applicationContext

val e3KeyUploadUiModule = applicationContext {
    factory { E3KeyUploadSetupMessageLiveEvent(get()) }
    factory { E3KeyUploadMessageUploadLiveEvent() }
    factory { params ->
        E3KeyUploadPresenter(
                params["lifecycleOwner"],
                get(),
                get(parameters = { params.values }),
                get(),
                get(),
                get(),
                params["e3UploadView"])
    }
    viewModel { E3KeyUploadViewModel(get(), get()) }
}