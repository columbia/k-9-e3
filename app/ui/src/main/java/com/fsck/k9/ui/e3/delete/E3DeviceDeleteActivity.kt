package com.fsck.k9.ui.e3.delete

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import com.fsck.k9.ui.R
import com.fsck.k9.ui.e3.E3ActionBaseActivity
import kotlinx.android.synthetic.main.crypto_e3_device_delete.*
import kotlinx.android.synthetic.main.wizard_cancel_done.*
import org.koin.android.ext.android.inject
import timber.log.Timber

class E3DeviceDeleteActivity : E3ActionBaseActivity(), View.OnClickListener {
    private val presenter: E3DeviceDeletePresenter by inject {
        mapOf("lifecycleOwner" to this, "e3DeleteDeviceView" to this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crypto_e3_device_delete)

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT)

        cancel.setOnClickListener(this)
        done.setOnClickListener(this)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        presenter.initFromIntent(accountUuid)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            presenter.onClickHome()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    fun addDevicesToListView(devices: List<String>, listener: AdapterView.OnItemClickListener) {
        val phrasesAdapter = ArrayAdapter<String>(this, R.layout.crypto_e3_key_verify_phrase_row, devices)

        e3DeviceDeleteLayoutDevices.adapter = phrasesAdapter
        e3DeviceDeleteLayoutDevices.onItemClickListener = listener
    }

    fun sceneBegin() {
        e3DeviceDeleteLayoutInstructions.visibility = View.VISIBLE
        e3DeviceDeleteLayoutDevices.visibility = View.VISIBLE

        cancel.visibility = View.VISIBLE
        done.visibility = View.GONE
    }

    fun sceneFinished(nextKey: Boolean, transition: Boolean = false) {
        if (transition) {
            setupSceneTransition()
        }


    }

    fun populateListViewWithE3Devices(e3KeyIdNames: List<E3DeviceDeletePresenter.E3KeyIdName>, adapterListener: AdapterView.OnItemClickListener) {
        addDevicesToListView(e3KeyIdNames.map {
            it.e3KeyName ?: resources.getString(R.string.e3_device_delete_missing_device_name)
        }, adapterListener)

    }

    override fun onClick(v: View) {
        if (v.id == R.id.cancel) {
            setResult(Activity.RESULT_CANCELED)

            finish()
        } else if (v.id == R.id.done) {
            finish()
        }
    }

    companion object {
        private const val EXTRA_ACCOUNT = "account"

        fun createIntent(context: Context, accountUuid: String): Intent {
            val intent = Intent(context, E3DeviceDeleteActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT, accountUuid)
            return intent
        }
    }
}