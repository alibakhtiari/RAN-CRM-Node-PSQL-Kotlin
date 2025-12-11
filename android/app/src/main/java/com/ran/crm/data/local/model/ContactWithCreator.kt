package com.ran.crm.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.ran.crm.data.local.entity.Contact

data class ContactWithCreator(
        @Embedded val contact: Contact,
        @ColumnInfo(name = "creator_name") val creatorName: String?
)
