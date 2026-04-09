import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File

/**
 * Representa uma música no formato ORIGINAL (antes da compactação).
 *
 * Esse formato é "legível", mas altamente redundante:
 * - Strings repetidas (principalmente artista)
 * - Lista de dificuldades como texto (ineficiente)
 * - Estrutura agrupada que aumenta overhead
 */
@Serializable
data class OldSong(
  val id: String,
  val title: String,
  val difficulties: List<String>,
  val filtering: String
)

/**
 * Agrupamento original por artista.
 *
 * Problema:
 * - O nome do artista é repetido implicitamente em todas as músicas
 * - Estrutura aninhada gera mais bytes no JSON
 */
@Serializable
data class OldArtistGroup(
  val artist: String,
  val songs: List<OldSong>
)

/**
 * Representa uma música no formato COMPACTADO.
 *
 * Estratégias aplicadas:
 *
 * 1. Chaves curtas:
 *    - t = title
 *    - a = artist index
 *    - d = difficulties (bitmask)
 *    - f = filtering
 *
 * 2. Remoção de redundância:
 *    - artista não é mais string → é índice (Int)
 *
 * 3. Compactação de dados:
 *    - dificuldades viram bitmask (Int ao invés de lista de strings)
 */
@Serializable
data class NewSong(
  val id: String,

  // título da música (mantido como string pois é único por item)
  val t: String,

  // índice do artista na lista global "artists"
  val a: Int,

  // dificuldades codificadas em bitmask (ver função encodeDifficulties)
  val d: Int,

  // string otimizada para busca (mantida por performance)
  val f: String
)

/**
 * Estrutura final compactada.
 *
 * Separação clara entre:
 * - tabela de artistas (sem repetição)
 * - lista de músicas (referenciando artista por índice)
 *
 * Isso segue o mesmo princípio de "normalização" usado em bancos de dados.
 */
@Serializable
data class CompactData(
  val artists: List<String>,
  val songs: List<NewSong>
)

// ============================================================
// ============================ MAIN ===========================
// ============================================================

fun main() {

  /**
   * Parser JSON configurado para ser tolerante.
   * ignoreUnknownKeys evita quebra caso o JSON original evolua.
   */
  val json = Json { ignoreUnknownKeys = true }

  // ========================================================
  // 1. LEITURA DO JSON ORIGINAL
  // ========================================================

  val input = File("songs.json").readText()

  val oldData = json.decodeFromString<List<OldArtistGroup>>(input)

  /**
   * Estruturas auxiliares para normalização:
   *
   * artistList:
   *  - lista única de artistas (sem duplicação)
   *
   * artistIndexMap:
   *  - mapa para lookup rápido (artist -> index)
   *
   * Isso evita:
   *  - busca linear
   *  - duplicação de strings
   */
  val artistList = mutableListOf<String>()
  val artistIndexMap = HashMap<String, Int>()

  /**
   * Lista final de músicas já compactadas.
   */
  val newSongs = mutableListOf<NewSong>()

  // ========================================================
  // 2. PROCESSAMENTO E COMPACTAÇÃO
  // ========================================================

  for (group in oldData) {

    val artist = group.artist

    /**
     * Normalização de artista:
     *
     * - Se o artista já existe → reutiliza índice
     * - Senão → adiciona na lista e cria novo índice
     *
     * Isso transforma:
     *   "Avenged Sevenfold"
     * em:
     *   0
     */
    val index = artistIndexMap.getOrPut(artist) {
      val newIndex = artistList.size
      artistList.add(artist)
      newIndex
    }

    /**
     * Para cada música do artista:
     * - converte dificuldades → bitmask
     * - cria objeto compacto
     */
    for (song in group.songs) {

      val bitmask = encodeDifficulties(song.difficulties)

      newSongs.add(
        NewSong(
          id = song.id,
          t = song.title,
          a = index,
          d = bitmask,
          f = song.filtering
        )
      )
    }
  }

  // ========================================================
  // 3. CONSTRUÇÃO DO RESULTADO FINAL
  // ========================================================

  val compact = CompactData(
    artists = artistList,
    songs = newSongs
  )

  /**
   * JSON sem pretty print:
   * - remove espaços e quebras de linha
   * - reduz tamanho final significativamente
   */
  val outputJson = Json { prettyPrint = false }

  File("songs_compact.json").writeText(
    outputJson.encodeToString(compact)
  )

  println("Compactação concluída!")
}

// ============================================================
// ===================== BITMASK ENCODE ========================
// ============================================================

/**
 * Converte lista de dificuldades em um inteiro usando bitmask.
 *
 * Motivação:
 * - Lista de strings é cara (memória + tamanho JSON)
 * - Int é extremamente compacto
 *
 * Mapeamento:
 *   easy   = 1  (0001)
 *   medium = 2  (0010)
 *   hard   = 4  (0100)
 *   expert = 8  (1000)
 *
 * Combinação é feita com OR bit a bit:
 *
 * Exemplo:
 *   ["hard", "expert"]
 *
 *   4 (0100)
 * | 8 (1000)
 * = 12 (1100)
 *
 * Caso especial:
 *   ["all"] → 15 (1111)
 */
fun encodeDifficulties(list: List<String>): Int {

  var result = 0

  for (d in list) {
    when (d) {
      "easy" -> result = result or 1
      "medium" -> result = result or 2
      "hard" -> result = result or 4
      "expert" -> result = result or 8

      // atalho: todas as dificuldades
      "all" -> return 15
    }
  }

  return result
}