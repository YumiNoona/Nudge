package com.nudge.android.data

import androidx.room.*

@Dao
interface ReceiptDao {
    @Insert suspend fun insertReceipt(receipt: ReceiptEntity)
    @Insert suspend fun insertPages(pages: List<ReceiptPageEntity>)
    @Insert suspend fun insertItems(items: List<ReceiptLineItemEntity>)
    @Insert suspend fun insertLinks(links: List<ReceiptTransactionLinkEntity>)

    @Query("SELECT * FROM receipts WHERE id = :id") suspend fun getReceipt(id: String): ReceiptEntity?
    @Query("SELECT * FROM receipt_line_items WHERE receipt_id = :receiptId ORDER BY sort_order") suspend fun getItems(receiptId: String): List<ReceiptLineItemEntity>
    @Query("SELECT * FROM receipt_pages WHERE receipt_id = :receiptId ORDER BY page_index") suspend fun getPages(receiptId: String): List<ReceiptPageEntity>
    @Query("SELECT receipt_id FROM receipt_transaction_links WHERE transaction_id = :transactionId LIMIT 1") suspend fun receiptIdForTransaction(transactionId: String): String?
}
