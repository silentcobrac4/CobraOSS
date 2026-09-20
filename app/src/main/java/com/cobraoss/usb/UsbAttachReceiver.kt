package com.cobraoss.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UsbAttachReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Device enumeration is performed on demand through the public UsbManager API.
    }
}
