package com.alkhair.dawaidiary

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Medicine(val id: String, val name: String, val dose: String, val time: String)
data class DoseLog(val medicine: String, val dose: String, val status: String, val timestamp: Long)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("dawai_diary", Context.MODE_PRIVATE) }
    private val medicines = mutableListOf<Medicine>()
    private val logs = mutableListOf<DoseLog>()
    private lateinit var content: LinearLayout

    private val ink = Color.rgb(28, 43, 54)
    private val muted = Color.rgb(107, 122, 132)
    private val teal = Color.rgb(22, 125, 117)
    private val bg = Color.rgb(244, 247, 248)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadData()
        render()
    }

    private fun loadData() {
        medicines.clear(); logs.clear()
        try {
            val a = JSONArray(prefs.getString("medicines", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                medicines.add(Medicine(o.getString("id"), o.getString("name"), o.getString("dose"), o.getString("time")))
            }
            val l = JSONArray(prefs.getString("logs", "[]"))
            for (i in 0 until l.length()) {
                val o = l.getJSONObject(i)
                logs.add(DoseLog(o.getString("medicine"), o.getString("dose"), o.getString("status"), o.getLong("timestamp")))
            }
        } catch (_: Exception) { }
    }

    private fun saveData() {
        val a = JSONArray()
        medicines.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("dose", it.dose).put("time", it.time)) }
        val l = JSONArray()
        logs.take(300).forEach { l.put(JSONObject().put("medicine", it.medicine).put("dose", it.dose).put("status", it.status).put("timestamp", it.timestamp)) }
        prefs.edit().putString("medicines", a.toString()).putString("logs", l.toString()).apply()
    }

    private fun render() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(28))
        }
        scroll.addView(content)
        setContentView(scroll)

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val heading = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        heading.addView(text("💊  Dawai Diary", 26, ink, true))
        heading.addView(text("Your personal medicine tracker", 14, muted))
        header.addView(heading)
        header.addView(button("＋ Add", teal) { addMedicine() })
        content.addView(header)
        space(18)

        val today = todayLogs()
        val taken = today.count { it.status == "TAKEN" }
        val total = medicines.size
        card {
            addView(text("📊  TODAY'S OVERVIEW", 12, muted, true))
            space(8)
            addView(text("$taken of $total medicines logged as taken", 19, ink, true))
            space(8)
            val progress = ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal)
            progress.max = maxOf(total, 1); progress.progress = taken
            progress.progressTintList = android.content.res.ColorStateList.valueOf(teal)
            addView(progress, LinearLayout.LayoutParams(-1, dp(8)))
            space(5)
            addView(text(if (total == 0) "Add your medicines to start tracking." else "${(taken * 100 / total)}% completed today", 13, muted))
        }
        space(20)

        sectionTitle("💊  My medicines")
        if (medicines.isEmpty()) {
            card { addView(text("No medicines added yet.\nTap ＋ Add to create your first medicine record.", 15, muted)) }
        } else {
            medicines.forEach { med ->
                card {
                    val top = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                    val labels = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
                    labels.addView(text(med.name, 18, ink, true))
                    labels.addView(text("${med.dose}  •  🕒 ${med.time}", 14, muted))
                    top.addView(labels)
                    top.addView(button("✏️", Color.rgb(235, 241, 242)) { editMedicine(med) })
                    top.addView(button("🗑", Color.rgb(255, 239, 238)) { deleteMedicine(med) })
                    addView(top)
                    space(12)
                    val status = todayLogs().lastOrNull { it.medicine == med.name }
                    if (status != null) {
                        addView(text(if (status.status == "TAKEN") "✅ Taken today" else "⏭ Skipped today", 13,
                            if (status.status == "TAKEN") teal else muted, true))
                        space(8)
                    }
                    val actions = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
                    actions.addView(button("✅  Taken", teal) { logDose(med, "TAKEN") }, LinearLayout.LayoutParams(0, dp(44), 1f))
                    val gap = View(this@MainActivity); actions.addView(gap, LinearLayout.LayoutParams(dp(8), 1))
                    actions.addView(button("⏭  Skip", Color.rgb(92, 108, 119)) { logDose(med, "SKIPPED") }, LinearLayout.LayoutParams(0, dp(44), 1f))
                    addView(actions)
                }
                space(10)
            }
        }

        space(12)
        sectionTitle("🧾  Recent activity")
        val recent = logs.sortedByDescending { it.timestamp }.take(12)
        if (recent.isEmpty()) {
            card { addView(text("Your dose history will appear here.", 14, muted)) }
        } else {
            card {
                recent.forEachIndexed { index, log ->
                    val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                    val left = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
                    left.addView(text(log.medicine, 15, ink, true))
                    left.addView(text("${log.dose}  •  ${dateTime(log.timestamp)}", 12, muted))
                    row.addView(left)
                    row.addView(text(if (log.status == "TAKEN") "✅ Taken" else "⏭ Skipped", 13,
                        if (log.status == "TAKEN") teal else muted, true))
                    addView(row)
                    if (index != recent.lastIndex) {
                        val line = View(this@MainActivity).apply { setBackgroundColor(Color.rgb(232, 237, 239)) }
                        addView(line, LinearLayout.LayoutParams(-1, dp(1)))
                        space(10)
                    }
                }
            }
        }
        space(14)
        content.addView(text("🔒  Your records are saved on this device. This app does not replace medical advice.", 12, muted))
    }

    private fun addMedicine() {
        val fields = formFields()
        AlertDialog.Builder(this).setTitle("Add medicine")
            .setView(fields.first)
            .setPositiveButton("Save") { _, _ ->
                val (name, dose, time) = fields.second()
                if (name.isBlank() || dose.isBlank()) { toast("Enter medicine name and dose"); return@setPositiveButton }
                medicines.add(Medicine(UUID.randomUUID().toString(), name, dose, time.ifBlank { "Not set" }))
                saveData(); render()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun editMedicine(m: Medicine) {
        val fields = formFields(m)
        AlertDialog.Builder(this).setTitle("Edit medicine").setView(fields.first)
            .setPositiveButton("Save") { _, _ ->
                val (name, dose, time) = fields.second()
                if (name.isBlank() || dose.isBlank()) { toast("Name and dose are required"); return@setPositiveButton }
                val i = medicines.indexOfFirst { it.id == m.id }
                if (i >= 0) medicines[i] = m.copy(name = name, dose = dose, time = time.ifBlank { "Not set" })
                saveData(); render()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun formFields(m: Medicine? = null): Pair<LinearLayout, () -> Triple<String,String,String>> {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(8), dp(22), dp(4)) }
        val name = edit("Medicine name", m?.name ?: "")
        val dose = edit("Dose (e.g. 10 mg)", m?.dose ?: "")
        val time = edit("Usual time (e.g. 08:00 AM)", m?.time?.takeUnless { it == "Not set" } ?: "")
        box.addView(name); box.addView(dose); box.addView(time)
        return Pair(box) { Triple(name.text.toString().trim(), dose.text.toString().trim(), time.text.toString().trim()) }
    }

    private fun edit(hintText: String, value: String): EditText = EditText(this).apply {
        hint = hintText; setText(value); textSize = 15f; maxLines = 1
        setPadding(dp(12), dp(8), dp(12), dp(8))
    }

    private fun deleteMedicine(m: Medicine) {
        AlertDialog.Builder(this).setTitle("Delete ${m.name}?")
            .setMessage("Its existing history entries will remain in your activity.")
            .setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
                medicines.removeAll { it.id == m.id }; saveData(); render()
            }.show()
    }

    private fun logDose(m: Medicine, status: String) {
        logs.add(0, DoseLog(m.name, m.dose, status, System.currentTimeMillis()))
        saveData(); render()
        toast(if (status == "TAKEN") "Dose recorded as taken" else "Dose recorded as skipped")
    }

    private fun todayLogs(): List<DoseLog> {
        val f = SimpleDateFormat("yyyyMMdd", Locale.US)
        val today = f.format(Date())
        return logs.filter { f.format(Date(it.timestamp)) == today }
    }

    private fun dateTime(t: Long) = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(t))

    private fun sectionTitle(s: String) { content.addView(text(s, 18, ink, true)); space(10) }
    private fun space(h: Int) { content.addView(View(this).apply { setBackgroundColor(Color.TRANSPARENT) }, LinearLayout.LayoutParams(1, dp(h))) }
    private fun text(s: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size.toFloat(); setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun button(label: String, color: Int, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 13f; isAllCaps = false; setTextColor(if (color == Color.rgb(235,241,242) || color == Color.rgb(255,239,238)) ink else white)
        background = rounded(color, 12); setPadding(dp(12), 0, dp(12), 0); setOnClickListener { action() }
        minHeight = dp(42); minimumHeight = dp(42)
    }
    private fun card(block: LinearLayout.() -> Unit) {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(15), dp(16), dp(15))
            background = rounded(white, 18); elevation = dp(1).toFloat()
            block()
        }
        content.addView(c, LinearLayout.LayoutParams(-1, -2))
    }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
