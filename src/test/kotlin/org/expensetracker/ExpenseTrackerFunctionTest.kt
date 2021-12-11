package org.expensetracker

import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.User
import com.google.cloud.functions.HttpRequest
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.BufferedReader
import java.io.Reader

@ExtendWith(MockKExtension::class)
@ExperimentalSerializationApi
internal class ExpenseTrackerFunctionTest {

    @InjectMockKs
    private lateinit var function: ExpenseTrackerFunction

    @MockK
    lateinit var repository: FirebaseRepository

    @MockK
    lateinit var telegramService: TelegramService

    @MockK
    lateinit var request: HttpRequest

    @MockK
    lateinit var reader: BufferedReader

    @BeforeEach
    fun setUp() {
        every { telegramService.sendMessage(any(), any()) } just runs
        every { request.reader } returns reader
        every { reader.close() } just runs
        mockkStatic(Reader::readText)
    }

    @Test
    fun `Should process getSummary command correctly`() {
        val userId = 23534672971

        val updateRequest = createUpdateRequest()

        every { reader.readText() } returns updateRequest
        every { telegramService.parseUpdate(updateRequest) } returns createUpdate(userId, "/getSummary ")
        every { repository.getSummaryById(userId) } returns Summary(amountInCents = 20000)

        function.service(request, mockk())

        verify { telegramService.sendMessage(userId, """Your total expense is: 200.0$""") }
    }

    @Test
    fun `Should process addExpense command correctly`() {
        val userId = 23534672971
        val initialSummary = Summary(amountInCents = 20000)
        val updatedSummary = Summary(amountInCents = 30000)

        val updateRequest = createUpdateRequest()

        every { reader.readText() } returns updateRequest
        every { telegramService.parseUpdate(updateRequest) } returns createUpdate(userId, "/addExpense 100")
        every { repository.getSummaryById(userId) } returns initialSummary andThen updatedSummary
        every { repository.updateSummary(userId, updatedSummary) } returns updatedSummary

        function.service(request, mockk())

        verify { telegramService.sendMessage(userId, """Expense added successfully, your new total is 300.0$""") }
    }

    @Test
    fun `Should process fractions in addExpense command`() {
        val userId = 23534672971
        val initialSummary = Summary(amountInCents = 20000)
        val updatedSummary = Summary(amountInCents = 21056)

        val updateRequest = createUpdateRequest()

        every { reader.readText() } returns updateRequest
        every { telegramService.parseUpdate(updateRequest) } returns createUpdate(userId, "/addExpense 10.56")
        every { repository.getSummaryById(userId) } returns initialSummary andThen updatedSummary
        every { repository.updateSummary(userId, updatedSummary) } returns updatedSummary

        function.service(request, mockk())

        verify { telegramService.sendMessage(userId, """Expense added successfully, your new total is 210.56$""") }
    }

    @Test
    fun `Should process incorrect number in addExpense command`() {
        val userId = 23534672972
        val initialSummary = Summary(amountInCents = 20000)
        val updatedSummary = Summary(amountInCents = 21056)

        val updateRequest = createUpdateRequest()

        every { reader.readText() } returns updateRequest
        every { telegramService.parseUpdate(updateRequest) } returns createUpdate(userId, "/addExpense abc")
        every { repository.getSummaryById(userId) } returns initialSummary andThen updatedSummary
        every { repository.updateSummary(userId, updatedSummary) } returns updatedSummary

        function.service(request, mockk())

        verify { telegramService.sendMessage(userId, "This expense amount seems incorrect") }
    }

    @Test
    fun `Should not answer when no command is sent`() {
        val userId = 23534672971
        val initialSummary = Summary(amountInCents = 20000)
        val updatedSummary = Summary(amountInCents = 21056)

        val updateRequest = createUpdateRequest()

        every { reader.readText() } returns updateRequest
        every { telegramService.parseUpdate(updateRequest) } returns createUpdate(userId, "some text")
        every { repository.getSummaryById(userId) } returns initialSummary andThen updatedSummary
        every { repository.updateSummary(userId, updatedSummary) } returns updatedSummary

        function.service(request, mockk())

        verify(exactly = 0) { telegramService.sendMessage(any(), any()) }
    }

    @Test
    fun `Should create new summary if it doesn't exist for user`() {
        val userId = 17927643532
        val initialSummary = Summary(amountInCents = 20000)

        val updateRequest = createUpdateRequest()

        every { reader.readText() } returns updateRequest
        every { telegramService.parseUpdate(updateRequest) } returns createUpdate(userId, "/addExpense 200")
        every { repository.getSummaryById(userId) } returns null
        every { repository.updateSummary(userId, initialSummary) } returns initialSummary

        function.service(request, mockk())

        verify(exactly = 1) { telegramService.sendMessage(any(), any()) }
    }

    private fun createUpdate(userId: Long, text: String) = Update(
        updateId = 34245235636,
        message = Message(
            messageId = 54523875856,
            text = text,
            from = User(
                id = userId,
                firstName = "test_user",
                isBot = false,
            ),
            date = 463721354784682354,
            chat = Chat(
                id = 5683,
                type = "group"
            )
        )
    )

    //language=json
    private fun createUpdateRequest() = """
        {
            "update_id": 644668149,
            "message": {
                "message_id": 2165,
                "text": "/getSummary",
                "from": {
                    "id": 196277202,
                    "username": "test"
                },
                "date": 1638299653
            }
        }
    """.trimIndent()
}
