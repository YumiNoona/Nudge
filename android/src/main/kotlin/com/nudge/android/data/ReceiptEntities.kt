package com.nudge.android.data

import androidx.room.*

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "merchant") val merchant: String,
    @ColumnInfo(name = "printed_total_cents") val printedTotalCents: Long,
    @ColumnInfo(name = "calculated_total_cents") val calculatedTotalCents: Long,
    @ColumnInfo(name = "subtotal_cents") val subtotalCents: Long?,
    @ColumnInfo(name = "discount_cents") val discountCents: Long,
    @ColumnInfo(name = "tax_cents") val taxCents: Long,
    @ColumnInfo(name = "fee_cents") val feeCents: Long,
    @ColumnInfo(name = "tip_cents") val tipCents: Long,
    @ColumnInfo(name = "rounding_cents") val roundingCents: Long,
    @ColumnInfo(name = "purchase_timestamp") val purchaseTimestamp: Long,
    @ColumnInfo(name = "raw_text") val rawText: String,
    @ColumnInfo(name = "confidence") val confidence: Float,
    @ColumnInfo(name = "save_mode") val saveMode: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "receipt_pages",
    foreignKeys = [ForeignKey(entity = ReceiptEntity::class, parentColumns = ["id"], childColumns = ["receipt_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("receipt_id")],
)
data class ReceiptPageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "receipt_id") val receiptId: String,
    @ColumnInfo(name = "page_index") val pageIndex: Int,
    @ColumnInfo(name = "local_uri") val localUri: String,
    @ColumnInfo(name = "ocr_text") val ocrText: String,
    @ColumnInfo(name = "warning") val warning: String?,
)

@Entity(
    tableName = "receipt_line_items",
    foreignKeys = [ForeignKey(entity = ReceiptEntity::class, parentColumns = ["id"], childColumns = ["receipt_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("receipt_id"), Index("transaction_id")],
)
data class ReceiptLineItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "receipt_id") val receiptId: String,
    @ColumnInfo(name = "transaction_id") val transactionId: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "quantity") val quantity: Double,
    @ColumnInfo(name = "unit_price_cents") val unitPriceCents: Long?,
    @ColumnInfo(name = "line_total_cents") val lineTotalCents: Long,
    @ColumnInfo(name = "allocated_total_cents") val allocatedTotalCents: Long,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "confidence") val confidence: Float,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

@Entity(
    tableName = "receipt_transaction_links",
    primaryKeys = ["receipt_id", "transaction_id"],
    foreignKeys = [
        ForeignKey(entity = ReceiptEntity::class, parentColumns = ["id"], childColumns = ["receipt_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transaction_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("receipt_id"), Index("transaction_id")],
)
data class ReceiptTransactionLinkEntity(
    @ColumnInfo(name = "receipt_id") val receiptId: String,
    @ColumnInfo(name = "transaction_id") val transactionId: String,
)
