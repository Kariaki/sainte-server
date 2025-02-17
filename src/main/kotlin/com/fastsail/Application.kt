package com.fastsail

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.FirestoreOptions
import com.google.cloud.firestore.Query
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun getMessageUsers(ref: CollectionReference, members: List<String>): List<AppUser> {
    val result = ref.whereIn("userId", members)
        .get().get()
    return result.documents.map {
        return@map it.toObject(AppUser::class.java)
    }
}

fun Application.module() {
    val fileName = "service-file.json"//"qtalk-server-file.json"
    val serviceAccount = FileInputStream("./resources/$fileName")
    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        //  .setDatabaseUrl("https://qtalk-4dd0f.firebaseio.com")
        .setFirestoreOptions(FirestoreOptions.getDefaultInstance())
        .build()
    FirebaseApp.initializeApp(options)
    configureHTTP()
    configureRouting()

    val userCollection = FirestoreClient.getFirestore().collection("user")

    CoroutineScope(Dispatchers.IO).launch {
        FirestoreClient.getFirestore().collection("messages")
            .orderBy("timestamp",Query.Direction.DESCENDING)
            .addSnapshotListener { querySnapshot, _ ->
                run {
                    val message = querySnapshot?.documents?.firstOrNull()
                    println(message?.data)
                    if (message != null) {
                        try{
                            val messageResult = message.toObject(AppMessage::class.java)
                            println("message received -> ${messageResult.text}")
                            val messageMembers =
                                getMessageUsers(userCollection, listOf(messageResult.senderId, messageResult.receiverId))
                            val senderUser = messageMembers.first { it.userId == messageResult.senderId }
                            val receiver = messageMembers.first { it.userId == messageResult.receiverId }
                            val token = receiver.pushNotificationToken
                            println("token found -> $token")
                            token?.let {
                                sendMessage(
                                    title = senderUser.name,
                                    body = messageResult.text,
                                    token = it
                                )
                            }
                        }catch (e:Exception){
                            e.printStackTrace()
                            println("exception occurred -> ${e.message}")

                        }

                    }
                }
            }
    }
}

private fun sendMessage(title: String, body: String, token: String) {
    FirebaseMessaging.getInstance().send(
        Message.builder()
            .setToken(
                token
            )
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body).build()
            ).build()
    )
}
