package com.example.civilrightshelper

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

// Sources:
// - https://stackoverflow.com/questions/54413155/mocking-okhttp-response
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class FetchLLMStreamInstrumentedTest {

    @Test
    fun fetchLLMStream_streams_chunks_until_end() = runBlocking {
        val chunks = mutableListOf<String>()

        // Fake server streaming response
        val bodyText = "Hello World"
        val body = bodyText.toResponseBody("text/plain".toMediaTypeOrNull())

        val response = Response.Builder()
            .request(Request.Builder().url("http://localhost").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body)
            .build()

        // Mock OkHttp to return our fake streaming response
        val mockCall = mock<Call> { whenever(it.execute()).thenReturn(response) }
        val mockClient = mock<OkHttpClient> { whenever(it.newCall(any())).thenReturn(mockCall) }

        // streaming
        fetchLLMStream("test", "en", client = mockClient) { chunks.add(it) }

        assertEquals(listOf("Hello World"), chunks)
    }
}
