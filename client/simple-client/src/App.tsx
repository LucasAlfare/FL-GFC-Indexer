import { useEffect, useMemo, useRef, useState } from "react"
import { useVirtualizer } from "@tanstack/react-virtual"
import { motion } from "framer-motion"

// ===== GLOBAL SETUP =====
// Remove default body margin, apply background and hide scrollbars
if (typeof document !== "undefined") {
  document.body.style.margin = "0"
  document.body.style.background = "#050508"

  const style = document.createElement("style")
  style.innerHTML = `
    ::-webkit-scrollbar { display: none; }
  `
  document.head.appendChild(style)
}

// ===== TYPES =====
// (would be: types/song.ts)

type Song = {
  id: string
  title: string
  artist: string
  difficulties: number
  filtering: string
}

// (would be: types/compact-data.ts)

type CompactData = {
  artists: string[]
  songs: {
    id: string
    t: string
    a: number
    d: number
    f: string
  }[]
}

// ===== DATA LOADING =====
// (would be: services/loadSongs.ts)

async function loadSongs(): Promise<Song[]> {
  const res = await fetch("/songs_compact.json")
  const data: CompactData = await res.json()

  return data.songs.map(s => ({
    id: s.id,
    title: s.t,
    artist: data.artists[s.a],
    difficulties: s.d,
    filtering: s.f
  }))
}

// ===== SEARCH =====
// (would be: utils/search.ts)

function normalizeQuery(q: string) {
  return q.replace(/\s/g, "").toLowerCase()
}

// ===== DIFFICULTY =====
// (would be: constants/difficulty.ts)

const DIFF = {
  easy: 1,
  medium: 2,
  hard: 4,
  expert: 8
}

const DIFF_EMOJI: Record<string, string> = {
  easy: "🟢",
  medium: "🟡",
  hard: "🔴",
  expert: "⚫"
}

function hasDifficulty(songMask: number, filterMask: number) {
  if (filterMask === 0) return true
  return (songMask & filterMask) !== 0
}

// ===== APP =====

export default function App() {
  const [songs, setSongs] = useState<Song[]>([])
  const [query, setQuery] = useState("")
  const [filterMask, setFilterMask] = useState(0)

  useEffect(() => {
    loadSongs().then(setSongs)
  }, [])

  const filtered = useMemo(() => {
    const q = normalizeQuery(query)

    return songs.filter(s => {
      if (q && !s.filtering.includes(q)) return false
      if (!hasDifficulty(s.difficulties, filterMask)) return false
      return true
    })
  }, [songs, query, filterMask])

  function toggleDifficulty(value: number) {
    setFilterMask(prev =>
      (prev & value) !== 0 ? prev & ~value : prev | value
    )
  }

  function openSong(id: string) {
    window.open(`http://guitarflash.me/?gfcMus=${id}`, "_blank")
  }

  // ===== VIRTUALIZER =====

  const parentRef = useRef<HTMLDivElement>(null)

  const rowVirtualizer = useVirtualizer({
    count: filtered.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 110,
    overscan: 10
  })

  return (
    <div style={styles.page}>
      <div style={styles.bg1} />
      <div style={styles.bg2} />

      <div style={styles.container}>
        <div style={styles.header}>
          <h1 style={styles.title}>🎸 Guitar Flash Index</h1>
          <p style={styles.subtitle}>
            Virtualização nível profissional
          </p>
        </div>

        <motion.input
          placeholder="🔍 Buscar música ou artista..."
          value={query}
          onChange={e => setQuery(e.target.value)}
          style={styles.search}
          whileFocus={{ scale: 1.02 }}
        />

        <div style={styles.filters}>
          {Object.entries(DIFF).map(([name, value]) => {
            const active = (filterMask & value) !== 0

            return (
              <motion.button
                key={name}
                onClick={() => toggleDifficulty(value)}
                style={{
                  ...styles.filterBtn,
                  background: active
                    ? "linear-gradient(135deg, #7c3aed, #4f46e5)"
                    : "rgba(255,255,255,0.05)"
                }}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.92 }}
              >
                {DIFF_EMOJI[name]} {name}
              </motion.button>
            )
          })}
        </div>

        {/* Virtualized list (scroll hidden) */}
        <div ref={parentRef} style={styles.listContainer}>
          {/* top fade */}
          <div style={styles.fadeTop} />
          {/* bottom fade */}
          <div style={styles.fadeBottom} />
          <div
            style={{
              height: rowVirtualizer.getTotalSize(),
              position: "relative"
            }}
          >
            {rowVirtualizer.getVirtualItems().map(virtualRow => {
              const song = filtered[virtualRow.index]

              return (
                <div
                  key={song.id}
                  style={{
                    position: "absolute",
                    top: 0,
                    left: 0,
                    width: "100%",
                    transform: `translateY(${virtualRow.start}px)`,
                    padding: "8px 20px",
                    boxSizing: "border-box"
                  }}
                >
                  <motion.div
                    style={styles.card}
                    onClick={() => openSong(song.id)}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.97 }}
                  >
                    <div style={styles.songTitle}>
                      🎵 {song.title}
                    </div>

                    <div style={styles.artist}>
                      👤 {song.artist}
                    </div>

                    <div style={styles.badges}>
                      {Object.entries(DIFF).map(([name, value]) => {
                        if ((song.difficulties & value) === 0) return null
                        return (
                          <span key={name} style={styles.badge}>
                            {DIFF_EMOJI[name]} {name}
                          </span>
                        )
                      })}
                    </div>
                  </motion.div>
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </div>
  )
}

// ===== STYLES =====
// (would be: styles/theme.ts)

const styles: any = {
  page: {
    minHeight: "100vh",
    background: "#050508",
    color: "white",
    fontFamily: "JetBrains Mono, monospace"
  },

  container: {
    width: "100%",
    margin: 0,
    padding: "20px"
  },

  listContainer: {
    height: "600px",
    overflow: "auto",
    scrollbarWidth: "none",
    msOverflowStyle: "none",

    /* center the results panel */
    maxWidth: "900px",
    margin: "0 auto",
    width: "100%",

    position: "relative",

    /* floating panel effect */
    borderRadius: "16px",
    boxShadow: "0 20px 60px rgba(0,0,0,0.6)"
  },

  bg1: {
    position: "fixed",
    width: 500,
    height: 500,
    background: "#7c3aed",
    filter: "blur(200px)",
    opacity: 0.25,
    top: -150,
    left: -150
  },

  bg2: {
    position: "fixed",
    width: 500,
    height: 500,
    background: "#4f46e5",
    filter: "blur(200px)",
    opacity: 0.25,
    bottom: -150,
    right: -150
  },

  header: {
    textAlign: "center",
    marginBottom: "30px"
  },

  title: {
    fontSize: "42px",
    fontWeight: 800,
    margin: 0,
    background: "linear-gradient(90deg,#a78bfa,#6366f1)",
    WebkitBackgroundClip: "text",
    WebkitTextFillColor: "transparent"
  },

  subtitle: {
    opacity: 0.6,
    marginTop: "8px"
  },

  search: {
    width: "100%",
    maxWidth: "900px",
    margin: "0 auto 20px auto",
    display: "block",
    padding: "16px",
    borderRadius: "12px",
    border: "1px solid rgba(255,255,255,0.1)",
    background: "rgba(255,255,255,0.05)",
    color: "white",
    outline: "none",
    boxSizing: "border-box",
    fontFamily: "inherit",
    fontSize: "16px"
  },

  filters: {
    display: "flex",
    gap: "10px",
    marginBottom: "20px",
    justifyContent: "center",
    flexWrap: "wrap"
  },

  filterBtn: {
    padding: "10px 16px",
    borderRadius: "999px",
    border: "none",
    color: "white",
    cursor: "pointer",
    fontFamily: "inherit"
  },

  card: {
    padding: "18px",
    borderRadius: "16px",
    background: "rgba(255,255,255,0.05)",
    backdropFilter: "blur(10px)",
    cursor: "pointer",
    boxShadow: "0 10px 30px rgba(124,58,237,0.25)"
  },

  songTitle: {
    fontSize: "20px",
    fontWeight: 800,
    marginBottom: "5px"
  },

  artist: {
    fontSize: "14px",
    opacity: 0.65,
    marginBottom: "8px"
  },

  badges: {
    display: "flex",
    gap: "6px",
    flexWrap: "wrap"
  },

  badge: {
    fontSize: "12px",
    padding: "5px 10px",
    borderRadius: "999px",
    background: "rgba(124,58,237,0.3)"
  },

  // fade overlays for top/bottom edges
  fadeTop: {
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    height: "40px",
    background: "linear-gradient(to bottom, #050508, transparent)",
    pointerEvents: "none",
    zIndex: 2
  },

  fadeBottom: {
    position: "absolute",
    bottom: 0,
    left: 0,
    right: 0,
    height: "40px",
    background: "linear-gradient(to top, #050508, transparent)",
    pointerEvents: "none",
    zIndex: 2
  }
}
