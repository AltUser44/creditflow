package com.creditflow.users

import java.util.UUID

// row shape for doobie; never return passwordHash on http responses
final case class User(
    id: UUID,
    email: String,
    passwordHash: String
)

// handy if you expose user json without internal fields
final case class UserPublic(
    id: UUID,
    email: String
)
