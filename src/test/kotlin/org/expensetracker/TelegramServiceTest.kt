package org.expensetracker

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class TelegramServiceTest {

    val service = TelegramService()

    @Test
    fun `Should parse incoming update`() {
        //language=json
        val updateString = """
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

        val result = service.parseUpdate(updateString)

        assertEquals(2165, result.message!!.messageId)
        assertEquals(196277202, result.message!!.from!!.id)
    }
}
