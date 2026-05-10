package ru.kuzdikenov.fooddiary.exception

class EmailAlreadyExistsException(email: String) : RuntimeException("User with $email already exists")