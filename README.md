Aqui está seu README com a seção de `filtering` **bem mais explicada e integrada**, sem quebrar teu estilo:

---

# 🎸 Guitar Flash Custom Indexer

Script em Kotlin para **indexar todas as músicas do Guitar Flash Custom**, gerando um JSON estruturado, organizado e otimizado para busca.
O site original é antigo e não possui sistema de busca eficiente — esse projeto resolve isso criando um índice local completo.

Você pode acessar a página inicial da listagem de músicas do GF custom [neste link](https://guitarflash.com/custom/lista.asp?pag=0). Já as páginas das músicas seguem esse formato:
`http://guitarflash.me/?gfcMus=XXXXXXXXXXXX`, onde esses "XXX..." são o ID respectivo da música.

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

## ⚡ Performance

O script já foi pensado pra ser leve:

* ❌ sem regex dentro de loop
* ✅ `.lowercase()` controlado
* ✅ deduplicação imediata
* ✅ uso de `Sequence` em parsing

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
