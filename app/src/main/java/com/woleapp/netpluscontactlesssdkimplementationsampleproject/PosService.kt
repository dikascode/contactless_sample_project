package com.woleapp.netpluscontactlesssdkimplementationsampleproject

import com.google.gson.JsonObject
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PosService {
    @POST("pos-transaction/process")
    fun posTransaction(@Body credentials: JsonObject?): Single<Response<JsonObject>>
}