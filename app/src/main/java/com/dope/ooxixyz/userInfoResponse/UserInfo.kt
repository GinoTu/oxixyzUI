package com.dope.ooxixyz.userInfoResponse

data class UserInfo(
    val _id: String,
    val membersList: List<Members>,
    val membersRequest: List<MembersRequest>,
    val password: String,
    val phoneNumber: Int,
    val userName: String
)