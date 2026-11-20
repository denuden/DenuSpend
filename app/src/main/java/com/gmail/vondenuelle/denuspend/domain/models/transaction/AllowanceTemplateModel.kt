package com.gmail.vondenuelle.denuspend.domain.models.transaction

import androidx.annotation.Keep

// A saved set of allowances (e.g. "Weekday": Food ₱300, Transportation ₱150) that adds them all for today in one tap.
// Stored as a list in one allowance_templates/{uid} doc per user (one read for all templates), deleted with the account.
@Keep
data class AllowanceTemplateModel(
    val id : String = "",
    val name : String = "",
    val items : List<AllowanceTemplateItem> = emptyList(),
)

@Keep
data class AllowanceTemplateItem(
    val id : String = "", //only used as a stable key for the editor rows
    val category : String = "",
    val amount : Long = 0L, //cents, positive
)

// The allowance_templates/{uid} doc, read with toObject()
@Keep
data class AllowanceTemplatesDoc(
    val userId : String = "",
    val templates : List<AllowanceTemplateModel> = emptyList(),
)
