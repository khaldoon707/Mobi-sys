package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val phone: String,
    val company: String,
    val balance: Double = 0.0 // positive means they owe us, negative means we owe them
)

@Entity(tableName = "inventory")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sku: String, // barcode or item code
    val name: String,
    val category: String,
    val quantity: Int,
    val price: Double,
    val cost: Double,
    val reorderLevel: Int = 5
)

data class OrderItem(
    val itemId: Int,
    val itemName: String,
    val sku: String,
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val customerName: String,
    val orderDate: Long = System.currentTimeMillis(),
    val status: String, // "Pending", "Completed", "Cancelled"
    val totalAmount: Double,
    val paymentStatus: String, // "Paid", "Unpaid", "Partial"
    val items: List<OrderItem>
)

data class POItem(
    val itemId: Int,
    val itemName: String,
    val sku: String,
    val quantity: Int,
    val unitCost: Double
)

@Entity(tableName = "purchase_orders")
data class PurchaseOrder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val supplierName: String,
    val poDate: Long = System.currentTimeMillis(),
    val status: String, // "Ordered", "Received", "Cancelled"
    val totalAmount: Double,
    val items: List<POItem>
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val type: String, // "Income" or "Expense"
    val category: String, // "Sale", "Inventory Purchase", "Operating Expense", etc.
    val amount: Double,
    val description: String,
    val orderId: Int? = null,
    val poId: Int? = null
)
