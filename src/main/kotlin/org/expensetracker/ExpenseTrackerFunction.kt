package org.expensetracker

import com.github.kotlintelegrambot.entities.Update
import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.math.roundToLong

@Suppress("unused")
@ExperimentalSerializationApi
class ExpenseTrackerFunction(
    private val repository: FirebaseRepository = FirebaseRepository(),
    private val telegramService: TelegramService = TelegramService(),
) : HttpFunction {

    override fun service(request: HttpRequest, response: HttpResponse) {
        val update = telegramService.parseUpdate(request.reader.readText())
        println("Update is $update")

        handleUpdate(update)?.also {
            telegramService.sendMessage(update.getUserId(), it)
        }
        request.reader.close()
    }

    private fun handleUpdate(update: Update): String? = when {
        update.isCommand("getSummary") -> handleGetSummary(update)
        update.isCommand("addExpense") -> handleAddExpense(update)
        else -> null
    }

    private fun Update.isCommand(command: String) =
        message?.text?.drop(1)?.startsWith(command) == true

    private fun handleAddExpense(update: Update): String {
        val amount = update.getAmount() ?: return "This expense amount seems incorrect"
        val summary = addAmount(update, amount) ?: return "There was an error trying to add expense"

        return "Expense added successfully, your new total is ${summary.getPrettyAmount()}"
    }

    private fun Update.getAmount() =
        message?.text?.drop(12)?.trim()
            ?.toDoubleOrNull()
            ?.let { (it * 100).roundToLong() }

    private fun addAmount(update: Update, amount: Long): Summary? {
        val id = update.getUserId()
        return repository.getSummaryById(id)
            ?.addAmount(amount)
            ?.let { repository.updateSummary(id, it) }
    }

    private fun Summary.addAmount(amount: Long) = copy(
        amountInCents = this.amountInCents + amount
    )

    private fun handleGetSummary(update: Update): String {
        val summary = repository.getSummaryById(update.getUserId())
            ?: return "There was an error trying to add expense"

        return "Your total expense is: ${summary.getPrettyAmount()}"
    }

    private fun Summary.getPrettyAmount() = "${amountInCents.toDouble() / 100}$"

    private fun Update.getUserId() = message?.from?.id!!
}
