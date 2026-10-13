package com.zenitharranger.model

import android.content.Context
import android.os.Build
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BeatPresetManager {

    fun sanitizeName(name: String): String =
        name.trim().replace("\\s+".toRegex(), "_").replace("[\\\\/:*?\"<>|]".toRegex(), "")

    fun getStylesDir(context: Context): File {
        // 1. App-specific external storage (requires ZERO permissions across all Android versions)
        try {
            val ext = context.getExternalFilesDir("Styles")
            if (ext != null && (ext.exists() || ext.mkdirs())) return ext
        } catch (_: Exception) {}

        // 2. Public Music folder only if MANAGE_EXTERNAL_STORAGE was explicitly granted
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                val pubZenith = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "ZenithArranger/Styles")
                if (pubZenith.exists() || pubZenith.mkdirs()) return pubZenith
            }
        } catch (_: Exception) {}

        // 3. Fallback: Internal app sandbox storage
        val fb = File(context.filesDir, "styles")
        if (!fb.exists()) fb.mkdirs()
        return fb
    }

    fun ensureInitialized(context: Context) {
        try {
            val root = getStylesDir(context)
            val factoryDir = File(root, "Factory")
            val userDir = File(root, "User")
            File(userDir, "My_Beats").mkdirs()

            getFactoryPresets().forEach { preset ->
                val cleanFolder = sanitizeName(preset.folder)
                val cleanName = sanitizeName(preset.name)
                val styleDir = File(File(factoryDir, cleanFolder), cleanName)
                styleDir.mkdirs()
                val manifest = File(styleDir, "style.json")
                if (!manifest.exists()) {
                    manifest.writeText(serializePreset(preset.copy(id = cleanName, folder = cleanFolder)).toString(2))
                }
            }
        } catch (_: Exception) {
            // Failsafe: never crash app on storage initialization
        }
    }

    fun getFactoryPresets(): List<BeatPreset> = listOf(
        BeatPreset(
            id = "Pop_Standard_4_4", name = "Pop Standard (4/4)", bpm = 120f,
            timeSigNum = 4, timeSigDen = 4, folder = "Pop_&_Rock", category = "Factory",
            sections = listOf(
                ArrangerSection("main_a", "Main A", ArrangerSectionType.MAIN, 2),
                ArrangerSection("fill_a", "Fill A", ArrangerSectionType.FILL, 1),
                ArrangerSection("main_b", "Main B", ArrangerSectionType.MAIN, 2),
                ArrangerSection("fill_b", "Fill B", ArrangerSectionType.FILL, 1),
                ArrangerSection("intro", "Intro", ArrangerSectionType.INTRO, 2),
                ArrangerSection("ending", "Ending", ArrangerSectionType.ENDING, 2)
            )
        ),
        BeatPreset(
            id = "Waltz_Groove_3_4", name = "Waltz Groove (3/4)", bpm = 136f,
            timeSigNum = 3, timeSigDen = 4, folder = "Ballad_&_Waltz", category = "Factory",
            sections = listOf(
                ArrangerSection("waltz_a", "Main A", ArrangerSectionType.MAIN, 2),
                ArrangerSection("waltz_fill", "Fill", ArrangerSectionType.FILL, 1),
                ArrangerSection("waltz_b", "Main B", ArrangerSectionType.MAIN, 2),
                ArrangerSection("waltz_end", "Ending", ArrangerSectionType.ENDING, 2)
            )
        ),
        BeatPreset(
            id = "Balkan_Drive_7_8", name = "Balkan Drive (7/8)", bpm = 140f,
            timeSigNum = 7, timeSigDen = 8, folder = "World_Dance", category = "Factory",
            sections = listOf(
                ArrangerSection("balkan_groove", "Groove", ArrangerSectionType.MAIN, 2),
                ArrangerSection("balkan_fill", "Break Fill", ArrangerSectionType.FILL, 1),
                ArrangerSection("balkan_climax", "Climax", ArrangerSectionType.MAIN, 2)
            )
        ),
        BeatPreset(
            id = "Slow_Blues_6_8", name = "Slow Blues (6/8)", bpm = 72f,
            timeSigNum = 6, timeSigDen = 8, folder = "Blues_&_Jazz", category = "Factory",
            sections = listOf(
                ArrangerSection("blues_verse", "Verse", ArrangerSectionType.MAIN, 2),
                ArrangerSection("blues_fill", "Fill", ArrangerSectionType.FILL, 1),
                ArrangerSection("blues_chorus", "Chorus", ArrangerSectionType.MAIN, 2),
                ArrangerSection("blues_outro", "Outro", ArrangerSectionType.ENDING, 2)
            )
        )
    )

    fun listSubfolders(context: Context, category: String): List<String> {
        ensureInitialized(context)
        val catDir = File(getStylesDir(context), category)
        val defaultList = if (category == "Factory")
            listOf("Pop_&_Rock", "Ballad_&_Waltz", "World_Dance", "Blues_&_Jazz")
        else listOf("My_Beats")
        val dirs = catDir.listFiles { f -> f.isDirectory }?.map { it.name } ?: emptyList()
        return (defaultList + dirs).distinct()
    }

    fun listPresetsInSubfolder(context: Context, category: String, subfolder: String): List<BeatPreset> {
        ensureInitialized(context)
        val folderDir = File(File(getStylesDir(context), category), subfolder)
        val list = mutableListOf<BeatPreset>()
        folderDir.listFiles()?.forEach { file ->
            val manifest = if (file.isDirectory) File(file, "style.json") else if (file.extension == "json") file else null
            if (manifest != null && manifest.exists()) {
                try {
                    val json = JSONObject(manifest.readText())
                    val styleDir = if (file.isDirectory) file else file.parentFile
                    list.add(deserializePreset(json, subfolder, category, styleDir))
                } catch (_: Exception) {}
            }
        }
        return list
    }

    fun listPresets(context: Context): List<BeatPreset> {
        ensureInitialized(context)
        val list = mutableListOf<BeatPreset>()
        val root = getStylesDir(context)
        root.walkTopDown().forEach { file ->
            val isManifest = (file.name == "style.json") || (file.extension == "json" && file.parentFile?.name != "style")
            if (isManifest && file.isFile) {
                try {
                    val cat = if (file.absolutePath.contains("/Factory/")) "Factory" else "User"
                    val styleDir = if (file.name == "style.json") file.parentFile else null
                    val sub = (if (file.name == "style.json") styleDir?.parentFile?.name else file.parentFile?.name) ?: "Styles"
                    list.add(deserializePreset(JSONObject(file.readText()), sub, cat, styleDir))
                } catch (_: Exception) {}
            }
        }
        return if (list.isEmpty()) getFactoryPresets() else list.distinctBy { it.id }
    }

    fun saveUserPreset(context: Context, preset: BeatPreset, subfolder: String): Boolean {
        return try {
            ensureInitialized(context)
            val cleanSub = sanitizeName(subfolder.ifBlank { "My_Beats" })
            val cleanStyleName = sanitizeName(preset.name.ifBlank { "Untitled_Style" })
            val styleDir = File(File(File(getStylesDir(context), "User"), cleanSub), cleanStyleName)
            styleDir.mkdirs()

            val updatedSections = preset.sections.map { section ->
                val sourcePath = section.samplePath
                if (sourcePath.isNotBlank()) {
                    val src = File(sourcePath)
                    if (src.exists()) {
                        val ext = src.extension.ifBlank { "wav" }
                        val sampleFileName = "${sanitizeName(section.name)}_${preset.bpm.toInt()}_${preset.timeSigNum}-${preset.timeSigDen}_${section.bars}bar.$ext"
                        val dest = File(styleDir, sampleFileName)
                        if (src.absolutePath != dest.absolutePath) src.copyTo(dest, overwrite = true)
                        section.copy(samplePath = dest.absolutePath, sampleName = sampleFileName)
                    } else section
                } else section
            }
            val savedPreset = preset.copy(
                id = cleanStyleName,
                name = preset.name,
                folder = cleanSub,
                category = "User",
                sections = updatedSections
            )
            File(styleDir, "style.json").writeText(serializePreset(savedPreset).toString(2))
            true
        } catch (_: Exception) { false }
    }

    fun deletePreset(context: Context, id: String): Boolean {
        val root = getStylesDir(context)
        val cleanId = sanitizeName(id)
        root.walkTopDown().forEach { f ->
            if (f.isDirectory && f.name == cleanId && File(f, "style.json").exists()) {
                return f.deleteRecursively()
            } else if (f.isFile && f.name == "$cleanId.json") {
                return f.delete()
            }
        }
        return false
    }

    private fun serializePreset(p: BeatPreset): JSONObject {
        val json = JSONObject()
        json.put("id", p.id)
        json.put("name", p.name)
        json.put("bpm", p.bpm.toDouble())
        json.put("timeSigNum", p.timeSigNum)
        json.put("timeSigDen", p.timeSigDen)
        json.put("folder", p.folder)
        json.put("category", p.category)
        json.put("createdAt", p.createdAt)

        val arr = JSONArray()
        p.sections.forEach { s ->
            val sObj = JSONObject()
            sObj.put("id", s.id)
            sObj.put("name", s.name)
            sObj.put("type", s.type.rawValue)
            sObj.put("bars", s.bars)
            sObj.put("samplePath", s.samplePath)
            sObj.put("sampleName", s.sampleName)
            sObj.put("sampleBpm", s.sampleBpm.toDouble())
            arr.put(sObj)
        }
        json.put("sections", arr)
        return json
    }

    private fun deserializePreset(json: JSONObject, folder: String, category: String, dir: File?): BeatPreset {
        val sectionsList = mutableListOf<ArrangerSection>()
        val arr = json.optJSONArray("sections") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val sName = o.optString("sampleName", "")
            val rawPath = o.optString("samplePath", "")
            val resolvedPath = if (dir != null && sName.isNotBlank() && File(dir, sName).exists()) {
                File(dir, sName).absolutePath
            } else rawPath

            sectionsList.add(
                ArrangerSection(
                    id = o.optString("id", "sec_$i"),
                    name = o.optString("name", "Section $i"),
                    type = ArrangerSectionType.fromRaw(o.optInt("type", 0)),
                    bars = o.optInt("bars", 2),
                    samplePath = resolvedPath,
                    sampleName = sName,
                    sampleBpm = o.optDouble("sampleBpm", 120.0).toFloat()
                )
            )
        }
        return BeatPreset(
            id = json.getString("id"),
            name = json.getString("name"),
            bpm = json.optDouble("bpm", 120.0).toFloat(),
            timeSigNum = json.optInt("timeSigNum", 4),
            timeSigDen = json.optInt("timeSigDen", 4),
            sections = sectionsList,
            folder = json.optString("folder", folder),
            category = category,
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
