package com.fampay.`in`.adapters

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.fampay.`in`.R
import com.fampay.`in`.base.BaseActivity
import com.fampay.`in`.helpers.analytics.Source
import com.fampay.`in`.models.enums.TransactionPerspective
import com.fampay.`in`.models.transactions.BasicTransactionResponseModel
import com.fampay.`in`.models.transactions.external_source.ExternalSourceModel
import com.fampay.`in`.models.user.User
import com.fampay.`in`.ui.transaction_details.TransactionDetailsActivity
import com.fampay.`in`.utils.ImageUtils
import com.fampay.`in`.utils.StringUtils
import com.fampay.`in`.utils.ViewUtils

class RecentTransactionsAdapter(private var baseActivity: BaseActivity) :
    RecyclerView.Adapter<RecentTransactionsAdapter.TransactionViewHolder>() {

    enum class CardType {
        CARD_TOP,
        CARD_MIDDLE,
        CARD_BOTTOM,
        CARD_SINGLE
    }

    private var transactions: ArrayList<BasicTransactionResponseModel> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        return TransactionViewHolder(
            inflater.inflate(
                R.layout.recent_transaction_recycler_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        var cardType = CardType.CARD_SINGLE
        if (position == 0 && transactions.size != 1) {
            cardType = CardType.CARD_TOP
        } else if (position != 0 && position == transactions.size - 1) {
            cardType = CardType.CARD_BOTTOM
        } else if (position != 0 && position < transactions.size - 1) {
            cardType = CardType.CARD_MIDDLE
        }
        holder.bindData(baseActivity, transactions[position], cardType)
    }

    fun loadRecentTransactions(recentTransactions: ArrayList<BasicTransactionResponseModel>) {
        transactions.clear()
        transactions.addAll(recentTransactions)
        notifyDataSetChanged()
    }

    override fun getItemCount() = transactions.size

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val TAG = "MoneyPage"
        fun bindData(
            baseActivity: BaseActivity,
            transaction: BasicTransactionResponseModel,
            cardType: CardType
        ) {

            itemView.setOnClickListener {
                val intent = Intent(baseActivity, TransactionDetailsActivity::class.java).apply {
                    this.putExtra(
                        TransactionDetailsActivity.INTENT_EXTRA_TRANSACTION_ID,
                        transaction.id
                    )
                }
                baseActivity.startActivity(intent)
            }

            val horizontalDivider: View = itemView.findViewById(R.id.transaction_divider_line)
            val itemContainer: LinearLayout =
                itemView.findViewById(R.id.recent_transactions_item_container)
            when (cardType) {
                CardType.CARD_SINGLE -> {
                    itemContainer.setBackgroundResource(
                        R.drawable.rounded_rect_background_white_12dp
                    )
                    horizontalDivider.visibility = View.GONE
                }
                CardType.CARD_TOP -> {
                    itemContainer.setBackgroundResource(
                        R.drawable.rounded_top_rect_background_white_12dp
                    )
                    horizontalDivider.visibility = View.VISIBLE
                }
                CardType.CARD_BOTTOM -> {
                    itemContainer.setBackgroundResource(
                        R.drawable.rounded_bottom_rect_background_white_12dp
                    )
                    horizontalDivider.visibility = View.GONE
                }
                CardType.CARD_MIDDLE -> {}
                else -> {}
            }
            itemContainer.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#292929"))

            val dateTextView: TextView = itemView.findViewById(R.id.tv_date)
            dateTextView.text = StringUtils.getFormattedString(
                transaction.dateCreated,
                "dd MMM • h:m a"
            )

            val userImageView: ImageView = itemView.findViewById(R.id.iv_user_avatar)
            val amountTextView: TextView = itemView.findViewById(R.id.tv_amount)
            val userNameTextView: TextView = itemView.findViewById(R.id.tv_user_name)
            val txnStatusTextView: TextView = itemView.findViewById(R.id.tv_txn_status)

            var amountColor = Color.WHITE
            var title = ""
            var isAmountCredited = true
            transaction.setupStatus(
                txnStatusTextView, TransactionPerspective.USER,
                "#F1F6F7",
                "#AEAEAE"
            )

            if (transaction.status == BasicTransactionResponseModel.STATUS_SUCCESS) {
                txnStatusTextView.visibility = View.GONE
            } else {
                txnStatusTextView.visibility = View.VISIBLE
                if (transaction.status == BasicTransactionResponseModel.STATUS_FAILED) {
                    txnStatusTextView.setTextColor(Color.parseColor("#F96D6D"))
                }
            }

            when (transaction.type) {
                BasicTransactionResponseModel.TRANSACTION_TYPE_USER_TO_USER -> {
                    var userToDisplay: User?
                    if (transaction.isMe) {
                        userToDisplay = transaction.user
                        isAmountCredited = false
                        amountColor = Color.parseColor("#FFFCFC") // White
                    } else {
                        userToDisplay = transaction.createdBy
                        amountColor = transaction.getColorByStatus(baseActivity)
                    }
                    title = userToDisplay.name()
                    ImageUtils.loadImage(
                        baseActivity,
                        userToDisplay.avatar,
                        userImageView,
                        true,
                        userToDisplay.firstName
                    )
                    userImageView.setOnClickListener {
                        baseActivity.navigateToUserProfile(userToDisplay.username, Source.TXN_CARD)
                    }
                }

                BasicTransactionResponseModel.TRANSACTION_TYPE_USER_TO_EXTERNAL -> {
                    isAmountCredited = false
                    transaction.externalSourceModel?.let {
                        when (it.type) {
                            ExternalSourceModel.SOURCE_TYPE_VPA -> {
                                title = it.vpa.getName()
                                it.vpa.loadVpaImage(baseActivity, userImageView)
                            }
                            ExternalSourceModel.SOURCE_TYPE_BANK_ACCOUNT -> {
                                title = it.bankAccountModel.accountHolderName
                                userImageView.setImageResource(R.drawable.ic_bank_new)
                            }
                            ExternalSourceModel.SOURCE_TYPE_MERCHANT -> {
                                title = it.merchant.name
                                ImageUtils.loadImage(
                                    baseActivity,
                                    it.merchant.icon,
                                    userImageView,
                                    R.drawable.ic_shop_new
                                )
                            }
                        }
                    }
                }

                BasicTransactionResponseModel.TRANSACTION_TYPE_EXTERNAL_TO_USER -> {
                    transaction.externalSourceModel?.let {
                        when (it.type) {
                            ExternalSourceModel.SOURCE_TYPE_UPI_APP,
                            ExternalSourceModel.SOURCE_TYPE_NETBANKING,
                            ExternalSourceModel.SOURCE_TYPE_SAVED_CARD,
                            ExternalSourceModel.SOURCE_TYPE_CARD -> {
                                title = transaction.createdBy.name()
                                ImageUtils.loadImage(
                                    baseActivity,
                                    transaction.createdBy.avatar,
                                    userImageView,
                                    true,
                                    transaction.createdBy.firstName
                                )
                                userImageView.setOnClickListener {
                                    baseActivity.navigateToUserProfile(
                                        transaction.createdBy.username,
                                        Source.TXN_CARD
                                    )
                                }
                            }
                            ExternalSourceModel.SOURCE_TYPE_VPA -> {
                                if (BasicTransactionResponseModel.MODE_UPI == transaction.mode) {
                                    title = transaction.externalSourceModel.vpa.getName()
                                    transaction.externalSourceModel.vpa.loadVpaImage(
                                        baseActivity, userImageView
                                    )
                                } else {
                                    title = transaction.createdBy.name()
                                    ImageUtils.loadImage(
                                        baseActivity,
                                        transaction.createdBy.avatar,
                                        userImageView,
                                        true,
                                        transaction.createdBy.firstName
                                    )
                                    userImageView.setOnClickListener {
                                        baseActivity.navigateToUserProfile(
                                            transaction.createdBy.username,
                                            Source.TXN_CARD
                                        )
                                    }
                                }
                            }
                            ExternalSourceModel.SOURCE_TYPE_MERCHANT -> {
                                title = it.merchant.name
                                ImageUtils.loadImage(
                                    baseActivity,
                                    transaction.externalSourceModel.merchant.icon,
                                    userImageView,
                                    R.drawable.ic_shop_new
                                )
                            }
                        }
                    }
                }

                BasicTransactionResponseModel.TRANSACTION_TYPE_PERSONAL_TO_SAVINGS -> {
                    isAmountCredited = false
                    if (transaction.isMe) {
                        title = "Savings Account"
                        userImageView.setImageResource(R.drawable.ic_savings_challenge)
                    } else {
                        // TODO: handle wrong state
                    }
                }

                BasicTransactionResponseModel.TRANSACTION_TYPE_SAVINGS_TO_PERSONAL -> {
                    if (transaction.isMe) {
                        title = "Savings Account"
                        userImageView.setImageResource(R.drawable.ic_savings_challenge)
                        amountColor = Color.parseColor("#41cb79") // Green
                    } else {
                        // TODO: handle wrong state
                    }
                }

                BasicTransactionResponseModel.TRANSACTION_TYPE_CREDIT_TO_EXTERNAL -> {
                    val external = transaction.externalSourceModel

                    if (transaction.isMe) {
                        when (external.type) {
                            ExternalSourceModel.SOURCE_TYPE_VPA -> {
                                title = external.vpa.getName()
                                external.vpa.loadVpaImage(baseActivity, userImageView)
                                userImageView.setOnClickListener(null)
                            }
                            ExternalSourceModel.SOURCE_TYPE_MERCHANT -> {
                                title = external.merchant.name
                                ImageUtils.loadImage(
                                    baseActivity,
                                    external.merchant.icon,
                                    userImageView,
                                    R.drawable.ic_shop_new
                                )
                                userImageView.setOnClickListener(null)
                            }
                            else -> {
                                Toast.makeText(
                                    baseActivity,
                                    "Wrong external type : " + external.type + " in UE transaction type",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return
                            }
                        }
                    } else {
                        // Not Possible
                        Toast.makeText(
                            baseActivity,
                            "Wrong type CE, !transaction.isMe",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    isAmountCredited = false
                    amountColor = Color.parseColor("#FFFCFC") // White
                }

                BasicTransactionResponseModel.TRANSACTION_TYPE_EXTERNAL_TO_CREDIT -> {

                    when (transaction.externalSourceModel.type) {
                        ExternalSourceModel.SOURCE_TYPE_UPI_APP,
                        ExternalSourceModel.SOURCE_TYPE_NETBANKING,
                        ExternalSourceModel.SOURCE_TYPE_SAVED_CARD,
                        ExternalSourceModel.SOURCE_TYPE_CARD -> {
                            title = transaction.createdBy.name()
                            ImageUtils.loadImage(
                                baseActivity,
                                transaction.createdBy.avatar,
                                userImageView,
                                true,
                                transaction.createdBy.firstName
                            )
                            userImageView.setOnClickListener {
                                baseActivity.navigateToUserProfile(
                                    transaction.createdBy.username,
                                    Source.TXN_CARD
                                )
                            }
                        }
                        ExternalSourceModel.SOURCE_TYPE_VPA -> {
                            if (transaction.mode.equals(BasicTransactionResponseModel.MODE_UPI)) {
                                title = transaction.externalSourceModel.vpa.getName()
                                transaction.externalSourceModel.vpa.loadVpaImage(
                                    baseActivity,
                                    userImageView
                                )
                            } else {
                                title = transaction.createdBy.name()
                                ImageUtils.loadImage(
                                    baseActivity,
                                    transaction.createdBy.avatar,
                                    userImageView,
                                    true,
                                    transaction.createdBy.firstName
                                )
                                userImageView.setOnClickListener {
                                    baseActivity.navigateToUserProfile(transaction.createdBy.username, Source.TXN_CARD)
                                }
                            }
                        }
                        ExternalSourceModel.SOURCE_TYPE_MERCHANT -> {
                            title = transaction.externalSourceModel.merchant.name
                            ImageUtils.loadImage(
                                baseActivity,
                                transaction.externalSourceModel.merchant.icon,
                                userImageView,
                                R.drawable.ic_shop_new
                            )
                            userNameTextView.setOnClickListener(null)
                        }
                    }

                    ViewUtils.setupAmountText(
                        baseActivity,
                        amountTextView,
                        null,
                        "₹",
                        "${transaction.amount / 100f}",
                        transaction.getColorByStatus(baseActivity)
                    )
                }

                BasicTransactionResponseModel.TRANSACTION_TYPE_USER_TO_CREDIT -> {

                    if (transaction.isMe) {
                        title = "Credit Account"
                        ViewUtils.setupAmountText(
                            baseActivity,
                            amountTextView,
                            "- ",
                            "₹",
                            "${transaction.amount / 100f}",
                            Color.BLACK
                        )
                    } else {
                        // Wrong State
                        Toast.makeText(
                            baseActivity,
                            "Wrong type, !transaction.isMe",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    userImageView.setImageResource(R.drawable.ic_credit_load)
                }
            }

            if (transaction.status != BasicTransactionResponseModel.STATUS_SUCCESS) {
                amountColor = Color.parseColor("#838383") // Grey
            } else if (isAmountCredited) {
                amountColor = Color.parseColor("#5FB06C")
            }

            if (isAmountCredited) {
                ViewUtils.setupAmountText(
                    baseActivity,
                    amountTextView,
                    if (transaction.status == BasicTransactionResponseModel.STATUS_SUCCESS) "+ " else "",
                    "₹",
                    getAmountForUI(transaction.amount),
                    amountColor
                )
            } else {
                ViewUtils.setupAmountText(
                    baseActivity,
                    amountTextView,
                    "",
                    "₹",
                    getAmountForUI(transaction.amount),
                    amountColor
                )
            }
            userNameTextView.text = title
        }

        private fun getAmountForUI(amount: Int): String {
            val hasPaise = amount % 100 > 0
            return if (hasPaise) {
                "${amount / 100f}"
            } else {
                "${amount / 100}"
            }
        }
    }
}
