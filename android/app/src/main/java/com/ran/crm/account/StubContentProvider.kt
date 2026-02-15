package com.ran.crm.account

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Stub ContentProvider required by the SyncAdapter framework.
 *
 * The SyncAdapter framework requires a ContentProvider to be declared with the same authority as
 * the sync adapter. Since we don't use a ContentProvider for data access (we use Room), this is a
 * no-op stub.
 */
class StubContentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true
    override fun query(
            uri: Uri,
            p: Array<out String>?,
            s: String?,
            sa: Array<out String>?,
            so: String?
    ): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, sa: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, s: String?, sa: Array<out String>?): Int =
            0
}
