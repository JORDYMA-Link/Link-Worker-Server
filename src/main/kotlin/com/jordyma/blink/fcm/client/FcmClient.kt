package com.jordyma.blink.fcm.client

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.springframework.stereotype.Component

@Component
class FcmClient(private val firebaseMessaging: FirebaseMessaging
) {

    fun send(message: Message) {
        val firebaseMessaging: FirebaseMessaging = FirebaseMessaging.getInstance()
        firebaseMessaging.send(message)
    }
}