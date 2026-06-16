package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.ERPRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ERPViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ERPRepository
    val context: Context = application.applicationContext

    // Streams from Repository
    val customers: StateFlow<List<Customer>>
    val inventory: StateFlow<List<InventoryItem>>
    val orders: StateFlow<List<Order>>
    val purchaseOrders: StateFlow<List<PurchaseOrder>>
    val transactions: StateFlow<List<Transaction>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ERPRepository(database)

        customers = repository.allCustomers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        inventory = repository.allInventory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        orders = repository.allOrders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        purchaseOrders = repository.allPOs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        transactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed initial ERP data if everything is absolutely empty - makes it usable instantly!
        viewModelScope.launch {
            customers.first().let { currentCustomers ->
                if (currentCustomers.isEmpty()) {
                    seedDemoData()
                }
            }
        }
    }

    // --- Customer Actions ---
    fun addCustomer(name: String, email: String, phone: String, company: String, initialBalance: Double) {
        viewModelScope.launch {
            repository.insertCustomer(
                Customer(
                    name = name,
                    email = email,
                    phone = phone,
                    company = company,
                    balance = initialBalance
                )
            )
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    // --- Inventory Actions ---
    fun addInventoryItem(sku: String, name: String, category: String, quantity: Int, price: Double, cost: Double, reorderLevel: Int) {
        viewModelScope.launch {
            repository.insertInventoryItem(
                InventoryItem(
                    sku = sku,
                    name = name,
                    category = category,
                    quantity = quantity,
                    price = price,
                    cost = cost,
                    reorderLevel = reorderLevel
                )
            )
        }
    }

    fun updateInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.updateInventoryItem(item)
        }
    }

    fun deleteInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item)
        }
    }

    // --- Sale Order Actions ---
    fun createOrder(customerId: Int, customerName: String, items: List<OrderItem>, paymentStatus: String) {
        viewModelScope.launch {
            val totalAmount = items.sumOf { it.unitPrice * it.quantity }
            val order = Order(
                customerId = customerId,
                customerName = customerName,
                status = "Completed",
                totalAmount = totalAmount,
                paymentStatus = paymentStatus,
                items = items
            )
            repository.createCustomerOrder(order)
        }
    }

    fun cancelOrder(order: Order) {
        viewModelScope.launch {
            repository.cancelOrder(order)
        }
    }

    // --- Purchase Order Actions ---
    fun createPO(supplierName: String, items: List<POItem>) {
        viewModelScope.launch {
            val totalAmount = items.sumOf { it.unitCost * it.quantity }
            val po = PurchaseOrder(
                supplierName = supplierName,
                status = "Ordered",
                totalAmount = totalAmount,
                items = items
            )
            repository.insertPO(po)
        }
    }

    fun fulfillPO(po: PurchaseOrder) {
        viewModelScope.launch {
            repository.fulfillPurchaseOrder(po)
        }
    }

    fun cancelPO(po: PurchaseOrder) {
        viewModelScope.launch {
            repository.cancelPO(po)
        }
    }

    // --- Accounting Actions ---
    fun addManualTransaction(type: String, category: String, amount: Double, description: String) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    type = type,
                    category = category,
                    amount = amount,
                    description = description
                )
            )
        }
    }

    // --- Automated Reporting Data ---
    data class ERPReportMetrics(
        val totalRevenue: Double = 0.0,
        val totalCOGS: Double = 0.0,
        val otherExpenses: Double = 0.0,
        val netProfit: Double = 0.0,
        val marginPercentage: Double = 0.0,
        val totalInventoryValue: Double = 0.0,
        val totalOutstandingReceivables: Double = 0.0,
        val lowStockItemCount: Int = 0,
        val completedSalesCount: Int = 0,
        val pendingPOsCount: Int = 0
    )

    val reportMetrics: StateFlow<ERPReportMetrics> = combine(
        inventory, customers, orders, purchaseOrders, transactions
    ) { inv, cust, ords, pos, txs ->
        val totalRevenue = txs.filter { it.type == "Income" && it.category == "Sale" }.sumOf { it.amount }
        
        // Cost of Goods Sold calculated based on completed orders' unitCost
        val totalCOGS = ords.filter { it.status == "Completed" }.flatMap { it.items }.sumOf { it.unitCost * it.quantity }
        val otherExpenses = txs.filter { it.type == "Expense" }.sumOf { it.amount }
        val netProfit = totalRevenue - (totalCOGS + otherExpenses)
        val margin = if (totalRevenue > 0) (netProfit / totalRevenue) * 100.0 else 0.0

        val invVal = inv.sumOf { it.quantity * it.cost }
        val outstanding = cust.sumOf { it.balance }
        val lowStock = inv.filter { it.quantity <= it.reorderLevel }.size
        val saleCount = ords.filter { it.status == "Completed" }.size
        val pendingPOs = pos.filter { it.status == "Ordered" }.size

        ERPReportMetrics(
            totalRevenue = totalRevenue,
            totalCOGS = totalCOGS,
            otherExpenses = otherExpenses,
            netProfit = netProfit,
            marginPercentage = margin,
            totalInventoryValue = invVal,
            totalOutstandingReceivables = outstanding,
            lowStockItemCount = lowStock,
            completedSalesCount = saleCount,
            pendingPOsCount = pendingPOs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ERPReportMetrics()
    )

    // --- Export Functions ---
    fun exportToCSV(): String {
        val metrics = reportMetrics.value
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val csvBuilder = StringBuilder()

        // Write header & metadata
        csvBuilder.append("Simple ERP - Automated Performance Report\n")
        csvBuilder.append("Generated On,${dateFormat.format(Date())}\n\n")

        // Write metrics
        csvBuilder.append("METRIC,VALUE,DESCRIPTION\n")
        csvBuilder.append("Total Revenue,$${String.format("%.2f", metrics.totalRevenue)},Income received from all inventory sales\n")
        csvBuilder.append("Cost of Goods Sold (COGS),$${String.format("%.2f", metrics.totalCOGS)},Resource acquisition cost of sold items\n")
        csvBuilder.append("Other Operating Expenses,$${String.format("%.2f", metrics.otherExpenses)},Purchases and manual expense transactions\n")
        csvBuilder.append("Net Operating Profit,$${String.format("%.2f", metrics.netProfit)},Revenue minus costs and expenses\n")
        csvBuilder.append("Net Operating Margin,${String.format("%.1f", metrics.marginPercentage)}%,Percentage profit margin\n")
        csvBuilder.append("Inventory Assets Valuation,$${String.format("%.2f", metrics.totalInventoryValue)},Total value of stock in warehouse at cost\n")
        csvBuilder.append("Accounts Receivables,$${String.format("%.2f", metrics.totalOutstandingReceivables)},Outstanding credit owed by customers\n")
        csvBuilder.append("Low Stock ItemsCount,${metrics.lowStockItemCount},Active items below safe reorder thresholds\n")
        csvBuilder.append("Fitted Sale Orders,${metrics.completedSalesCount},Total sales volume fulfilled\n")
        csvBuilder.append("Open Purchase Orders,${metrics.pendingPOsCount},Unfulfilled pending supplier order pipelines\n\n")

        // Customers Sheet
        csvBuilder.append("CUSTOMERS LIST\n")
        csvBuilder.append("ID,Name,Email,Company,Phone,Outstanding Owed Balance\n")
        customers.value.forEach {
            csvBuilder.append("${it.id},\"${it.name.replace("\"", "\"\"")}\",${it.email},\"${it.company.replace("\"", "\"\"")}\",${it.phone},$${String.format("%.2f", it.balance)}\n")
        }
        csvBuilder.append("\n")

        // Inventory Sheet
        csvBuilder.append("INVENTORY LIST\n")
        csvBuilder.append("ID,SKU/Barcode,Item Name,Category,Qty on Hand,Price,Unit Cost,Value at Cost,Status\n")
        inventory.value.forEach {
            val status = if (it.quantity <= it.reorderLevel) "REORDER WARNING" else "In Stock"
            val totalVal = it.quantity * it.cost
            csvBuilder.append("${it.id},${it.sku},\"${it.name.replace("\"", "\"\"")}\",\"${it.category}\",${it.quantity},$${String.format("%.2f", it.price)},$${String.format("%.2f", it.cost)},$${String.format("%.2f", totalVal)},$status\n")
        }

        val csvContent = csvBuilder.toString()
        saveContentToFile("Simple_ERP_Report.csv", csvContent)
        return csvContent
    }

    fun exportToPDF() {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size 595x842
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }

        val paintSub = Paint().apply {
            color = Color.GRAY
            textSize = 10f
        }

        val paintHeader = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            isFakeBoldText = true
        }

        val paintBody = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }

        val paintNegative = Paint().apply {
            color = Color.RED
            textSize = 10f
        }

        val paintPositive = Paint().apply {
            color = Color.rgb(0, 150, 0) // Green
            textSize = 10f
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        var yPosition = 40f

        // Title
        canvas.drawText("SIMPLE ERP - AUTOMATED EXECUTIVE REPORT", 40f, yPosition, paintTitle)
        yPosition += 20f
        canvas.drawText("Generated On: ${dateFormat.format(Date())}", 40f, yPosition, paintSub)
        yPosition += 30f

        // High Level Financial metrics
        canvas.drawText("EXECUTIVE FINANCIAL METRICS", 40f, yPosition, paintHeader)
        yPosition += 5f
        canvas.drawLine(40f, yPosition, 555f, yPosition, paintHeader)
        yPosition += 20f

        val metrics = reportMetrics.value
        val itemsToPrint = listOf(
            "Total Accumulated Revenue" to metrics.totalRevenue,
            "Cost of Goods Sold (COGS)" to metrics.totalCOGS,
            "Operating Expense Ledger" to metrics.otherExpenses,
            "Net ERP Business Profit" to metrics.netProfit,
            "Inventory Assets Evaluation" to metrics.totalInventoryValue,
            "Active Accounts Receivables" to metrics.totalOutstandingReceivables
        )

        for ((label, value) in itemsToPrint) {
            canvas.drawText(label, 40f, yPosition, paintBody)
            val paintToUse = if (label.contains("Profit") && value < 0) paintNegative else if (label.contains("Profit") && value > 0) paintPositive else paintBody
            val formattedValue = if (label.contains("Margin")) "${String.format("%.1f", value)}%" else "$${String.format("%.2f", value)}"
            canvas.drawText(formattedValue, 320f, yPosition, paintToUse)
            yPosition += 20f
        }

        yPosition += 15f
        // High Level Logistical metrics
        canvas.drawText("LOGISTICAL & FULFILLMENT INSIGHTS", 40f, yPosition, paintHeader)
        yPosition += 5f
        canvas.drawLine(40f, yPosition, 555f, yPosition, paintHeader)
        yPosition += 20f

        canvas.drawText("Low Stock Warning Alert Items", 40f, yPosition, paintBody)
        val paintStock = if (metrics.lowStockItemCount > 0) paintNegative else paintBody
        canvas.drawText("${metrics.lowStockItemCount} item(s)", 320f, yPosition, paintStock)
        yPosition += 20f

        canvas.drawText("Fulfilled Orders Counts", 40f, yPosition, paintBody)
        canvas.drawText("${metrics.completedSalesCount}", 320f, yPosition, paintBody)
        yPosition += 20f

        canvas.drawText("Pending POs in Pipeline", 40f, yPosition, paintBody)
        canvas.drawText("${metrics.pendingPOsCount} PO(s)", 320f, yPosition, paintBody)
        yPosition += 30f

        canvas.drawText("Thank you for using Simple ERP system reporting.", 40f, yPosition, paintSub)

        document.finishPage(page)

        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Simple_ERP_Executive_Report.pdf")
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            fos.close()
            Toast.makeText(context, "Executive PDF exported to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            document.close()
        }
    }

    private fun saveContentToFile(fileName: String, content: String) {
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            val fos = FileOutputStream(file)
            fos.write(content.toByteArray())
            fos.close()
            Toast.makeText(context, "$fileName saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save CSV file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Demo Seeding Logic ---
    private suspend fun seedDemoData() {
        // 1. Seed base customers
        val c1 = Customer(name = "Global Retailers", email = "orders@globalretail.com", phone = "555-0192", company = "Global Retail Inc.", balance = 150.0)
        val c2 = Customer(name = "Initech Corp", email = "finance@initech.com", phone = "555-0421", company = "Initech LLC", balance = 0.0)
        val c3 = Customer(name = "Umbrella Logistics", email = "shipping@umbrella.com", phone = "555-8902", company = "Umbrella Corp Ltd", balance = 890.0)
        repository.insertCustomer(c1)
        repository.insertCustomer(c2)
        repository.insertCustomer(c3)

        // 2. Seed inventory items (Skus matched standard test codes)
        val i1 = InventoryItem(sku = "880123456789", name = "HyperX Wireless Mouse", category = "Electronics", quantity = 30, price = 49.99, cost = 20.00, reorderLevel = 10)
        val i2 = InventoryItem(sku = "690123456789", name = "Ducky One 3 Keyboard", category = "Electronics", quantity = 4, price = 119.99, cost = 55.00, reorderLevel = 5) // Low stock!
        val i3 = InventoryItem(sku = "450123456789", name = "Anker USB-C Hub 8-in-1", category = "Accessories", quantity = 18, price = 39.99, cost = 16.00, reorderLevel = 5)
        val i4 = InventoryItem(sku = "120123456789", name = "SteelSeries Headset", category = "Electronics", quantity = 11, price = 79.99, cost = 32.00, reorderLevel = 4)
        repository.insertInventoryItem(i1)
        repository.insertInventoryItem(i2)
        repository.insertInventoryItem(i3)
        repository.insertInventoryItem(i4)

        // Seed some historic transactions & orders to populate the reporting dashboard immediately
        val seedOrder = Order(
            customerId = 1,
            customerName = "Global Retailers",
            status = "Completed",
            totalAmount = 149.97,
            paymentStatus = "Paid",
            items = listOf(
                OrderItem(itemId = 1, itemName = "HyperX Wireless Mouse", sku = "880123456789", quantity = 3, unitPrice = 49.99, unitCost = 20.00)
            )
        )
        repository.createCustomerOrder(seedOrder)

        val seedPO = PurchaseOrder(
            supplierName = "Logitech Supply Ltd",
            status = "Ordered",
            totalAmount = 275.00,
            items = listOf(
                POItem(itemId = 2, itemName = "Ducky One 3 Keyboard", sku = "690123456789", quantity = 5, unitCost = 55.00)
            )
        )
        repository.insertPO(seedPO)

        // Add an operating expense manual transaction
        repository.insertTransaction(
            Transaction(
                type = "Expense",
                category = "Office Lease",
                amount = 350.0,
                description = "Monthly co-working hub seat rental"
            )
        )
    }
}
