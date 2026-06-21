package eu.kanade.tachiyomi.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import rx.Observable
import rx.subscriptions.Subscriptions
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HttpException(val code: Int) : IllegalStateException("HTTP error $code")

fun Call.asObservable(): Observable<Response> {
    return Observable.create(
        Observable.OnSubscribe<Response> { subscriber ->
            val call = this
            call.enqueue(
                object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onNext(response)
                            subscriber.onCompleted()
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        if (!subscriber.isUnsubscribed) subscriber.onError(e)
                    }
                },
            )
            subscriber.add(Subscriptions.create { call.cancel() })
        },
    )
}

fun Call.asObservableSuccess(): Observable<Response> = asObservable().doOnNext { response ->
    if (!response.isSuccessful) {
        response.close()
        throw HttpException(response.code)
    }
}

suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(
        object : Callback {
            override fun onResponse(call: Call, response: Response) {
                cont.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isCancelled) cont.resumeWithException(e)
            }
        },
    )
    cont.invokeOnCancellation { runCatching { cancel() } }
}

suspend fun Call.awaitSuccess(): Response {
    val response = await()
    if (!response.isSuccessful) {
        response.close()
        throw HttpException(response.code)
    }
    return response
}
