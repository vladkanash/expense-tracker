package org.expensetracker

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.network.serialization.GsonFactory

class TelegramService {

    private val bot: Bot = bot { token = System.getenv("BOT_TOKEN") }
    private val gson = GsonFactory.createForApiClient()

    fun sendMessage(id: Long, message: String) {
        bot.sendMessage(
            ChatId.fromId(id),
            text = message,
            parseMode = ParseMode.MARKDOWN
        )
    }

    fun parseUpdate(update: String): Update = gson.fromJson(update, Update::class.java)
}
