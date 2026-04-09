// Necessário no Gradle:
// implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
// plugins { kotlin("plugin.serialization") }

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File
import java.net.URL

/**
 * Representa uma música individual já processada.
 *
 * id          -> ID único da música (extraído da URL ?gfcMus=)
 * title       -> título limpo da música
 * difficulties-> lista normalizada (easy, medium, hard, expert ou ["all"])
 * filtering   -> string otimizada para busca (sem espaços e lowercase)
 */
@Serializable
data class Song(
  val id: String,
  val title: String,
  val difficulties: List<String>,
  val filtering: String
)

/**
 * Agrupamento final por artista.
 *
 * artist -> nome do artista (ou "unknown")
 * songs  -> lista de músicas ordenadas por título
 */
@Serializable
data class ArtistGroup(
  val artist: String,
  val songs: List<Song>
)

fun main() {

  // Total de páginas informado pelo site
  val totalPages = 88

  // Usado para deduplicação IMEDIATA (regra importante)
  val seenIds = HashSet<String>()

  // Mapa principal: artista -> lista de músicas
  val artistMap = HashMap<String, MutableList<Song>>()

  // Percorre todas as páginas
  for (page in 0 until totalPages) {

    println("entering in the page $page...")

    val url = "https://guitarflash.com/custom/lista.asp?pag=$page"

    // Baixa o HTML da página
    val html = URL(url).readText()

    /**
     * Divide o HTML em linhas de tabela
     * drop(2) remove:
     *  - header da tabela
     *  - primeira linha (thead)
     */
    val rows = html.split("<tr>").drop(2)

    for (row in rows) {

      // ignora qualquer linha que não tenha música
      if (!row.contains("?gfcMus=")) continue

      // extrai o ID da música
      val id = extractId(row) ?: continue

      // deduplicação imediata (regra crítica de performance)
      if (!seenIds.add(id)) continue

      // nome bruto (ex: "Maniac - Michael Sembello")
      val rawName = extractName(row)

      // dificuldades brutas (ex: "Fácil, Médio, Difícil, Expert")
      val difficulties = extractDifficulties(row)

      /**
       * Pipeline de limpeza:
       * 1. remove "chart by"
       * 2. faz split título/artista
       */
      val (title, artist) = splitTitleArtist(cleanName(rawName))

      // normaliza artista
      val normalizedArtist = normalizeArtist(artist)

      // cria string de busca
      val filtering = buildFiltering(title, normalizedArtist)

      // cria objeto final da música
      val song = Song(
        id = id,
        title = title,
        difficulties = normalizeDifficulties(difficulties),
        filtering = filtering
      )

      // adiciona no agrupamento por artista
      artistMap.getOrPut(normalizedArtist) { mutableListOf() }.add(song)
    }
  }

  /**
   * Montagem final:
   * - ordena músicas por título
   * - ordena artistas
   */
  val result = artistMap.map { (artist, songs) ->
    ArtistGroup(
      artist = artist,
      songs = songs.sortedBy { it.title }
    )
  }.sortedBy { it.artist }

  // JSON final formatado
  val json = Json { prettyPrint = true }

  // salva em arquivo
  File("songs.json").writeText(json.encodeToString(result))
}

/**
 * Extrai o ID da música da URL.
 *
 * Exemplo:
 * href="...?gfcMus=HUCDL2GD0TFN"
 */
fun extractId(row: String): String? {
  val start = row.indexOf("?gfcMus=")
  if (start == -1) return null

  val end = row.indexOf("\"", start)

  return row.substring(start + 8, end)
}

/**
 * Extrai o texto do link (nome da música).
 */
fun extractName(row: String): String {
  val start = row.indexOf("\">") + 2
  val end = row.indexOf("</a>")
  return row.substring(start, end).trim()
}

/**
 * Extrai o conteúdo da célula de dificuldades.
 */
fun extractDifficulties(row: String): String {
  val start = row.lastIndexOf("<td>") + 4
  val end = row.indexOf("</td>", start)
  return row.substring(start, end)
}

/**
 * 🔹 LIMPEZA DO NOME
 *
 * Remove tudo após "chart by" (case insensitive)
 */
fun cleanName(name: String): String {
  val lower = name.lowercase()
  val idx = lower.indexOf("chart by")

  return if (idx != -1) {
    name.substring(0, idx).trim()
  } else name
}

/**
 * 🔹 SPLIT (title / artist)
 *
 * Ordem obrigatória:
 * 1. " por "
 * 2. " by "
 * 3. " - "
 * 4. fallback
 */
fun splitTitleArtist(input: String): Pair<String, String> {
  return when {

    input.contains(" por ") -> {
      val parts = input.split(" por ", limit = 2)
      parts[0] to parts[1]
    }

    input.contains(" by ") -> {
      val parts = input.split(" by ", limit = 2)
      parts[0] to parts[1]
    }

    input.contains(" - ") -> {
      val parts = input.split(" - ", limit = 2)
      parts[0] to parts[1]
    }

    else -> input to "unknown"
  }
}

/**
 * 🔹 NORMALIZAÇÃO DE ARTISTA
 */
fun normalizeArtist(artist: String): String {
  val a = artist.trim()
  val lower = a.lowercase()

  return if (
    a.isEmpty() ||
    a == "-" || a == "--" || a == " - " ||
    lower == "unknown" ||
    lower == "no artist" ||
    lower == "none"
  ) {
    "unknown"
  } else a
}

/**
 * 🔹 DIFICULDADES
 *
 * - traduz
 * - remove duplicados
 * - ordena
 * - converte para ["all"] se tiver todas
 */
fun normalizeDifficulties(raw: String): List<String> {

  val map = mapOf(
    "fácil" to "easy",
    "médio" to "medium",
    "difícil" to "hard",
    "expert" to "expert"
  )

  val list = raw.split(",")
    .asSequence() // melhor performance
    .map { it.trim().lowercase() }
    .mapNotNull { map[it] }
    .toSet()      // remove duplicados
    .toList()
    .sorted()

  return if (list.containsAll(listOf("easy", "medium", "hard", "expert"))) {
    listOf("all")
  } else list
}

/**
 * 🔹 FILTERING
 *
 * - artist + title (se tiver artista)
 * - senão só title
 * - remove espaços
 * - lowercase
 */
fun buildFiltering(title: String, artist: String): String {
  val base = if (artist != "unknown") artist + title else title
  return base.replace(" ", "").lowercase()
}