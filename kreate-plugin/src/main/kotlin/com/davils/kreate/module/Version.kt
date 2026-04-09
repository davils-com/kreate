package com.davils.kreate.module

public fun getProjectVersion(key: String): String = System.getenv(key) ?: "1.0.0"
