package org.expensetracker

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.fuel.serialization.responseObject
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.Result.Failure
import com.github.kittinunf.result.Result.Success
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val ACCESS_TOKEN_PARAM = "access_token"
private const val EXPENSE_SUMMARY_URI = "/summary"

@Serializable
data class Summary(val amountInCents: Long)

class FirebaseRepository {

    private val credentials: GoogleCredentials

    init {
        FuelManager.instance.basePath = System.getenv("FIREBASE_URL")
        credentials = GoogleCredentials.getApplicationDefault().createScoped(
            listOf(
                "https://www.googleapis.com/auth/userinfo.email",
                "https://www.googleapis.com/auth/firebase.database"
            )
        )
    }

    fun getSummaryById(id: Long): Summary? {
        val (_, _, result) = buildSummaryUrl(id)
            .httpGet(listOf(accessTokenParam()))
            .responseObject<Summary>()

        return getResponse(result)
    }

    @ExperimentalSerializationApi
    fun updateSummary(id: Long, summary: Summary): Summary? {
        val (_, _, result) = buildSummaryUrl(id)
            .httpPut(listOf(accessTokenParam()))
            .body(Json.encodeToString(summary))
            .responseObject<Summary>()

        return getResponse(result)
    }

    private fun buildSummaryUrl(id: Long) = "$EXPENSE_SUMMARY_URI/$id.json"

    private fun accessTokenParam() = ACCESS_TOKEN_PARAM to
            credentials
                .apply { refreshIfExpired() }
                .accessToken.tokenValue

    private fun getResponse(result: Result<Summary, FuelError>) =
        when (result) {
            is Success -> result.get()
            is Failure -> {
                println(result.getException())
                null
            }
        }
}
