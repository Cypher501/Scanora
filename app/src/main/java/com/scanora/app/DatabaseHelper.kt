package com.scanora.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "educoin.db", null, 1) {

    // ─────────────────────────────────────────
    //  CREATE TABLES
    // ─────────────────────────────────────────
    override fun onCreate(db: SQLiteDatabase) {

        // Students
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS students (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                barcode     TEXT    UNIQUE NOT NULL,
                name        TEXT    NOT NULL,
                dept        TEXT,
                batch       TEXT,
                pin         TEXT    DEFAULT '0000',
                coins       REAL    DEFAULT 0.0,
                daily_limit REAL    DEFAULT 500.0,
                active      INTEGER DEFAULT 1,
                created_at  TEXT
            )
        """.trimIndent())

        // Vendors (canteen, xerox, etc.)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS vendors (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                name       TEXT    NOT NULL,
                type       TEXT,
                username   TEXT    UNIQUE NOT NULL,
                password   TEXT    NOT NULL,
                earnings   REAL    DEFAULT 0.0,
                active     INTEGER DEFAULT 1
            )
        """.trimIndent())

        // Transactions
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS transactions (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                barcode     TEXT    NOT NULL,
                type        TEXT    NOT NULL,
                amount      REAL    NOT NULL,
                balance_after REAL,
                vendor_id   INTEGER DEFAULT 0,
                description TEXT,
                timestamp   TEXT
            )
        """.trimIndent())

        // Coupon codes
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS coupons (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                code       TEXT    UNIQUE NOT NULL,
                amount     REAL    NOT NULL,
                used       INTEGER DEFAULT 0,
                used_by    TEXT,
                created_at TEXT
            )
        """.trimIndent())

        // Seed default admin vendor
        db.execSQL("""
            INSERT OR IGNORE INTO vendors
            (name, type, username, password, active)
            VALUES ('Admin', 'admin', 'admin', 'scanora2024', 1)
        """.trimIndent())

        // Seed default canteen vendor
        db.execSQL("""
            INSERT OR IGNORE INTO vendors
            (name, type, username, password, active)
            VALUES ('Main Canteen', 'canteen', 'canteen1', 'canteen123', 1)
        """.trimIndent())
    }

    override fun onUpgrade(
        db: SQLiteDatabase, oldVersion: Int, newVersion: Int
    ) {
        db.execSQL("DROP TABLE IF EXISTS students")
        db.execSQL("DROP TABLE IF EXISTS vendors")
        db.execSQL("DROP TABLE IF EXISTS transactions")
        db.execSQL("DROP TABLE IF EXISTS coupons")
        onCreate(db)
    }

    // ─────────────────────────────────────────
    //  TIMESTAMP HELPER
    // ─────────────────────────────────────────
    private fun now(): String =
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
        ).format(Date())

    // ─────────────────────────────────────────
    //  STUDENT OPERATIONS
    // ─────────────────────────────────────────

    fun addStudent(
        barcode: String,
        name: String,
        dept: String,
        batch: String,
        pin: String = "0000"
    ): Boolean {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("barcode",    barcode.trim().uppercase())
                put("name",       name)
                put("dept",       dept)
                put("batch",      batch)
                put("pin",        pin)
                put("coins",      0.0)
                put("active",     1)
                put("created_at", now())
            }
            db.insertOrThrow("students", null, cv)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getStudent(barcode: String): JSONObject? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM students WHERE barcode = ? AND active = 1",
            arrayOf(barcode.trim().uppercase())
        )
        return if (cursor.moveToFirst()) {
            val obj = JSONObject()
            for (i in 0 until cursor.columnCount) {
                obj.put(cursor.getColumnName(i), cursor.getString(i) ?: "")
            }
            cursor.close()
            obj
        } else {
            cursor.close()
            null
        }
    }

    fun getAllStudents(): JSONArray {
        val db  = readableDatabase
        val arr = JSONArray()
        val cursor = db.rawQuery(
            "SELECT * FROM students ORDER BY name ASC", null
        )
        while (cursor.moveToNext()) {
            val obj = JSONObject()
            for (i in 0 until cursor.columnCount) {
                obj.put(cursor.getColumnName(i), cursor.getString(i) ?: "")
            }
            arr.put(obj)
        }
        cursor.close()
        return arr
    }

    fun getBalance(barcode: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT coins FROM students WHERE barcode = ?",
            arrayOf(barcode.trim().uppercase())
        )
        val balance = if (cursor.moveToFirst()) {
            cursor.getDouble(0)
        } else 0.0
        cursor.close()
        return balance
    }

    fun verifyPin(barcode: String, pin: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT pin FROM students WHERE barcode = ? AND active = 1",
            arrayOf(barcode.trim().uppercase())
        )
        val valid = if (cursor.moveToFirst()) {
            cursor.getString(0) == pin
        } else false
        cursor.close()
        return valid
    }

    // ─────────────────────────────────────────
    //  COIN OPERATIONS
    // ─────────────────────────────────────────

    fun addCoins(
        barcode: String,
        amount: Double,
        description: String = "Admin recharge"
    ): Boolean {
        return try {
            val db      = writableDatabase
            val current = getBalance(barcode)
            val newBal  = current + amount

            db.execSQL(
                "UPDATE students SET coins = ? WHERE barcode = ?",
                arrayOf(newBal, barcode.trim().uppercase())
            )

            logTransaction(
                barcode     = barcode,
                type        = "CREDIT",
                amount      = amount,
                balanceAfter = newBal,
                vendorId    = 0,
                description = description
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deductCoins(
        barcode: String,
        amount: Double,
        vendorId: Int,
        description: String
    ): Pair<Boolean, String> {
        val current = getBalance(barcode)

        if (current < amount) {
            return Pair(false, "Insufficient coins. Balance: $current")
        }

        return try {
            val db     = writableDatabase
            val newBal = current - amount

            db.execSQL(
                "UPDATE students SET coins = ? WHERE barcode = ?",
                arrayOf(newBal, barcode.trim().uppercase())
            )

            // Add to vendor earnings
            db.execSQL(
                "UPDATE vendors SET earnings = earnings + ? WHERE id = ?",
                arrayOf(amount, vendorId)
            )

            logTransaction(
                barcode      = barcode,
                type         = "DEBIT",
                amount       = amount,
                balanceAfter = newBal,
                vendorId     = vendorId,
                description  = description
            )

            Pair(true, "Success. New balance: $newBal coins")
        } catch (e: Exception) {
            Pair(false, "Transaction failed: ${e.message}")
        }
    }

    fun transferCoins(
        fromBarcode: String,
        toBarcode: String,
        amount: Double,
        pin: String
    ): Pair<Boolean, String> {
        if (!verifyPin(fromBarcode, pin)) {
            return Pair(false, "Invalid PIN")
        }

        val fromBalance = getBalance(fromBarcode)
        if (fromBalance < amount) {
            return Pair(false, "Insufficient coins")
        }

        val toStudent = getStudent(toBarcode)
            ?: return Pair(false, "Recipient not found")

        return try {
            val db = writableDatabase

            db.execSQL(
                "UPDATE students SET coins = coins - ? WHERE barcode = ?",
                arrayOf(amount, fromBarcode.trim().uppercase())
            )
            db.execSQL(
                "UPDATE students SET coins = coins + ? WHERE barcode = ?",
                arrayOf(amount, toBarcode.trim().uppercase())
            )

            val newFromBal = getBalance(fromBarcode)
            val newToBal   = getBalance(toBarcode)

            logTransaction(fromBarcode, "TRANSFER_OUT", amount,
                newFromBal, 0,
                "Transfer to ${toStudent.getString("name")}")
            logTransaction(toBarcode, "TRANSFER_IN", amount,
                newToBal, 0,
                "Transfer from $fromBarcode")

            Pair(true, "Sent $amount coins to ${toStudent.getString("name")}")
        } catch (e: Exception) {
            Pair(false, "Transfer failed: ${e.message}")
        }
    }

    // ─────────────────────────────────────────
    //  COUPON OPERATIONS
    // ─────────────────────────────────────────

    fun generateCoupon(amount: Double): String {
        val code = "SCAN${(100000..999999).random()}"
        val db   = writableDatabase
        val cv   = ContentValues().apply {
            put("code",       code)
            put("amount",     amount)
            put("used",       0)
            put("created_at", now())
        }
        db.insert("coupons", null, cv)
        return code
    }

    fun redeemCoupon(
        code: String,
        barcode: String
    ): Pair<Boolean, String> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM coupons WHERE code = ? AND used = 0",
            arrayOf(code.trim().uppercase())
        )

        return if (cursor.moveToFirst()) {
            val amount = cursor.getDouble(
                cursor.getColumnIndexOrThrow("amount")
            )
            cursor.close()

            writableDatabase.execSQL(
                "UPDATE coupons SET used = 1, used_by = ? WHERE code = ?",
                arrayOf(barcode, code.trim().uppercase())
            )
            addCoins(barcode, amount, "Coupon: $code")
            Pair(true, "Redeemed! $amount coins added")
        } else {
            cursor.close()
            Pair(false, "Invalid or already used coupon")
        }
    }

    // ─────────────────────────────────────────
    //  TRANSACTION LOG
    // ─────────────────────────────────────────

    private fun logTransaction(
        barcode: String,
        type: String,
        amount: Double,
        balanceAfter: Double,
        vendorId: Int,
        description: String
    ) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("barcode",       barcode.trim().uppercase())
            put("type",          type)
            put("amount",        amount)
            put("balance_after", balanceAfter)
            put("vendor_id",     vendorId)
            put("description",   description)
            put("timestamp",     now())
        }
        db.insert("transactions", null, cv)
    }

    fun getTransactions(
        barcode: String? = null,
        limit: Int = 50
    ): JSONArray {
        val db  = readableDatabase
        val arr = JSONArray()
        val query = if (barcode != null) {
            db.rawQuery(
                """SELECT t.*, s.name as student_name,
                   v.name as vendor_name
                   FROM transactions t
                   LEFT JOIN students s ON t.barcode = s.barcode
                   LEFT JOIN vendors v ON t.vendor_id = v.id
                   WHERE t.barcode = ?
                   ORDER BY t.timestamp DESC LIMIT ?""",
                arrayOf(barcode.trim().uppercase(), limit.toString())
            )
        } else {
            db.rawQuery(
                """SELECT t.*, s.name as student_name,
                   v.name as vendor_name
                   FROM transactions t
                   LEFT JOIN students s ON t.barcode = s.barcode
                   LEFT JOIN vendors v ON t.vendor_id = v.id
                   ORDER BY t.timestamp DESC LIMIT ?""",
                arrayOf(limit.toString())
            )
        }
        while (query.moveToNext()) {
            val obj = JSONObject()
            for (i in 0 until query.columnCount) {
                obj.put(query.getColumnName(i), query.getString(i) ?: "")
            }
            arr.put(obj)
        }
        query.close()
        return arr
    }

    // ─────────────────────────────────────────
    //  VENDOR OPERATIONS
    // ─────────────────────────────────────────

    fun verifyVendor(
        username: String,
        password: String
    ): JSONObject? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """SELECT * FROM vendors
               WHERE username = ? AND password = ?
               AND active = 1""",
            arrayOf(username, password)
        )
        return if (cursor.moveToFirst()) {
            val obj = JSONObject()
            for (i in 0 until cursor.columnCount) {
                obj.put(cursor.getColumnName(i), cursor.getString(i) ?: "")
            }
            cursor.close()
            obj
        } else {
            cursor.close()
            null
        }
    }

    fun getVendorEarnings(vendorId: Int): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT earnings FROM vendors WHERE id = ?",
            arrayOf(vendorId.toString())
        )
        val earnings = if (cursor.moveToFirst()) {
            cursor.getDouble(0)
        } else 0.0
        cursor.close()
        return earnings
    }

    fun getAllVendors(): JSONArray {
        val db  = readableDatabase
        val arr = JSONArray()
        val cursor = db.rawQuery(
            "SELECT id, name, type, username, earnings FROM vendors",
            null
        )
        while (cursor.moveToNext()) {
            val obj = JSONObject()
            for (i in 0 until cursor.columnCount) {
                obj.put(cursor.getColumnName(i), cursor.getString(i) ?: "")
            }
            arr.put(obj)
        }
        cursor.close()
        return arr
    }

    // ─────────────────────────────────────────
    //  STATS FOR DASHBOARD
    // ─────────────────────────────────────────

    fun getStats(): JSONObject {
        val db  = readableDatabase
        val obj = JSONObject()

        // Total students
        val sc = db.rawQuery(
            "SELECT COUNT(*) FROM students WHERE active = 1", null
        )
        sc.moveToFirst()
        obj.put("total_students", sc.getInt(0))
        sc.close()

        // Total coins in circulation
        val cc = db.rawQuery(
            "SELECT SUM(coins) FROM students WHERE active = 1", null
        )
        cc.moveToFirst()
        obj.put("total_coins", cc.getDouble(0))
        cc.close()

        // Total transactions today
        val today = SimpleDateFormat(
            "yyyy-MM-dd", Locale.getDefault()
        ).format(Date())
        val tc = db.rawQuery(
            "SELECT COUNT(*), SUM(amount) FROM transactions WHERE timestamp LIKE ?",
            arrayOf("$today%")
        )
        tc.moveToFirst()
        obj.put("today_transactions", tc.getInt(0))
        obj.put("today_volume",       tc.getDouble(1))
        tc.close()

        return obj
    }
}