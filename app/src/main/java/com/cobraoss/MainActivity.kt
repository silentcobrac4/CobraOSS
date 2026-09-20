package com.cobraoss

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cobraoss.setup.CoreAssetInstaller
import com.cobraoss.terminal.TerminalSession
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CobraTheme { CobraApp(::requestPermissions) } }
    }

    private fun requestPermissions() {
        val requested = buildList {
            if (Build.VERSION.SDK_INT >= 31) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (requested.isNotEmpty()) permissionLauncher.launch(requested.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CobraApp(requestPermissions: () -> Unit) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf("Dashboard") }
    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.padding(8.dp))
                listOf("Dashboard", "Terminal", "USB Devices", "BLE Scanner", "Network Diagnostics", "Exports").forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item) }, selected = page == item,
                        onClick = { page = item; scope.launch { drawer.close() } },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(topBar = {
            TopAppBar(title = { Text("CobraOSS") }, navigationIcon = {
                IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Default.Menu, "Menu") }
            })
        }) { padding ->
            when (page) {
                "Dashboard" -> Dashboard(Modifier.padding(padding), requestPermissions)
                "Terminal" -> TerminalView(Modifier.padding(padding))
                else -> SimplePage(page, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun Dashboard(modifier: Modifier, requestPermissions: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    val installer = remember { CoreAssetInstaller(context) }
    val root = remember(refresh) { installer.install() }
    val usbCount = remember(refresh) {
        (context.getSystemService(Context.USB_SERVICE) as UsbManager).deviceList.size
    }
    val network = remember(refresh) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
    val battery = remember(refresh) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Security diagnostics", style = MaterialTheme.typography.headlineSmall)
        Text("Use only on devices and networks you own or are authorized to assess.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = requestPermissions) { Text("Review permissions") }
        StatusCard("Core environment", "Ready", root.absolutePath)
        StatusCard("USB host", "$usbCount device(s)", "Public UsbManager API")
        StatusCard("Network", if (network) "Connected" else "Offline", "ConnectivityManager")
        StatusCard("Battery", if (battery >= 0) "$battery%" else "Unknown", "BatteryManager")
        Text("Quick actions", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionCard("USB", Icons.Default.Cable, Modifier.weight(1f))
            ActionCard("BLE", Icons.Default.Bluetooth, Modifier.weight(1f))
            ActionCard("Shell", Icons.Default.Terminal, Modifier.weight(1f))
        }
        Button(onClick = { refresh++ }) { Text("Refresh status") }
    }
}

@Composable
private fun StatusCard(title: String, value: String, detail: String) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(value, style = MaterialTheme.typography.headlineSmall); Text(detail, style = MaterialTheme.typography.bodySmall) } }
}

@Composable
private fun ActionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(modifier) { Column(Modifier.padding(12.dp)) { Icon(icon, title); Text(title) } }
}

@Composable
private fun SimplePage(title: String, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text("This module is available in the safe diagnostics MVP. Add authorized, bounded operations here.")
    }
}

@Composable
private fun TerminalView(modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = remember { TerminalSession(context) }
    var output by remember { mutableStateOf("CobraOSS sandbox terminal\n$ ") }
    var command by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { session.start { text -> output += text } }
    Column(modifier.fillMaxSize().background(Color.Black).padding(8.dp)) {
        Text(output, color = Color(0xFFB9F6CA), modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedTextField(command, { command = it }, label = { Text("Command") }, modifier = Modifier.weight(1f), singleLine = true)
            Button(onClick = { val value = command.trim(); if (value.isNotEmpty()) { scope.launch { session.writeLine(value) }; command = "" } }) { Text("Run") }
        }
    }
}

@Composable
private fun CobraTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
