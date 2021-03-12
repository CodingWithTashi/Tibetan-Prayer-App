package com.codingwithtashi.dailyprayer.model

import com.google.firebase.firestore.DocumentId

/**
 * Created by kunchok on 10/03/2021
 */
data class Prayer(
    @DocumentId
    var id: String? = null,
    var title:String? = null,
    var content:String? = null,
    var imageUrl: String? = null
)
