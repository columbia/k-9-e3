package com.fsck.k9.e3;


import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.PendingCommandController;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.e3.smime.SMIMEDetectorPredicate;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

/**
 * Created on 1/9/2018.
 *
 * @author koh
 */

public class E3ForceEncryptFoldersAsyncTask extends AsyncTask<LocalFolder, Void, List<String>> {
    private static final int BATCH_SZ = 20;

    private final Account account;
    private final Function<MimeMessage, MimeMessage> encryptFunction;
    private final PendingCommandController pendingCommandController;
    private final Predicate<MimeMessage> isSMIMEPredicate;
    private final AlertDialog completedDialog;
    private final String alertStringFromCtx;

    public E3ForceEncryptFoldersAsyncTask(final Context context, final Account account, final Function<MimeMessage, MimeMessage> encryptFunction) {
        this.account = account;
        this.encryptFunction = encryptFunction;
        this.pendingCommandController = new PendingCommandController(context);
        this.isSMIMEPredicate = new SMIMEDetectorPredicate();
        this.completedDialog = new AlertDialog.Builder(context).create();
        this.alertStringFromCtx = context.getString(R.string.e3_force_encrypt_completed);
    }

    @Override
    protected void onPreExecute() {
        // TODO: Add a completedDialog indicator of some sort
        //completedDialog = ProgressDialog.show()
    }

    @Override
    protected List<String> doInBackground(LocalFolder... folders) {
        for (final LocalFolder folder : folders) {
            try {
                return encryptFolder(folder);
            } catch (final MessagingException e) {
                Timber.e(e, "Failed to get list of message UIDs in given folder: " + folder);
            }
        }

        return Collections.emptyList();
    }

    @Override
    protected void onPostExecute(final List<String> encryptedMessages) {
        completedDialog.setMessage(String.format(alertStringFromCtx, encryptedMessages.size()));
        completedDialog.show();
    }

    private List<String> encryptFolder(final LocalFolder folder) throws MessagingException {
        final List<String> encryptedSubjects = new ArrayList<>();
        final List<String> uids = folder.getAllMessageUids();

        final List<List<String>> partitions = Lists.partition(uids, BATCH_SZ);

        for (final List<String> partition : partitions) {
            final List<LocalMessage> batch = folder.getMessagesByUids(partition);
            final Iterable<LocalMessage> filtered = Collections2.filter(batch, Predicates.not(isSMIMEPredicate));

            for (final LocalMessage originalMsg : filtered) {
                final MimeMessage encryptedMsg = encryptFunction.apply(originalMsg);

                Preconditions.checkNotNull(encryptedMsg, "Failed to encrypt originalMsg: " + originalMsg);

                encryptedMsg.setFlag(Flag.E3, true);

                // Store the encrypted message locally
                final LocalMessage localMessage = folder.storeSmallMessage(encryptedMsg, new Runnable
                        () {
                    @Override
                    public void run() {
                        // TODO: add completedDialog?
                        //completedDialog.incrementAndGet();
                    }
                });

                final List<String> uidSingleton = Collections.singletonList(originalMsg.getUid());

                // First: Set \Deleted and \E3_DONE on the original message
                pendingCommandController.queueSetFlag(account, folder.getName(), true,
                        Flag.DELETED, uidSingleton);
                pendingCommandController.queueSetFlag(account, folder.getName(), true,
                        Flag.E3_DONE, uidSingleton);

                // Second: Move original to Gmail's trash folder
                pendingCommandController.queueMoveOrCopy(account, folder.getName(), false, uidSingleton);

                // Third: Append encrypted remotely
                pendingCommandController.queueAppend(account, folder.getName(), encryptedMsg.getUid());

                // Fourth: Queue empty trash (expunge) command
                pendingCommandController.queueEmptyTrash(account);

                encryptedSubjects.add(originalMsg.getSubject());
            }

            // Final: Run all the queued commands
            pendingCommandController.processPendingCommandsSynchronous(account, Collections.<MessagingListener>emptySet());
        }

        return encryptedSubjects;
    }
}
