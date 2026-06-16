package com.example.data.repository

import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class ERPRepository(private val db: AppDatabase) {
    private val customerDao = db.customerDao()
    private val inventoryDao = db.inventoryDao()
    private val orderDao = db.orderDao()
    private val purchaseOrderDao = db.purchaseOrderDao()
    private val transactionDao = db.transactionDao()

    // --- Customer Functions ---
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    suspend fun getCustomerById(id: Int) = customerDao.getCustomerById(id)
    suspend fun insertCustomer(customer: Customer) = customerDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    // --- Inventory Functions ---
    val allInventory: Flow<List<InventoryItem>> = inventoryDao.getAllInventory()
    suspend fun getInventoryItemById(id: Int) = inventoryDao.getInventoryItemById(id)
    suspend fun getInventoryItemBySku(sku: String) = inventoryDao.getInventoryItemBySku(sku)
    suspend fun insertInventoryItem(item: InventoryItem) = inventoryDao.insertInventoryItem(item)
    suspend fun updateInventoryItem(item: InventoryItem) = inventoryDao.updateInventoryItem(item)
    suspend fun deleteInventoryItem(item: InventoryItem) = inventoryDao.deleteInventoryItem(item)

    // --- Order & Sale Logic ---
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    suspend fun getOrderById(id: Int) = orderDao.getOrderById(id)

    /**
     * Executes placing a customer order. Deducts stock, creates accounting transaction, and updates customer balance if unpaid.
     */
    suspend fun createCustomerOrder(order: Order) {
        // 1. Insert order
        val orderIdLong = orderDao.insertOrder(order)
        val orderId = orderIdLong.toInt()

        // 2. Adjust inventory stock
        for (item in order.items) {
            val invItem = inventoryDao.getInventoryItemById(item.itemId)
            if (invItem != null) {
                val newQty = (invItem.quantity - item.quantity).coerceAtLeast(0)
                inventoryDao.updateInventoryItem(invItem.copy(quantity = newQty))
            }
        }

        // 3. Update customer balance (if unpaid or partial, they owe money)
        if (order.paymentStatus != "Paid") {
            val customer = customerDao.getCustomerById(order.customerId)
            if (customer != null) {
                val unpaidAmount = if (order.paymentStatus == "Unpaid") order.totalAmount else (order.totalAmount / 2.0)
                customerDao.updateCustomer(customer.copy(balance = customer.balance + unpaidAmount))
            }
        }

        // 4. Create internal Accounting transaction if some payment is received
        val receivedAmount = when (order.paymentStatus) {
            "Paid" -> order.totalAmount
            "Partial" -> order.totalAmount / 2.0
            else -> 0.0
        }
        if (receivedAmount > 0.0) {
            transactionDao.insertTransaction(
                Transaction(
                    type = "Income",
                    category = "Sale",
                    amount = receivedAmount,
                    description = "Payment received for Sale Order #$orderId (Customer: ${order.customerName})",
                    orderId = orderId
                )
            )
        }
    }

    suspend fun cancelOrder(order: Order) {
        // Mark as cancelled
        orderDao.updateOrder(order.copy(status = "Cancelled"))

        // Add back inventory stock
        for (item in order.items) {
            val invItem = inventoryDao.getInventoryItemById(item.itemId)
            if (invItem != null) {
                inventoryDao.updateInventoryItem(invItem.copy(quantity = invItem.quantity + item.quantity))
            }
        }

        // Adjust customer balance
        if (order.paymentStatus != "Paid") {
            val customer = customerDao.getCustomerById(order.customerId)
            if (customer != null) {
                val unpaidAmount = if (order.paymentStatus == "Unpaid") order.totalAmount else (order.totalAmount / 2.0)
                customerDao.updateCustomer(customer.copy(balance = (customer.balance - unpaidAmount).coerceAtLeast(0.0)))
            }
        }

        // Record adjustment transaction if we refunded
        val receivedAmount = when (order.paymentStatus) {
            "Paid" -> order.totalAmount
            "Partial" -> order.totalAmount / 2.0
            else -> 0.0
        }
        if (receivedAmount > 0.0) {
            transactionDao.insertTransaction(
                Transaction(
                    type = "Expense",
                    category = "Refund",
                    amount = receivedAmount,
                    description = "Refund / Cancellation for Sale Order #${order.id}"
                )
            )
        }
    }

    // --- Purchase Order (PO) Fulfillment Logic ---
    val allPOs: Flow<List<PurchaseOrder>> = purchaseOrderDao.getAllPOs()
    suspend fun getPOById(id: Int) = purchaseOrderDao.getPOById(id)
    suspend fun insertPO(po: PurchaseOrder) = purchaseOrderDao.insertPO(po)

    /**
     * Complete and fulfill a PO from supplier, adding stock, creating expense transaction.
     */
    suspend fun fulfillPurchaseOrder(po: PurchaseOrder) {
        // 1. Mark PO as Received
        purchaseOrderDao.updatePO(po.copy(status = "Received"))

        // 2. Increase inventory stock
        for (item in po.items) {
            val invItem = inventoryDao.getInventoryItemById(item.itemId)
            if (invItem != null) {
                inventoryDao.updateInventoryItem(invItem.copy(quantity = invItem.quantity + item.quantity))
            }
        }

        // 3. Record Accounting expense
        transactionDao.insertTransaction(
            Transaction(
                type = "Expense",
                category = "Inventory Purchase",
                amount = po.totalAmount,
                description = "Fulfilled Purchase Order #${po.id} from Supplier: ${po.supplierName}",
                poId = po.id
            )
        )
    }

    suspend fun cancelPO(po: PurchaseOrder) {
        purchaseOrderDao.updatePO(po.copy(status = "Cancelled"))
    }

    // --- Accounting / Transactions Functions ---
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    suspend fun insertTransaction(transaction: Transaction) = transactionDao.insertTransaction(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)
}
