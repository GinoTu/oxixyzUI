package com.dope.ooxixyz.loginResponse

data class Response(
    val status: Int,
    val token: String,
    val userDetail: UserDetail
)