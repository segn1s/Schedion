package net.schedion.app.core

import java.io.IOException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ContentStateTest {
    @Test fun refreshFailure_keepsDataAndSnapshotTime() {
        val initial = ContentState<String>().accept(Resource.Success("saved", false, 123L))
        val result = initial.accept(Resource.Loading).accept(Resource.Error("", IOException()))
        assertEquals("saved", result.data)
        assertEquals(123L, result.savedAt)
        assertTrue(result.isOffline)
        assertFalse(result.isRefreshing)
    }

    @Test fun retryWithCachedData_doesNotFlickerOfflineBanner() {
        val offline = ContentState<String>().accept(Resource.Success("saved", true, 123L))
            .accept(Resource.Error("", IOException()))
        val retry = offline.accept(Resource.Loading).accept(Resource.Success("saved", true, 123L))
        assertTrue(retry.isOffline)
        assertTrue(retry.isRefreshing)
        val fresh = retry.accept(Resource.Success("fresh", false, 456L))
        assertFalse(fresh.isOffline)
        assertNull(fresh.error)
        assertFalse(fresh.isRefreshing)
        assertEquals("fresh", fresh.data)
    }

    @Test fun httpError_isNotOffline() {
        val error = HttpException(Response.error<Any>(503, "unavailable".toResponseBody()))
        val result = ContentState<String>().accept(Resource.Error("", error))
        assertFalse(result.isOffline)
        assertTrue(result.resource is Resource.Error)
    }

    @Test fun emptyCachedCollection_isDataRatherThanAnError() {
        val result = ContentState<List<String>>().accept(Resource.Success(emptyList(), true, 1L))
            .accept(Resource.Error("", IOException()))
        assertNotNull(result.data)
        assertTrue(result.data!!.isEmpty())
        assertTrue(result.resource is Resource.Success)
    }
}
