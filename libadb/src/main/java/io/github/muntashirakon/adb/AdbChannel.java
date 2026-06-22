// SPDX-License-Identifier: GPL-3.0-or-later OR Apache-2.0
//
// A2AM-USB: lokale toevoeging aan de vendored libadb-android (beslispunt 2b).
// Niet in upstream. Zelfde dual-licentie als de rest van de module.

package io.github.muntashirakon.adb;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Een transport-abstractie voor {@link AdbConnection}. Upstream maakt zijn {@link java.net.Socket}
 * in een private constructor zonder injectie-seam; deze interface laat een alternatief transport
 * (bv. een USB-ADB-host via {@code UsbDeviceConnection.bulkTransfer}) de ruwe byte-stromen leveren
 * waarover hetzelfde A_CNXN/A_AUTH/A_OPEN-protocol + de stream-multiplexer draaien.
 *
 * <p>Belangrijk: het ADB-bericht (24-byte header + payload) wordt door {@link AdbConnection#sendPacket}
 * als één {@code write(byte[])} aangeboden, gevolgd door {@code flush()}. Een USB-implementatie mag
 * dat als precies één ADB-bericht behandelen (header + optionele payload) en zelf framen naar de
 * bulk-endpoints. Aan de leeskant volstaat een bufferende {@link InputStream}: de parser leest
 * 24 header-bytes en daarna {@code data_length} payload-bytes, ongeacht USB-transfergrenzen.
 */
public interface AdbChannel extends Closeable {
    /** Ruwe inkomende byte-stroom van de adbd (bufferend; framing-onafhankelijk). */
    @NonNull
    InputStream getInputStream() throws IOException;

    /** Ruwe uitgaande byte-stroom naar de adbd. Eén {@code write()} = één volledig ADB-bericht. */
    @NonNull
    OutputStream getOutputStream() throws IOException;

    /** Of het onderliggende transport nog open/verbonden is. */
    boolean isConnected();
}
