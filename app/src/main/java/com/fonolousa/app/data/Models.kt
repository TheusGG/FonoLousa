package com.fonolousa.app.data

data class FonoDatabase(
    val app: String,
    val versao: String,
    val categorias: List<Categoria>
)

data class Categoria(
    val id: String,
    val nome: String,
    val cor: String,
    val niveis: List<Nivel>
)

data class Nivel(
    val nivel: Int,
    val descricao: String,
    val itens: List<ItemFono>
)

data class ItemFono(
    val id: String,
    val palavra: String,
    val arquivoImagem: String,
    val arquivoSom: String,
    val frase: String,
    val promptImagem: String
)
