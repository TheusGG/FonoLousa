package com.fonolousa.app.data

import android.content.Context
import android.content.res.AssetManager
import com.google.gson.Gson

class DataRepository(context: Context) {
    private val assets: AssetManager = context.applicationContext.assets
    private val gson = Gson()

    val database: FonoDatabase by lazy {
        assets.open("database.json").bufferedReader(Charsets.UTF_8).use { reader ->
            gson.fromJson(reader, FonoDatabase::class.java)
        }
    }

    fun categorias(): List<Categoria> = database.categorias

    fun categoria(id: String): Categoria =
        database.categorias.first { it.id == id }

    fun nivel(categoryId: String, level: Int): Nivel =
        categoria(categoryId).niveis.first { it.nivel == level }

    fun item(categoryId: String, level: Int, index: Int): ItemFono =
        nivel(categoryId, level).itens[index.coerceIn(0, nivel(categoryId, level).itens.lastIndex)]

    fun itensDaCategoria(categoryId: String): List<ItemFono> =
        categoria(categoryId)
            .niveis
            .flatMap { it.itens }
            .distinctBy { it.palavra.trim().lowercase() }

    fun assetPath(path: String): String = path.removePrefix("assets/")

    fun assetExists(path: String): Boolean {
        return try {
            assets.open(assetPath(path)).close()
            true
        } catch (_: Exception) {
            false
        }
    }
}
