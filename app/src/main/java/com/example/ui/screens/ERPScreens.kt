package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.viewmodel.ERPViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ERPScreen(viewModel: ERPViewModel) {
    var currentTab by remember { mutableStateOf(0) }
    var showScanDialog by remember { mutableStateOf(false) }
    var scannedSkuToUse by remember { mutableStateOf("") }

    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val purchaseOrders by viewModel.purchaseOrders.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val reportMetrics by viewModel.reportMetrics.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BusinessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Simple ERP",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showScanDialog = true },
                        modifier = Modifier.testTag("global_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Quick Barcode Scanner",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val items = listOf(
                    NavigationItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, 0),
                    NavigationItem("Inventory", Icons.Filled.Inventory, Icons.Outlined.Inventory, 1),
                    NavigationItem("Customers", Icons.Filled.People, Icons.Outlined.People, 2),
                    NavigationItem("Sales Orders", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, 3),
                    NavigationItem("POs & Ledger", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, 4)
                )
                items.forEach { item ->
                    val selected = currentTab == item.index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = item.index },
                        label = { Text(item.title, fontSize = 11.sp, maxLines = 1) },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                0 -> DashboardTab(viewModel, reportMetrics, transactions) {
                    showScanDialog = true
                }
                1 -> InventoryTab(viewModel, inventory, selectedSkuToScan = scannedSkuToUse, clearScannedSku = { scannedSkuToUse = "" })
                2 -> CustomersTab(viewModel, customers)
                3 -> OrdersTab(viewModel, orders, customers, inventory)
                4 -> LedgerTab(viewModel, purchaseOrders, transactions, inventory)
            }
        }
    }

    if (showScanDialog) {
        BarcodeScannerDialog(
            inventoryItems = inventory,
            onDismiss = { showScanDialog = false },
            onBarcodeScanned = { sku ->
                showScanDialog = false
                scannedSkuToUse = sku
                currentTab = 1 // Switch to Inventory Tab automatically
                Toast.makeText(context, "Scanned SKU: $sku. Redirected to Inventory.", Toast.LENGTH_LONG).show()
            }
        )
    }
}

data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val index: Int
)

// --- DASHBOARD TAB ---
@Composable
fun DashboardTab(
    viewModel: ERPViewModel,
    metrics: ERPViewModel.ERPReportMetrics,
    transactions: List<Transaction>,
    onLaunchScanner: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Executive Welcome Header
            Column {
                Text(
                    text = "Welcome Back,",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = "ERP Overview Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Main Performance Insights Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Sales Revenue",
                    value = "$${String.format("%.2f", metrics.totalRevenue)}",
                    icon = Icons.Default.TrendingUp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Net ERP Profit",
                    value = "$${String.format("%.2f", metrics.netProfit)}",
                    icon = Icons.Default.MonetizationOn,
                    color = if (metrics.netProfit >= 0) Color(0, 150, 0) else Color.Red,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Stock Valuation",
                    value = "$${String.format("%.2f", metrics.totalInventoryValue)}",
                    icon = Icons.Default.Warehouse,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Accounts Receivable",
                    value = "$${String.format("%.2f", metrics.totalOutstandingReceivables)}",
                    icon = Icons.Default.Payments,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Actions Row
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Quick Executive Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onLaunchScanner,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_scan"),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Scan Barcode", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.exportToCSV()
                                Toast.makeText(context, "Performance CSV Exported successfully!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_export_csv"),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.PivotTableChart, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export CSV", fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.exportToPDF()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_action_export_pdf")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export PDF Executive Statement")
                    }
                }
            }
        }

        // Active Alerts Log
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Automated Operating Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Badge(
                        containerColor = if (metrics.lowStockItemCount > 0) MaterialTheme.colorScheme.error else Color.Gray,
                        contentColor = Color.White
                    ) {
                        Text("${metrics.lowStockItemCount} Low Stock Alert(s)", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                if (metrics.lowStockItemCount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Column {
                                Text(
                                    text = "Stock Depletion Warnings",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "${metrics.lowStockItemCount} item catalogs are currently at or below minimum reorder buffer levels. Replenish via Purchase Orders inside the POs tab.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0, 150, 0)
                            )
                            Text(
                                text = "All warehouses report healthy buffer volumes. No low stock anomalies detected.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                              )
                        }
                    }
                }
            }
        }

        // Recent Transaction Mini ledger
        item {
            Text(
                text = "Recent Accounting Ledger Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (transactions.isEmpty()) {
            item {
                Text(
                    text = "No recent operating transactions recorded.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            }
        } else {
            items(transactions.take(5)) { tx ->
                TransactionListItem(transaction = tx)
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// --- INVENTORY TAB ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryTab(
    viewModel: ERPViewModel,
    inventory: List<InventoryItem>,
    selectedSkuToScan: String,
    clearScannedSku: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<InventoryItem?>(null) }

    // Preset search query if redirected from Barcode Scanner
    LaunchedEffect(selectedSkuToScan) {
        if (selectedSkuToScan.isNotBlank()) {
            searchQuery = selectedSkuToScan
            filterCategory = "All"
            clearScannedSku()
        }
    }

    val categories = listOf("All") + inventory.map { it.category }.distinct()

    val filteredItems = inventory.filter {
        (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.sku.contains(searchQuery)) &&
        (filterCategory == "All" || it.category == filterCategory)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inventory Catalog",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_item_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add Item", fontSize = 12.sp)
            }
        }

        // Search & Scan Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Item Name or SKU / Barcode...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inventory_search_field"),
            shape = RoundedCornerShape(12.dp)
        )

        // Preset Categories Pills Scrollable
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(filterCategory).coerceAtLeast(0),
            edgePadding = 0.dp,
            divider = {},
            indicator = {}
        ) {
            categories.forEach { category ->
                val isSelected = filterCategory == category
                Tab(
                    selected = isSelected,
                    onClick = { filterCategory = category },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    SuggestionChip(
                        onClick = { filterCategory = category },
                        label = { Text(category) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.LightGray
                    )
                    Text("No matching inventory items found.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredItems) { item ->
                    InventoryListItem(
                        item = item,
                        onEdit = { selectedItemForEdit = item },
                        onDelete = { viewModel.deleteInventoryItem(item) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        InventoryAddEditDialog(
            item = null,
            onDismiss = { showAddDialog = false },
            onSave = { sku, name, category, qty, price, cost, reorder ->
                viewModel.addInventoryItem(sku, name, category, qty, price, cost, reorder)
                showAddDialog = false
            }
        )
    }

    if (selectedItemForEdit != null) {
        InventoryAddEditDialog(
            item = selectedItemForEdit,
            onDismiss = { selectedItemForEdit = null },
            onSave = { sku, name, category, qty, price, cost, reorder ->
                selectedItemForEdit?.let {
                    viewModel.updateInventoryItem(it.copy(sku = sku, name = name, category = category, quantity = qty, price = price, cost = cost, reorderLevel = reorder))
                }
                selectedItemForEdit = null
            }
        )
    }
}

@Composable
fun InventoryListItem(
    item: InventoryItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = item.quantity <= item.reorderLevel
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("inventory_item_${item.sku}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(item.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("SKU/Barcode: ${item.sku}", fontSize = 12.sp, color = Color.Gray)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit catalog item", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove catalog item", tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Sale Price", fontSize = 11.sp, color = Color.Gray)
                        Text("$${String.format("%.2f", item.price)}", fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Unit Cost", fontSize = 11.sp, color = Color.Gray)
                        Text("$${String.format("%.2f", item.cost)}", color = Color.DarkGray)
                    }
                    Column {
                        Text("Margin", fontSize = 11.sp, color = Color.Gray)
                        val profit = item.price - item.cost
                        val percentage = if (item.price > 0) (profit / item.price) * 100 else 0.0
                        Text("${String.format("%.1f", percentage)}%", color = Color(0, 150, 0), fontWeight = FontWeight.SemiBold)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Quantity", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${item.quantity} units",
                            fontWeight = FontWeight.Bold,
                            color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        if (isLowStock) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Low Stock Alert",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- CUSTOMERS TAB ---
@Composable
fun CustomersTab(viewModel: ERPViewModel, customers: List<Customer>) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }

    val filteredCustomers = customers.filter {
        searchQuery.isBlank() ||
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.company.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Customers Directory",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_customer_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add Customer", fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, company...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("customer_search_field"),
            shape = RoundedCornerShape(12.dp)
        )

        if (filteredCustomers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No customers registered yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCustomers) { customer ->
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().testTag("customer_card_${customer.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(customer.company, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                }
                                Row {
                                    IconButton(onClick = { editingCustomer = customer }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit profiles", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { viewModel.deleteCustomer(customer) }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                        Text(customer.email, fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                        Text(customer.phone, fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Outstanding Credit Balance", fontSize = 10.sp, color = Color.Gray)
                                    Text(
                                        text = if (customer.balance > 0) "$${String.format("%.2f", customer.balance)}" else "$0.00",
                                        fontWeight = FontWeight.Bold,
                                        color = if (customer.balance > 0) MapNegativeColor else Color.Gray,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CustomerAddEditDialog(
            customer = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, email, phone, company, balance ->
                viewModel.addCustomer(name, email, phone, company, balance)
                showAddDialog = false
            }
        )
    }

    if (editingCustomer != null) {
        CustomerAddEditDialog(
            customer = editingCustomer,
            onDismiss = { editingCustomer = null },
            onSave = { name, email, phone, company, balance ->
                editingCustomer?.let {
                    viewModel.updateCustomer(it.copy(name = name, email = email, phone = phone, company = company, balance = balance))
                }
                editingCustomer = null
            }
        )
    }
}

// --- SALES/ORDERS TAB ---
@Composable
fun OrdersTab(
    viewModel: ERPViewModel,
    orders: List<Order>,
    customers: List<Customer>,
    inventory: List<InventoryItem>
) {
    var showCheckoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sales Operations",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { showCheckoutDialog = true },
                modifier = Modifier.testTag("new_sale_button")
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Process New Sale", fontSize = 12.sp)
            }
        }

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No sale orders placed yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(orders) { order ->
                    val isCancelled = order.status == "Cancelled"
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().testTag("order_card_${order.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Order #${order.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(order.orderDate))}", fontSize = 11.sp, color = Color.Gray)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isCancelled) Color.Red.copy(alpha = 0.1f) else Color(0, 150, 0).copy(alpha = 0.1f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = order.status,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCancelled) Color.Red else Color(0, 150, 0)
                                        )
                                    }

                                    if (!isCancelled) {
                                        IconButton(
                                            onClick = { viewModel.cancelOrder(order) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Cancel, contentDescription = "Cancel order", tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Text("Customer: ${order.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))

                            // Item lines
                            Column(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                order.items.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${item.itemName} x${item.quantity}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text("$${String.format("%.2f", item.unitPrice * item.quantity)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Payment:", fontSize = 11.sp, color = Color.Gray)
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (order.paymentStatus == "Paid") Color(0, 150, 0).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(order.paymentStatus, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (order.paymentStatus == "Paid") Color(0, 150, 0) else Color.Red)
                                    }
                                }

                                Text(
                                    text = "Total: $${String.format("%.2f", order.totalAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCheckoutDialog) {
        NewSaleDialog(
            customers = customers,
            inventory = inventory,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { customerId, customerName, items, paymentStatus ->
                viewModel.createOrder(customerId, customerName, items, paymentStatus)
                showCheckoutDialog = false
            }
        )
    }
}

// --- POs / ACCOUNTING LEDGER TAB ---
@Composable
fun LedgerTab(
    viewModel: ERPViewModel,
    purchaseOrders: List<PurchaseOrder>,
    transactions: List<Transaction>,
    inventory: List<InventoryItem>
) {
    var insideTabState by remember { mutableStateOf(0) } // 0: Purchase Orders, 1: Bookkeeping Ledger
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showNewPODialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toggle Hub
        TabRow(
            selectedTabIndex = insideTabState,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = insideTabState == 0,
                onClick = { insideTabState = 0 },
                text = { Text("Purchase Orders (POs)", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = insideTabState == 1,
                onClick = { insideTabState = 1 },
                text = { Text("Bookkeeping Ledger", fontWeight = FontWeight.Bold) }
            )
        }

        if (insideTabState == 0) {
            // Purchase Orders sub-tab
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Supplier Supply Pipelines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showNewPODialog = true },
                    modifier = Modifier.testTag("new_po_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Issue New PO", fontSize = 11.sp)
                }
            }

            if (purchaseOrders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No supplier Purchase Orders registered.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(purchaseOrders) { po ->
                        val isReceived = po.status == "Received"
                        val isCancelled = po.status == "Cancelled"
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().testTag("po_card_${po.id}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("PO #${po.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("Issued: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(po.poDate))}", fontSize = 11.sp, color = Color.Gray)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    when (po.status) {
                                                        "Received" -> Color(0, 150, 0).copy(alpha = 0.1f)
                                                        "Ordered" -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> Color.Red.copy(alpha = 0.1f)
                                                    },
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = po.status,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (po.status) {
                                                    "Received" -> Color(0, 150, 0)
                                                    "Ordered" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    else -> Color.Red
                                                }
                                            )
                                        }

                                        if (po.status == "Ordered") {
                                            IconButton(
                                                onClick = { viewModel.cancelPO(po) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Cancel, contentDescription = "Cancel PO", tint = Color.Red, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text("Supplier: ${po.supplierName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Spacer(Modifier.height(8.dp))

                                // PO Items List
                                Column(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    po.items.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("${item.itemName} x${item.quantity}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                            Text("Cost: $${String.format("%.2f", item.unitCost * item.quantity)}", fontSize = 12.sp, color = Color.DarkGray)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total Investment: $${String.format("%.2f", po.totalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    if (po.status == "Ordered") {
                                        Button(
                                            onClick = { viewModel.fulfillPO(po) },
                                            modifier = Modifier.testTag("fulfill_po_button_${po.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0, 150, 0))
                                        ) {
                                            Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Fulfill & Add Stock", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // General Ledger Transaction Bookkeeping sub-tab
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Operating Accounting Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showExpenseDialog = true },
                    modifier = Modifier.testTag("add_expense_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("Record Custom Expense", fontSize = 11.sp, color = Color.White)
                }
            }

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions logged in current ledger.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { tx ->
                        TransactionListItem(transaction = tx)
                    }
                }
            }
        }
    }

    if (showExpenseDialog) {
        RecordExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            onSave = { category, amount, description ->
                viewModel.addManualTransaction("Expense", category, amount, description)
                showExpenseDialog = false
            }
        )
    }

    if (showNewPODialog) {
        NewPODialog(
            inventoryItems = inventory,
            onDismiss = { showNewPODialog = false },
            onConfirm = { supplier, itemsList ->
                viewModel.createPO(supplier, itemsList)
                showNewPODialog = false
            }
        )
    }
}

@Composable
fun TransactionListItem(transaction: Transaction) {
    val isIncome = transaction.type == "Income"
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isIncome) Color(0, 150, 0).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (isIncome) Color(0, 150, 0) else Color.Red,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.category,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = (if (isIncome) "+" else "-") + "$${String.format("%.2f", transaction.amount)}",
                        fontWeight = FontWeight.Bold,
                        color = if (isIncome) Color(0, 150, 0) else Color.Red,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(transaction.description, fontSize = 12.sp, color = Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(transaction.date)),
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

// --- BARCODE SCANNER CAMERA SIMULATOR DIALOG ---
@Composable
fun BarcodeScannerDialog(
    inventoryItems: List<InventoryItem>,
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    var selectionIndex by remember { mutableStateOf(0) }
    var inputSku by remember { mutableStateOf("") }

    val scanAnimationValue = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scanAnimationValue.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("barcode_scanner_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Camera Finder Scanner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                // Camera viewfinder screen mockup with line scan sweep effect
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Frame Corner graphics
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 8f
                        val len = 40f
                        // Top Left
                        drawLine(Color.White, Offset(20f, 20f), Offset(20f + len, 20f), strokeWidth = stroke)
                        drawLine(Color.White, Offset(20f, 20f), Offset(20f, 20f + len), strokeWidth = stroke)
                        // Top Right
                        drawLine(Color.White, Offset(size.width - 20f, 20f), Offset(size.width - 20f - len, 20f), strokeWidth = stroke)
                        drawLine(Color.White, Offset(size.width - 20f, 20f), Offset(size.width - 20f, 20f + len), strokeWidth = stroke)
                        // Bottom Left
                        drawLine(Color.White, Offset(20f, size.height - 20f), Offset(20f + len, size.height - 20f), strokeWidth = stroke)
                        drawLine(Color.White, Offset(20f, size.height - 20f), Offset(20f, size.height - 20f - len), strokeWidth = stroke)
                        // Bottom Right
                        drawLine(Color.White, Offset(size.width - 20f, size.height - 20f), Offset(size.width - 20f - len, size.height - 20f), strokeWidth = stroke)
                        drawLine(Color.White, Offset(size.width - 20f, size.height - 20f), Offset(size.width - 20f, size.height - 20f - len), strokeWidth = stroke)
                    }

                    // Barcode graphic in the middle
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(72.dp)
                    )

                    // Laser sweep line animation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (20 + (140 * scanAnimationValue.value)).dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.Red, Color.Transparent)
                                )
                            )
                    )

                    Text(
                        text = "Aim camera at barcode",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = "Automated Preset Testing Barcodes:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Select from existing mock inventory barcodes to instantly trigger scan simulated
                LazyColumn(
                    modifier = Modifier.height(130.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(inventoryItems) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                .clickable { onBarcodeScanned(item.sku) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("SKU: ${item.sku}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Divider(color = Color.LightGray.copy(alpha = 0.5f))

                // Manual SKU Entry Fallback
                OutlinedTextField(
                    value = inputSku,
                    onValueChange = { inputSku = it },
                    placeholder = { Text("Or Type SKU / Barcode manually...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("scanner_manual_input")
                )

                Button(
                    onClick = { if (inputSku.isNotBlank()) onBarcodeScanned(inputSku) },
                    enabled = inputSku.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("scanner_manual_submit_button")
                ) {
                    Text("Submit Barcode Scan")
                }
            }
        }
    }
}

// --- ADAPTIVE ADD/EDIT INVENTORY DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryAddEditDialog(
    item: InventoryItem?,
    onDismiss: () -> Unit,
    onSave: (sku: String, name: String, category: String, qty: Int, price: Double, cost: Double, reorder: Int) -> Unit
) {
    var sku by remember { mutableStateOf(item?.sku ?: "") }
    var name by remember { mutableStateOf(item?.name ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "Electronics") }
    var qtyString by remember { mutableStateOf(item?.quantity?.toString() ?: "") }
    var priceString by remember { mutableStateOf(item?.price?.toString() ?: "") }
    var costString by remember { mutableStateOf(item?.cost?.toString() ?: "") }
    var reorderString by remember { mutableStateOf(item?.reorderLevel?.toString() ?: "5") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("inventory_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (item == null) "Add Inventory Catalog Item" else "Edit Catalog Item",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_item_name")
                )

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU / Barcode *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_item_sku")
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_item_category")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = qtyString,
                        onValueChange = { qtyString = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("input_item_qty")
                    )
                    OutlinedTextField(
                        value = reorderString,
                        onValueChange = { reorderString = it },
                        label = { Text("Reorder Trigger") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("input_item_reorder")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = priceString,
                        onValueChange = { priceString = it },
                        label = { Text("Sale Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("input_item_price")
                    )
                    OutlinedTextField(
                        value = costString,
                        onValueChange = { costString = it },
                        label = { Text("Unit Cost ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("input_item_cost")
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = qtyString.toIntOrNull() ?: 0
                            val price = priceString.toDoubleOrNull() ?: 0.0
                            val cost = costString.toDoubleOrNull() ?: 0.0
                            val reorder = reorderString.toIntOrNull() ?: 5
                            if (sku.isNotBlank() && name.isNotBlank() && category.isNotBlank()) {
                                onSave(sku, name, category, qty, price, cost, reorder)
                            }
                        },
                        enabled = sku.isNotBlank() && name.isNotBlank() && category.isNotBlank(),
                        modifier = Modifier.testTag("save_item_submit")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// --- ADAPTIVE ADD/EDIT CUSTOMER DIALOG ---
@Composable
fun CustomerAddEditDialog(
    customer: Customer?,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, phone: String, company: String, balance: Double) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var email by remember { mutableStateOf(customer?.email ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var company by remember { mutableStateOf(customer?.company ?: "") }
    var balanceString by remember { mutableStateOf(customer?.balance?.toString() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("customer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (customer == null) "Register New Customer" else "Edit Customer Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_customer_name")
                )

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_customer_company")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("input_customer_email")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("input_customer_phone")
                )

                OutlinedTextField(
                    value = balanceString,
                    onValueChange = { balanceString = it },
                    label = { Text("Outstanding Owed Balance ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_customer_balance")
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val balance = balanceString.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank()) {
                                onSave(name, email, phone, company, balance)
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.testTag("save_customer_submit")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// --- RECORD MANUAL EXPENSE DIALOG ---
@Composable
fun RecordExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (category: String, amount: Double, description: String) -> Unit
) {
    var category by remember { mutableStateOf("Operating Expense") }
    var amountString by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val presetCategories = listOf("Rent", "Utility Bills", "Operating Expense", "Salary", "Supplier Surcharges", "Logistic Carriage")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("expense_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Record Operating Expense Entry", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Text("Expense Asset Class:", fontSize = 11.sp, color = Color.Gray)
                ScrollableTabRow(
                    selectedTabIndex = presetCategories.indexOf(category).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {}
                ) {
                    presetCategories.forEach { cat ->
                        val isSelected = cat == category
                        Tab(
                            selected = isSelected,
                            onClick = { category = cat },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            SuggestionChip(
                                onClick = { category = cat },
                                label = { Text(cat) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountString,
                    onValueChange = { amountString = it },
                    label = { Text("Expense Amount ($) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_amount")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Vendor Reference *") },
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_desc")
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountString.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0 && description.isNotBlank()) {
                                onSave(category, amt, description)
                            }
                        },
                        enabled = amountString.isNotBlank() && description.isNotBlank(),
                        modifier = Modifier.testTag("save_expense_submit")
                    ) {
                        Text("Record Entry")
                    }
                }
            }
        }
    }
}

// --- NEW SALE POS CHECKOUT DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleDialog(
    customers: List<Customer>,
    inventory: List<InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (customerId: Int, customerName: String, items: List<OrderItem>, paymentStatus: String) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(customers.firstOrNull()) }
    var selectedItemsMap = remember { mutableStateMapOf<Int, Int>() } // ItemId to Quantity
    var paymentStatus by remember { mutableStateOf("Paid") } // "Paid", "Unpaid", "Partial"

    val totalCost = inventory.filter { selectedItemsMap.containsKey(it.id) }.sumOf {
        val qty = selectedItemsMap[it.id] ?: 0
        it.price * qty
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("sale_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Process Customer Checkout", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                // Customer Dropdown Mock
                Text("Select Buying Client Profile:", fontSize = 11.sp, color = Color.Gray)
                ScrollableTabRow(
                    selectedTabIndex = customers.indexOf(selectedCustomer).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {}
                ) {
                    customers.forEach { cust ->
                        val isSelected = cust.id == selectedCustomer?.id
                        Tab(
                            selected = isSelected,
                            onClick = { selectedCustomer = cust },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            SuggestionChip(
                                onClick = { selectedCustomer = cust },
                                label = { Text(cust.name) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }

                Text("Assemble Basket Items:", fontSize = 11.sp, color = Color.Gray)
                LazyColumn(
                    modifier = Modifier.height(150.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(inventory) { item ->
                        val qtySelected = selectedItemsMap[item.id] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Stock: ${item.quantity}  •  $${String.format("%.2f", item.price)}", fontSize = 11.sp, color = Color.DarkGray)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (qtySelected > 0) {
                                    IconButton(
                                        onClick = { selectedItemsMap[item.id] = (qtySelected - 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color.Gray)
                                    }
                                    Text("$qtySelected", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                IconButton(
                                    onClick = {
                                        if (qtySelected < item.quantity) {
                                            selectedItemsMap[item.id] = qtySelected + 1
                                        }
                                    },
                                    enabled = qtySelected < item.quantity,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Divider(color = Color.LightGray.copy(alpha = 0.5f))

                // Payment Status Configuration
                Text("Settlement Method:", fontSize = 11.sp, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Paid", "Unpaid", "Partial").forEach { status ->
                        val isSelected = paymentStatus == status
                        Button(
                            onClick = { paymentStatus = status },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text(status, fontSize = 12.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Bill Summary", fontSize = 11.sp, color = Color.Gray)
                        Text("$${String.format("%.2f", totalCost)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val currentCustomer = selectedCustomer
                                if (currentCustomer != null && selectedItemsMap.isNotEmpty()) {
                                    val orderItems = selectedItemsMap.filter { it.value > 0 }.mapNotNull { (itemId, qty) ->
                                        val stockItem = inventory.find { it.id == itemId }
                                        if (stockItem != null) {
                                            OrderItem(
                                                itemId = itemId,
                                                itemName = stockItem.name,
                                                sku = stockItem.sku,
                                                quantity = qty,
                                                unitPrice = stockItem.price,
                                                unitCost = stockItem.cost
                                            )
                                        } else null
                                    }
                                    if (orderItems.isNotEmpty()) {
                                        onConfirm(currentCustomer.id, currentCustomer.name, orderItems, paymentStatus)
                                    }
                                }
                            },
                            enabled = selectedCustomer != null && selectedItemsMap.any { it.value > 0 },
                            modifier = Modifier.testTag("save_sale_submit")
                        ) {
                            Text("Process Sale")
                        }
                    }
                }
            }
        }
    }
}

// --- NEW SUPPLIER PURCHASE ORDER DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPODialog(
    inventoryItems: List<InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (supplier: String, items: List<POItem>) -> Unit
) {
    var supplierName by remember { mutableStateOf("") }
    var poItemsMap = remember { mutableStateMapOf<Int, Int>() } // ItemId to Purchase Qty

    val totalInvestment = inventoryItems.filter { poItemsMap.containsKey(it.id) }.sumOf {
        val qty = poItemsMap[it.id] ?: 0
        it.cost * qty
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("po_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Draft New Supplier Purchase Order", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Supplier Company / Manufacturer *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_po_supplier")
                )

                Text("Replenish Catalog items:", fontSize = 11.sp, color = Color.Gray)
                LazyColumn(
                    modifier = Modifier.height(150.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(inventoryItems) { item ->
                        val qtySelected = poItemsMap[item.id] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Current Stock: ${item.quantity}  •  Unit Cost: $${String.format("%.2f", item.cost)}", fontSize = 11.sp, color = Color.DarkGray)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (qtySelected > 0) {
                                    IconButton(
                                        onClick = { poItemsMap[item.id] = (qtySelected - 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color.Gray)
                                    }
                                    Text("$qtySelected", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                IconButton(
                                    onClick = { poItemsMap[item.id] = qtySelected + 1 },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Divider(color = Color.LightGray.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Accrued Investment Value", fontSize = 11.sp, color = Color.Gray)
                        Text("$${String.format("%.2f", totalInvestment)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (supplierName.isNotBlank() && poItemsMap.isNotEmpty()) {
                                    val mappedItems = poItemsMap.filter { it.value > 0 }.mapNotNull { (itemId, qty) ->
                                        val stockItem = inventoryItems.find { it.id == itemId }
                                        if (stockItem != null) {
                                            POItem(
                                                itemId = itemId,
                                                itemName = stockItem.name,
                                                sku = stockItem.sku,
                                                quantity = qty,
                                                unitCost = stockItem.cost
                                            )
                                        } else null
                                    }
                                    if (mappedItems.isNotEmpty()) {
                                        onConfirm(supplierName, mappedItems)
                                    }
                                }
                            },
                            enabled = supplierName.isNotBlank() && poItemsMap.any { it.value > 0 },
                            modifier = Modifier.testTag("save_po_submit")
                        ) {
                            Text("Issue PO")
                        }
                    }
                }
            }
        }
    }
}

// Global colors matching Material 3 styles
val MapNegativeColor = Color(200, 0, 0)
