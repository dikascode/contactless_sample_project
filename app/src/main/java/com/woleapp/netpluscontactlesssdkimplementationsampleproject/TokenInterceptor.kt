package com.woleapp.netpluscontactlesssdkimplementationsampleproject

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class TokenInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val headersInReq = request.headers
        request = request.newBuilder()
            .addHeader("APP-KEY","218B8450-F4C3-4A78-85CE-ED7EE9FD75A2")
            .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdG9ybUlkIjoiYWFkMDBmNjYtYWY5MS00MmEzLTgxM2EtNzhmYWEyNDA1ZmVkIiwiYmFua05hbWUiOiJndGIiLCJ0ZXJtaW5hbElkIjoiMjAzM0FMWlAiLCJ1c2VyX3R5cGUiOiJyZWd1bGFyIiwibWVyY2hhbnRJZCI6IjIwMzNMQUdQT09PNzg4NSIsImJ1c2luZXNzTmFtZSI6IkRveWluIE1hbWEiLCJ1c2VybmFtZSI6IkRveWlubWFtYUBnbWFpbC5jb20iLCJidXNpbmVzc19hZGRyZXNzIjoiMzEsIEFndW5naSBSb2FkIEJlc2lkZSBDaGlja2VuIFJlcHVibGljLCBMZWtraSBMYWdvcy4iLCJwaG9uZV9udW1iZXIiOiIwODA2MzU4Mjc4OSIsIm5ldHBsdXNQYXlNaWQiOiJNSUQ2NGQwZGMwNTQyMzFjIiwicGFydG5lcklkIjoiOWIxOTRkNjMtODUzOS00MTM3LWEyYTctYTQyOTdjN2NkOGUxIiwiZG9tYWlucyI6WyJuZXRwb3MiXSwicm9sZXMiOlsiYWdlbnQiXSwiaXNzIjoic3Rvcm06YWNjb3VudHMiLCJzdWIiOiJ1c2VyIiwiaWF0IjoxNzA0ODQ2NDA3LCJleHAiOjE3MzYzODI0MDd9.3r-cZqOcrOpeEN2fbpN34PBos08fN3sbqv1O7I7w8no")
            .build()

        val response = chain.proceed(request)
        val body = response.body
        val bodyString = body?.string()

        return response.newBuilder()
            .body(bodyString!!.toResponseBody(body.contentType()))
            .build()
    }}
