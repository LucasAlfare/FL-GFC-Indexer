# 🎸 Guitar Flash Custom Indexer
Script em Kotlin para **indexar todas as músicas do Guitar Flash Custom**, gerando um JSON estruturado, organizado e otimizado para busca.
O site original é antigo e não possui sistema de busca eficiente — esse projeto resolve isso criando um índice local completo.
Você pode acessar a página inicial da lsitagem de músicas do GF custom [neste link](https://guitarflash.com/custom/lista.asp?pag=0). Já as páginas das músicas seguem esse formato: `http://guitarflash.me/?gfcMus=XXXXXXXXXXXX`, onde esses "XXX..." são o ID respectivo da música.

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
Mapeamento:

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
Criado para busca rápida:
* Se tem artista:
  ```
  artist + title
  ```
* Senão:
  ```
  title
  ```
Depois:
* remove espaços
* lowercase

Exemplo:
```
avengedsevenfoldalittlepieceofheaven
```

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
Se quiser evoluir o projeto:
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