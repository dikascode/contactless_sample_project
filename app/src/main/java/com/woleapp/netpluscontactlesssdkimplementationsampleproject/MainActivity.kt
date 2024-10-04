package com.woleapp.netpluscontactlesssdkimplementationsampleproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.clearPinKey
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.netpluspay.contactless.sdk.start.ContactlessSdk
import com.netpluspay.contactless.sdk.utils.ContactlessReaderResult
import com.netpluspay.nibssclient.models.IsoAccountType
import com.netpluspay.nibssclient.models.MakePaymentParams
import com.netpluspay.nibssclient.models.UserData
import com.netpluspay.nibssclient.service.NetposPaymentClient
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.CARD_HOLDER_NAME
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.CLEAR_PIN_KEY
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.CONFIG_DATA
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.ERROR_TAG
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.KEY_HOLDER
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.PAYMENT_ERROR_DATA_TAG
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.PAYMENT_SUCCESS_DATA_TAG
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.POS_ENTRY_MODE
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.TAG_CHECK_BALANCE
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.TAG_MAKE_PAYMENT
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.TAG_TERMINAL_CONFIGURATION
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.getSampleUserData
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.getSavedKeyHolder
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.dialog.LoadingDialog
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.models.CardResult
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.models.Status
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private val gson: Gson = Gson()
    private lateinit var makePaymentButton: Button
    private lateinit var checkBalanceButton: Button
    private lateinit var resultViewerTextView: TextView
    private lateinit var amountET: EditText
    private var userData: UserData = getSampleUserData()
    private var cardData: CardData? = null
    private var previousAmount: Long? = null
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    var netposPaymentClient: NetposPaymentClient = NetposPaymentClient
    val loaderDialog: LoadingDialog = LoadingDialog()
    private val makePaymentResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            if (result.resultCode == ContactlessReaderResult.RESULT_OK) {
                data?.let { i ->
                    val amountToPay = amountET.text.toString().toLong()
                    amountET.text.clear()
                    val cardReadData = i.getStringExtra("data")!!
                    val cardResult = gson.fromJson(cardReadData, CardResult::class.java)

                    Log.d("card_data_result", gson.toJson(cardResult))
                    makePayment(cardResult, amountToPay)
                }
            }
            if (result.resultCode == ContactlessReaderResult.RESULT_ERROR) {
                data?.let { i ->
                    val error = i.getStringExtra("data")
                    error?.let {
                        Timber.d("ERROR_TAG===>%s", it)
                        resultViewerTextView.text = it
                    }
                }
            }
        }

    private val checkBalanceResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            if (result.resultCode == ContactlessReaderResult.RESULT_OK) {
                data?.let { i ->
                    val cardReadData = i.getStringExtra("data")!!
                    val cardResult = gson.fromJson(cardReadData, CardResult::class.java)
                    checkBalance(cardResult)
                }
            }
            if (result.resultCode == ContactlessReaderResult.RESULT_ERROR) {
                data?.let { i ->
                    val error = i.getStringExtra("data")
                    error?.let {
                        Timber.d("ERROR_TAG===>%s", it)
                        resultViewerTextView.text = it
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Initialize Views
        initializeViews()
        configureTerminal()
        netposPaymentClient.logUser(this, gson.toJson(userData))
        Timber.d("DEVICE_SERIAL_NUMBER===>%s", getSampleUserData().terminalSerialNumber)
        makePaymentButton.setOnClickListener {
            resultViewerTextView.text = ""
            if (amountET.text.isNullOrEmpty() || amountET.text.toString().toLong() < 100L) {
                Toast.makeText(this, getString(R.string.enter_valid_amount), Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
            val amountToPay = amountET.text.toString().toLong().toDouble()

            launchContactless(makePaymentResultLauncher, amountToPay)
        }

        checkBalanceButton.setOnClickListener {
            launchContactless(checkBalanceResultLauncher, 100.0)
        }
    }

    private fun launchContactless(
        launcher: ActivityResultLauncher<Intent>,
        amountToPay: Double,
        cashBackAmount: Double = 0.0,
    ) {
        val savedKeyHolder = getSavedKeyHolder()

        savedKeyHolder?.run {
            ContactlessSdk.readContactlessCard(
                this@MainActivity,
                launcher,
                this.clearPinKey, // "86CBCDE3B0A22354853E04521686863D" // pinKey
                amountToPay, // amount
                cashBackAmount, // cashbackAmount(optional)
            )
        } ?: run {
            Toast.makeText(
                this,
                getString(R.string.terminal_not_configured),
                Toast.LENGTH_LONG,
            ).show()
            configureTerminal()
        }
    }

    private fun initializeViews() {
        makePaymentButton = findViewById(R.id.read_card_btn)
        resultViewerTextView = findViewById(R.id.result_tv)
        amountET = findViewById(R.id.amountToPay)
        checkBalanceButton = findViewById(R.id.check_balance)
    }

    private fun configureTerminal() {
        val loaderDialog: LoadingDialog = LoadingDialog()
        loaderDialog.loadingMessage = getString(R.string.configuring_terminal)
        loaderDialog.show(supportFragmentManager, TAG_TERMINAL_CONFIGURATION)
        compositeDisposable.add(
            netposPaymentClient.init(this, Gson().toJson(userData))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data, error ->
                    data?.let { response ->
                        Toast.makeText(
                            this,
                            getString(R.string.terminal_configured),
                            Toast.LENGTH_LONG,
                        ).show()
                        loaderDialog.dismiss()
                        val keyHolder = response.first
                        val configData = response.second
                        val pinKey = keyHolder?.clearPinKey
                        if (pinKey != null) {
                            Prefs.putString(KEY_HOLDER, gson.toJson(keyHolder))
                            Prefs.putString(CONFIG_DATA, gson.toJson(configData))
                            Prefs.putString(CLEAR_PIN_KEY, gson.toJson(pinKey))
                        }
                    }
                    error?.let {
                        Toast.makeText(
                            this,
                            getString(R.string.terminal_config_failed),
                            Toast.LENGTH_LONG,
                        ).show()
                        loaderDialog.dismiss()
                        Timber.d("%s%s", ERROR_TAG, it.localizedMessage)
                    }
                },
        )
    }

    private fun makePayment(cardResult: CardResult, amountToPay: Long) {
        val loaderDialog: LoadingDialog = LoadingDialog()
        loaderDialog.loadingMessage = getString(R.string.processing_payment)
        loaderDialog.show(supportFragmentManager, TAG_MAKE_PAYMENT)
        val cardData = cardResult.cardReadResult.let {
            CardData(it.track2Data, it.iccString, it.pan, POS_ENTRY_MODE)
        }

        val makePaymentParams =
            cardData.let { cdData ->
                previousAmount = amountToPay
                MakePaymentParams(
                    amount = amountToPay,
                    terminalId = userData.terminalId,
                    cardData = cdData,
                    accountType = IsoAccountType.SAVINGS,
                )
            }

        val credentials = JsonObject().apply {
            addProperty("terminalId", userData.terminalId)

            try {
                add("makeParams", JsonParser.parseString(Gson().toJson(makePaymentParams)))
                add("keyHolder", JsonParser.parseString(Prefs.getString(KEY_HOLDER)))
                add("configData", JsonParser.parseString(Prefs.getString(CONFIG_DATA)))
                add("clearPinKey", JsonParser.parseString(Prefs.getString(CLEAR_PIN_KEY)))
            } catch (e: JsonSyntaxException) {
                Log.e("JSON Error", "Error parsing JSON", e)
            }
            addProperty("cardScheme", cardResult.cardScheme)
            addProperty("cardHolderName", CARD_HOLDER_NAME)
        }


        compositeDisposable.add(
            PosClient.getInstance().posTransaction(credentials)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { response ->
                        loaderDialog.dismiss()

                        if (response.isSuccessful) {
                            val jsonObject = response.body()

                            val stringBuilder = StringBuilder()


                            jsonObject?.entrySet()?.forEach { entry ->
                                val key = entry.key
                                val value = entry.value

                                if (value.isJsonPrimitive) {
                                    val primitiveValue = value.asJsonPrimitive
                                    val displayValue = when {
                                        primitiveValue.isString -> primitiveValue.asString
                                        primitiveValue.isNumber -> primitiveValue.asNumber.toString()
                                        primitiveValue.isBoolean -> primitiveValue.asBoolean.toString()
                                        else -> "Unknown value type"
                                    }

                                    stringBuilder.append("$key: $displayValue\n")
                                } else if (value.isJsonNull) {
                                    stringBuilder.append("$key: null\n")
                                } else {
                                    stringBuilder.append("$key: Complex type (not displayed)\n")
                                }
                            }


                            resultViewerTextView.text = stringBuilder.toString()
                        } else {

                            resultViewerTextView.text = "Error: ${response.code()}"
                        }
                    },
                    { throwable ->
                        loaderDialog.dismiss()
                        resultViewerTextView.text = throwable.localizedMessage
                        Log.d("$PAYMENT_ERROR_DATA_TAG%s", throwable.localizedMessage)
                    }
                )
        )

//        val makePaymentParams =
//            cardData.let { cdData ->
//                previousAmount = amountToPay
//                MakePaymentParams(
//                    amount = amountToPay,
//                    terminalId = userData.terminalId,
//                    cardData = cdData,
//                    accountType = IsoAccountType.SAVINGS,
//                )
//            }
//        cardData.pinBlock = cardResult.cardReadResult.pinBlock
//        compositeDisposable.add(
//            netposPaymentClient.makePayment(
//                this,
//                userData.terminalId,
//                gson.toJson(makePaymentParams),
//                cardResult.cardScheme,
//                CARD_HOLDER_NAME,
//                "TESTING_TESTING",
//            ).subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(
//                    { transactionWithRemark ->
//                        loaderDialog.dismiss()
//                        resultViewerTextView.text = gson.toJson(transactionWithRemark)
//                        Log.d(
//                            "$PAYMENT_SUCCESS_DATA_TAG%s",
//                            gson.toJson(transactionWithRemark),
//                        )
//                    },
//                    { throwable ->
//                        loaderDialog.dismiss()
//                        resultViewerTextView.text = throwable.localizedMessage
//                        Timber.d(
//                            "$PAYMENT_ERROR_DATA_TAG%s",
//                            throwable.localizedMessage,
//                        )
//                    },
//                ),
//        )
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    private fun checkBalance(cardResult: CardResult) {
        val loaderDialog: LoadingDialog = LoadingDialog()
        loaderDialog.loadingMessage = getString(R.string.checking_balance)
        loaderDialog.show(supportFragmentManager, TAG_CHECK_BALANCE)
        val cardData = cardResult.cardReadResult.let {
            CardData(it.track2Data, it.iccString, it.pan, POS_ENTRY_MODE).also { cardD ->
                cardD.pinBlock = it.pinBlock
            }
        }
        compositeDisposable.add(
            netposPaymentClient.balanceEnquiry(this, cardData, IsoAccountType.SAVINGS.name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data, error ->
                    data?.let {
                        loaderDialog.dismiss()
                        val responseString = if (it.responseCode == Status.APPROVED.statusCode) {
                            "Response: APPROVED\nResponse Code: ${it.responseCode}\n\nAccount Balance:\n" + it.accountBalances.joinToString(
                                "\n",
                            ) { accountBalance ->
                                "${accountBalance.accountType}: ${
                                    accountBalance.amount.div(100).formatCurrencyAmount()
                                }"
                            }
                        } else {
                            "Response: ${it.responseMessage}\nResponse Code: ${it.responseCode}"
                        }
                        resultViewerTextView.text = responseString
                    }
                    error?.let {
                        loaderDialog.dismiss()
                        resultViewerTextView.text = it.localizedMessage
                    }
                },
        )
    }
}
