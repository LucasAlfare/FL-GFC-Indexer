# 🎸 FL Guitar Flash Custom Indexer

Script em Kotlin para **indexar todas as músicas do Guitar Flash Custom**, gerando um JSON estruturado, organizado e otimizado para busca.
O site original é antigo e não possui sistema de busca eficiente — esse projeto resolve isso criando um índice local completo.

Você pode acessar a página inicial da listagem de músicas do GF custom [neste link](https://guitarflash.com/custom/lista.asp?pag=0). Já as páginas das músicas seguem esse formato:
`http://guitarflash.me/?gfcMus=XXXXXXXXXXXX`, onde esses "XXX..." são o ID respectivo da música.

Já estou deixando incluso, uma versão prévia de resultado de processamento do índice. É o arquivo [songs.json](songs.json) (~1.7mb) e uma versão deste mesmo JSON com um pouco de compactação (loss-less) [songs_compact.json](songs_compact.json) (~800kb). Mais abaixo na leitura eu falo um pouco mais sobre como estruturei essa compactação.

Ainda é possível ter muito lixo e sujeira de split/parsing, mas por enquanto é o melhor índice que temos para o GF Custom.

---

## 📌 Funcionalidades

* 🔍 Varre automaticamente todas as páginas (88 no total por enquanto?)
* 🎵 Extrai:

  * ID da música
  * Título
  * Artista
  * Dificuldades
* 🧹 Normaliza e limpa os dados
* 🚫 Remove duplicados durante a coleta (mais eficiente)
* 🧠 Gera campo otimizado para busca (`filtering`)
* 📦 Agrupa músicas por artista
* 📄 Exporta tudo em JSON estruturado

---

## 🧱 Estrutura do JSON

```json
[
  {
    "artist": "Avenged Sevenfold",
    "songs": [
      {
        "id": "C2FB9M3D8FRB",
        "title": "A Little Piece of Heaven",
        "difficulties": ["all"],
        "filtering": "avengedsevenfoldalittlepieceofheaven"
      }
    ]
  }
]
```

---

## ⚙️ Requisitos

* Kotlin
* Gradle

### Dependências

```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
```

E no `plugins`:

```kotlin
kotlin("plugin.serialization")
```

---

## ▶️ Como usar

1. Clone ou copie o script
2. Execute o `main()`
3. Aguarde o processamento (88 páginas)
4. Um arquivo será gerado:

```
songs.json
```

---

## 🧠 Regras de processamento

### 🔹 1. Limpeza do nome

Antes de qualquer coisa:

* Remove tudo após `"chart by"` (case insensitive)

---

### 🔹 2. Separação título / artista

Ordem de prioridade:

1. `" por "`
2. `" by "`
3. `" - "`
4. fallback → artista = `"unknown"`

---

### 🔹 3. Normalização de artista

Se o artista for:

* vazio
* `" - "`, `"-"`, `"--"`
* `"unknown"`, `"no artist"`, `"none"`

👉 vira:

```
"unknown"
```

---

### 🔹 4. Dificuldades

| Original | Resultado |
| -------- | --------- |
| fácil    | easy      |
| médio    | medium    |
| difícil  | hard      |
| expert   | expert    |

Regras:

* remove duplicados
* ordena
* se tiver todas:

```json
["all"]
```

---

### 🔹 5. Deduplicação

* Feita **durante a coleta**
* Usa `Set` de IDs
* Ignora imediatamente duplicatas

---

### 🔹 6. Campo `filtering`

O campo `filtering` é o coração da busca no projeto. Ele funciona como uma **string pré-processada otimizada**, permitindo buscas extremamente rápidas e simples sem precisar de lógica complexa.

#### 📌 Como é construído

* Se tem artista:

  ```
  filtering = artist + title
  ```
* Senão:

  ```
  filtering = title
  ```

Depois:

* remove todos os espaços
* converte para lowercase

---

#### 📌 Exemplo

```
Title:  A Little Piece of Heaven
Artist: Avenged Sevenfold
```

Resultado:

```
avengedsevenfoldalittlepieceofheaven
```

---

#### 🧠 Como isso melhora a busca

Essa estratégia transforma cada música em uma **string contínua e normalizada**, permitindo buscas com apenas uma operação simples:

```kotlin
song.filtering.contains(query)
```

---

#### ✅ Vantagens

**1. Busca por substring**

* Funciona com qualquer parte do nome:

  * `avenged`
  * `heaven`
  * `sevenfold`

---

**2. Independente de ordem**

* Como artista + título estão juntos, não importa o que o usuário digite primeiro

---

**3. Tolerante a espaços**

* Usuário digita:

  ```
  avenged heaven
  ```
* Normaliza para:

  ```
  avengedheaven
  ```
* Ainda encontra:

  ```
  avengedsevenfoldalittlepieceofheaven
  ```

---

**4. Case insensitive automático**

* Tudo já está em lowercase → sem custo extra na busca

---

**5. Performance excelente**

* Toda a “inteligência” é feita na indexação
* A busca vira apenas:

  ```
  .contains()
  ```

---

#### 💡 Resumo

O `filtering` funciona como um **índice de busca simplificado**, onde:

* os dados já estão preparados previamente
* não há necessidade de parsing em tempo de busca
* a performance é alta mesmo com milhares de músicas

---

### 🔹 7. Agrupamento final

* Agrupado por `artist`
* Músicas ordenadas por `title`
* Artistas ordenados alfabeticamente

---

# 🗜️ Compactação do JSON (otimização avançada)

Após gerar o JSON original, foi criado um segundo processo chamado **Compactor/Optimizer**, responsável por reduzir drasticamente o tamanho do arquivo.

📉 Resultado real obtido:

```
~1700 KB → ~870 KB
```

Ou seja, praticamente **50% de redução** sem perda de informação.

---

## 🧠 Problema do JSON original

O JSON tradicional sofre com:

* 🔁 Repetição de strings (principalmente nomes de artistas)
* 🏷️ Chaves longas repetidas milhares de vezes
* 📦 Estrutura aninhada com overhead desnecessário
* 🧾 Dados textuais redundantes

Exemplo do problema:

```json
{
  "artist": "Avenged Sevenfold",
  "songs": [...]
}
```

O nome do artista aparece repetidamente no arquivo inteiro.

---

## 🧩 Estratégias de compactação aplicadas

### 🔹 1. Normalização de artistas (deduplicação estrutural)

Ao invés de repetir o nome do artista, ele é armazenado uma única vez:

```json
{
  "artists": [
    "Avenged Sevenfold",
    "Metallica"
  ]
}
```

E cada música referencia o artista por índice:

```json
{
  "a": 0
}
```

👉 Isso transforma uma string repetida em um número pequeno.

---

### 🔹 2. Estrutura flat (remoção de nesting)

Antes:

```json
[
  { "artist": "...", "songs": [...] }
]
```

Depois:

```json
{
  "artists": [...],
  "songs": [...]
}
```

👉 Menos estrutura = menos bytes.

---

### 🔹 3. Encurtamento de chaves

Antes:

```json
"title", "difficulties", "filtering"
```

Depois:

```json
["t", "d", "f", "a"]
```

👉 Redução massiva de repetição textual.

---

### 🔹 4. Bitmask para dificuldades

As dificuldades deixam de ser lista de strings e viram um número.

#### 📌 Mapeamento:

| dificuldade | valor |
| ----------- | ----- |
| easy        | 1     |
| medium      | 2     |
| hard        | 4     |
| expert      | 8     |

#### 📌 Exemplo:

Hard + Expert:

```
4 + 8 = 12
```

```json
{
  "d": 12
}
```

#### 📌 Caso especial:

```
["all"] → 15
```

---

### 🔹 5. Remoção de whitespace

JSON final não usa `prettyPrint`.

👉 Remove espaços, tabs e quebras de linha desnecessárias.

---

## 🧠 Como ler os dados compactados

### 🎵 Recuperar artista

```kotlin
val artistName = artists[song.a]
```

---

### 🎚️ Decodificar dificuldades

```kotlin
fun decode(bitmask: Int): List<String> {
    val result = mutableListOf<String>()

    if (bitmask and 1 != 0) result.add("easy")
    if (bitmask and 2 != 0) result.add("medium")
    if (bitmask and 4 != 0) result.add("hard")
    if (bitmask and 8 != 0) result.add("expert")

    return result
}
```

---

## 💡 Insight principal

Essa compactação segue um princípio clássico:

> **Remover redundância e substituir texto por referências**

Você basicamente transforma:

```
JSON legível → JSON eficiente
```

Sem perder a capacidade de reconstrução dos dados.

---

## ⚡ Performance

O script já foi pensado pra ser leve:

* ❌ sem regex dentro de loop
* ✅ `.lowercase()` controlado
* ✅ deduplicação imediata
* ✅ uso de `Sequence` em parsing

E após compactação:

* 📉 menor uso de memória
* ⚡ leitura mais rápida
* 🚀 melhor performance geral

---

## 🚀 Possíveis melhorias

### 🔄 Paralelismo

* Usar coroutines pra processar páginas em paralelo

### 💾 Cache local??

* Salvar HTML das páginas pra evitar múltiplos requests

### 🔍 Sistema de busca

* Criar:

  * CLI
  * App Android
  * Web UI

### 📊 Outras ideias

* Separar por dificuldade
* Tags automáticas por gênero (heurística)
* Index reverso

---

## ⚠️ Observações

* O script depende da estrutura HTML atual do site
* Se o site mudar, o parser pode quebrar
* Evite fazer muitas execuções seguidas (respeitar o servidor)

---

## 📄 Licença

Uso livre para fins pessoais, afinal não tenho direito autoral sobre nada do Guitar Flash, isso é apenas para uso pessoal, quando for necessário.
