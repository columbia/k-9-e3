package com.fsck.k9.ui.crypto;


import android.content.Intent;
import android.content.IntentSender;


public interface MessageCryptoCallback<T> {
    void onCryptoHelperProgress(int current, int max);
    void onCryptoOperationsFinished(T annotations);
    void startPendingIntentForCryptoHelper(IntentSender si, int requestCode, Intent fillIntent,
            int flagsMask, int flagValues, int extraFlags);
}
