package com.fastsail

import kotlinx.serialization.Serializable

@Serializable
data class AppUser(
    var  userId: String="",
    var name: String="",
    var avatar: String?="",
    var pushNotificationToken:String?=""
)