package com.gmail.vondenuelle.denuspend.data.remote.error

class CannotCreateTransactionException (
    override val message: String = "Cannot create transaction"
) : Exception(message)

class CannotUpdateTransactionException (
    override val message: String = "Cannot update transaction"
) : Exception(message)

class ReturnedNullException (
    override val message: String = "Data cannot be read"
) : Exception(message)

class DocumentNotFoundException (
    override val message: String = "Data does not exist in our database"
) : Exception(message)

class ReturnedEmptyListException (
    override val message: String = "Collection of data is empty. Nothing to show"
) : Exception(message)