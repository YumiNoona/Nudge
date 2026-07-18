package com.nudge.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Category(
    val id: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null, // hex color
    val type: CategoryType,
    @SerialName("is_default")
    val isDefault: Boolean = false,
    @SerialName("is_archived")
    val isArchived: Boolean = false,
    @SerialName("monthly_budget")
    val monthlyBudget: Long? = null // in cents, null = no budget set
)

@Serializable
enum class CategoryType {
    @SerialName("expense") EXPENSE,
    @SerialName("income") INCOME
}

@Serializable
data class Subcategory(
    val id: String,
    @SerialName("category_id")
    val categoryId: String,
    val name: String,
    val icon: String? = null
)
