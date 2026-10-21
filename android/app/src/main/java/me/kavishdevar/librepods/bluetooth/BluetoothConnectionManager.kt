/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.ParcelUuid
import android.util.Log

object BluetoothConnectionManager {
    var aacpSocket: BluetoothSocket? = null
    /**
     * True if a disconnect came from an older socket's reader while a newer connection is
     * already up, so it shouldn't tear that connection down. [endedSocketId] is the
     * identityHashCode of the socket that ended, or 0 if unknown.
     */
    fun isStaleDisconnect(endedSocketId: Int): Boolean {
        val current = aacpSocket ?: return false
        return endedSocketId != 0 && current.isConnected &&
            System.identityHashCode(current) != endedSocketId
    }
}

fun createBluetoothSocket(
    adapter: BluetoothAdapter, device: BluetoothDevice, uuid: ParcelUuid, psm: Int
): BluetoothSocket {
    val type = 3 // L2CAP
    val constructorSpecs = listOf(
        arrayOf(adapter, device, type, true, true, psm, uuid), // A16QPR3
        arrayOf(device, type, true, true, psm, uuid),
        arrayOf(device, type, 1, true, true, psm, uuid),
        arrayOf(type, 1, true, true, device, psm, uuid),
        arrayOf(type, true, true, device, psm, uuid)
    )

    val constructors = BluetoothSocket::class.java.declaredConstructors
    Log.d("createSocket<psm>", "BluetoothSocket has ${constructors.size} constructors:")

    constructors.forEachIndexed { index, constructor ->
        val params = constructor.parameterTypes.joinToString(", ") { it.simpleName }
        Log.d("createSocket<psm>", "Constructor $index: ($params)")
    }

    var lastException: Exception? = null
    var attemptedConstructors = 0

    for ((index, params) in constructorSpecs.withIndex()) {
        try {
            Log.d("createSocket<psm>", "Trying constructor signature #${index + 1}")
            attemptedConstructors++

            val paramTypes =
                params.map { it::class.javaPrimitiveType ?: it::class.java }.toTypedArray()
            val constructor = BluetoothSocket::class.java.getDeclaredConstructor(*paramTypes)
            constructor.isAccessible = true
            return constructor.newInstance(*params) as BluetoothSocket

        } catch (e: Exception) {
            Log.e("createSocket<psm>", "Constructor signature #${index + 1} failed: ${e.message}")
            lastException = e
        }
    }

    val errorMessage =
        "Failed to create BluetoothSocket after trying $attemptedConstructors constructor signatures"
    Log.e("createSocket<psm>", errorMessage)
    throw lastException ?: IllegalStateException(errorMessage)
}
