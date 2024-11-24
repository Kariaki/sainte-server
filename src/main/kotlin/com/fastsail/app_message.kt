package com.fastsail

import kotlinx.serialization.Serializable

 class AppMessage(
    val senderId: String = "",
    var receiverId: String= "",
    var id: String= "",
    var conversationId: String= "",

    var text: String= ""
)