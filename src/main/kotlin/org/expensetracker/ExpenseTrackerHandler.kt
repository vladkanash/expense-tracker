package org.expensetracker

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.network.serialization.GsonFactory
import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import kotlinx.serialization.ExperimentalSerializationApi

@Suppress("unused")
@ExperimentalSerializationApi
class ExpenseTrackerHandler : HttpFunction {

    private val gson = GsonFactory.createForApiClient()

    override fun service(request: HttpRequest, response: HttpResponse) {
        val update = request.getUpdate()
        println("Update is $update")

        val responseText = handleUpdate(update)

        initBot().sendMessage(
            ChatId.fromId(update.getUserId()),
            text = responseText,
            parseMode = ParseMode.MARKDOWN
        )
        println("Message sent successfully")

        request.reader.close()
    }

    private fun initBot() = bot { token = System.getenv("BOT_TOKEN") }

    private fun HttpRequest.getUpdate() = reader.readText().let { gson.fromJson(it, Update::class.java) }

    private fun handleUpdate(update: Update): String =
        when {
            update.isCommand("getSummary") -> handleGetSummary(update)
            update.isCommand("addExpense") -> handleAddExpense(update)
            else -> ""
        }

    private fun Update.isCommand(command: String) = message?.text?.drop(1)?.startsWith(command) == true

    private fun handleAddExpense(update: Update): String {
        val amount = update.getAmount()
            ?: return "You haven't specified expense amount"

        val summary = ExpenseSummaryService.addAmount(update.getUserId(), amount)
            ?: return "There was an error trying to add expense"

        return "Expense added successfully, your new total is ${summary.getPrettyAmount()}"
    }

    private fun handleGetSummary(update: Update): String {
        val summary = ExpenseSummaryService.getSummary(update.getUserId())
            ?: return "There was an error trying to add expense"

        return "Your expense total is: ${summary.getPrettyAmount()}"
    }

    private fun Summary.getPrettyAmount() = "${amountInCents.toDouble() / 100}$"

    private fun Update.getAmount() = message?.text?.drop(12)?.toLong()

    private fun Update.getUserId() = message?.from?.id!!
}
