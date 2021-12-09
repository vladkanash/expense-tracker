package org.expensetracker

import com.github.kotlintelegrambot.entities.Update
import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import kotlinx.serialization.ExperimentalSerializationApi

@Suppress("unused")
@ExperimentalSerializationApi
class ExpenseTrackerFunction : HttpFunction {

    override fun service(request: HttpRequest, response: HttpResponse) {
        val update = TelegramService.parseUpdate(request.reader.readText())
        println("Update is $update")

        val responseText = handleUpdate(update)

        TelegramService.sendMessage(update.getUserId(), responseText)
        request.reader.close()
    }

    private fun handleUpdate(update: Update): String = when {
        update.isCommand("getSummary") -> handleGetSummary(update)
        update.isCommand("addExpense") -> handleAddExpense(update)
        else -> ""
    }

    private fun Update.isCommand(command: String) = message?.text?.drop(1)?.startsWith(command) == true

    private fun handleAddExpense(update: Update): String {
        val amount = update.getAmount() ?: return "You haven't specified expense amount"
        val summary = addAmount(update, amount) ?: return "There was an error trying to add expense"

        return "Expense added successfully, your new total is ${summary.getPrettyAmount()}"
    }

    private fun addAmount(update: Update, amount: Long): Summary? {
        val id = update.getUserId()
        return FirebaseRepository.getSummaryById(id)
            ?.addAmount(amount)
            ?.let { FirebaseRepository.updateSummary(id, it) }
    }

    private fun Summary.addAmount(amount: Long) = copy(
        amountInCents = this.amountInCents + amount
    )

    private fun handleGetSummary(update: Update): String {
        val summary =
            FirebaseRepository.getSummaryById(update.getUserId()) ?: return "There was an error trying to add expense"

        return "Your expense total is: ${summary.getPrettyAmount()}"
    }

    private fun Summary.getPrettyAmount() = "${amountInCents.toDouble() / 100}$"

    private fun Update.getAmount() = message?.text?.drop(12)?.toLong()

    private fun Update.getUserId() = message?.from?.id!!
}
