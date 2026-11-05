import { useEffect, useRef, useState } from 'react'

/* ============================================================
   НАСТРОЙКИ API: если ваши эндпоинты называются иначе, меняйте здесь
   Запрос:  POST /api/ask   { "question": "..." }
   Ответ:   { "answer": "...", "sql": "...", "rows": [ {...}, ... ] }
   (поля sql и rows необязательны)
   ============================================================ */
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '')
const API_URL = `${API_BASE_URL}/api/ai/ask`
const TIMEOUT_MS = 90_000
const MAX_TABLE_ROWS = 50

async function askServer(question, signal, lang) {
  const res = await fetch(API_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question }),
    signal,
  })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  const data = await res.json()
  return {
    text: data.answer ?? data.text ?? data.message ?? data.response ?? '',
    sql: data.sql ?? data.query ?? null,
    rows: Array.isArray(data.rows) ? data.rows : Array.isArray(data.data) ? data.data : [],
  }
}

/* ============================================================
   ТЕКСТЫ интерфейса RU / UZ
   ============================================================ */
const T = {
  ru: {
    product: 'Ассистент по сделкам',
    title: 'Спросите о сделках биржи',
    lead: 'Отвечаю только по данным реестра сделок: числа, названия, коды. Если данных нет, так и скажу.',
    placeholder: 'Задайте вопрос о сделках…',
    send: 'Отправить',
    stop: 'Остановить',
    newChat: 'Новый чат',
    source: 'Основание ответа',
    sql: 'SQL-запрос',
    copy: 'Копировать',
    copied: 'Скопировано',
    thinking: 'Ищу в данных',
    retry: 'Повторить',
    empty: 'Сервер вернул пустой ответ.',
    more: (n, total) => `Показаны первые ${n} из ${total} строк`,
    errors: {
      network: 'Не удалось получить ответ от сервера. Проверьте соединение и повторите запрос.',
      timeout: 'Ответ занял больше 90 секунд. Попробуйте задать вопрос короче.',
      stopped: 'Запрос остановлен.',
      auth: 'Не задан токен доступа к API или он неверен. Повторите запрос и введите правильный токен.',
    },
    note: 'Все данные в реестре вымышленные. Проверяйте важные цифры по основанию ответа.',
    examples: [
      'Сколько сделок заключено в 2025 году?',
      'Топ-5 товаров по сумме сделок за 2024 год',
      'Какой брокер провёл больше всего сделок?',
      'Сколько сделок аннулировано из-за неоплаты?',
    ],
  },
  uz: {
    product: 'Bitimlar yordamchisi',
    title: 'Birja bitimlari haqida so‘rang',
    lead: 'Faqat bitimlar reyestri maʼlumotlari asosida javob beraman: raqamlar, nomlar, kodlar. Maʼlumot bo‘lmasa, buni ochiq aytaman.',
    placeholder: 'Bitimlar haqida savol bering…',
    send: 'Yuborish',
    stop: 'To‘xtatish',
    newChat: 'Yangi chat',
    source: 'Javob asosi',
    sql: 'SQL so‘rov',
    copy: 'Nusxalash',
    copied: 'Nusxalandi',
    thinking: 'Maʼlumotlardan qidiryapman',
    retry: 'Qayta urinish',
    empty: 'Server bo‘sh javob qaytardi.',
    more: (n, total) => `${total} qatordan dastlabki ${n} tasi ko‘rsatilgan`,
    errors: {
      network: 'Serverdan javob olib bo‘lmadi. Ulanishni tekshirib, so‘rovni takrorlang.',
      timeout: 'Javob 90 soniyadan oshdi. Savolni qisqaroq qilib bering.',
      stopped: 'So‘rov to‘xtatildi.',
      auth: 'API kirish tokeni kiritilmagan yoki noto‘g‘ri. So‘rovni takrorlab, to‘g‘ri tokenni kiriting.',
    },
    note: 'Reyestrdagi barcha maʼlumotlar o‘ylab topilgan. Muhim raqamlarni javob asosidan tekshiring.',
    examples: [
      '2025-yilda nechta bitim tuzilgan?',
      '2024-yilda summa bo‘yicha top-5 tovar',
      'Qaysi broker eng ko‘p bitim o‘tkazgan?',
      'To‘lanmagani uchun nechta bitim bekor qilingan?',
    ],
  },
}

/* ============================================================
   Мелкие компоненты
   ============================================================ */
function Logo({ height = 28 }) {
  const [failed, setFailed] = useState(false)
  if (failed) return <span className="logo-fallback" style={{ fontSize: height * 0.8 }}>UZEX</span>
  return (
    <img
      className="logo"
      src="/uzex-logo.svg"
      alt="UZEX"
      height={height}
      onError={() => setFailed(true)}
    />
  )
}

// **жирный** и маркированные списки, без внешних библиотек
function inline(s) {
  return s.split(/(\*\*[^*]+\*\*)/g).map((part, i) =>
    part.length > 4 && part.startsWith('**') && part.endsWith('**')
      ? <strong key={i}>{part.slice(2, -2)}</strong>
      : part,
  )
}

function RichText({ text }) {
  const out = []
  let list = []
  const flush = () => {
    if (list.length) {
      out.push(<ul key={'u' + out.length}>{list.map((l, i) => <li key={i}>{inline(l)}</li>)}</ul>)
      list = []
    }
  }
  String(text).split('\n').forEach((line) => {
    const m = line.match(/^\s*[-*•]\s+(.*)$/)
    if (m) { list.push(m[1]); return }
    flush()
    if (line.trim()) out.push(<p key={'p' + out.length}>{inline(line)}</p>)
  })
  flush()
  return <>{out}</>
}

function fmt(v) {
  if (v === null || v === undefined || v === '') return '—'
  if (typeof v === 'number') return v.toLocaleString('ru-RU', { maximumFractionDigits: 2 })
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

function Sources({ sql, rows, t }) {
  const [copied, setCopied] = useState(false)
  const hasTable = rows.length > 0 && typeof rows[0] === 'object' && rows[0] !== null && !Array.isArray(rows[0])
  if (!sql && !hasTable) return null

  const cols = hasTable ? Object.keys(rows[0]) : []
  const shown = rows.slice(0, MAX_TABLE_ROWS)

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(sql)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch { /* буфер обмена недоступен */ }
  }

  return (
    <details className="src">
      <summary>{t.source}</summary>
      <div className="src-body">
        {sql && (
          <div className="sqlbox">
            <button type="button" className="copy" onClick={copy}>{copied ? t.copied : t.copy}</button>
            <pre className="sql" aria-label={t.sql}>{sql}</pre>
          </div>
        )}
        {hasTable && (
          <div className="tablewrap">
            <table>
              <thead>
                <tr>{cols.map((c) => <th key={c}>{c}</th>)}</tr>
              </thead>
              <tbody>
                {shown.map((r, i) => (
                  <tr key={i}>
                    {cols.map((c) => (
                      <td key={c} className={typeof r[c] === 'number' ? 'num' : undefined}>{fmt(r[c])}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
            {rows.length > shown.length && <div className="more">{t.more(shown.length, rows.length)}</div>}
          </div>
        )}
      </div>
    </details>
  )
}

function Message({ m, t, onRetry }) {
  if (m.role === 'user') {
    return (
      <div className="msg user">
        <div className="bubble">{m.text}</div>
      </div>
    )
  }
  return (
    <div className="msg">
      <div className="avatar" aria-hidden="true">U</div>
      <div className="answer">
        {m.error ? (
          <div className={m.error === 'stopped' ? 'note-inline' : 'errbox'}>
            <span>{t.errors[m.error]}</span>
            {m.retry && <button type="button" className="retry" onClick={() => onRetry(m.retry)}>{t.retry}</button>}
          </div>
        ) : (
          <>
            {m.text ? <RichText text={m.text} /> : <p className="muted">{t.empty}</p>}
            <Sources sql={m.sql} rows={m.rows || []} t={t} />
          </>
        )}
      </div>
    </div>
  )
}

function Typing({ t }) {
  return (
    <div className="msg" role="status" aria-live="polite">
      <div className="avatar" aria-hidden="true">U</div>
      <div className="answer">
        <span className="typing">
          <span className="dots" aria-hidden="true"><i /><i /><i /></span>
          {t.thinking}
        </span>
      </div>
    </div>
  )
}

/* ============================================================
   Приложение
   ============================================================ */
export default function App() {
  const [lang, setLang] = useState(() => {
    try { return localStorage.getItem('uzex-lang') || 'ru' } catch { return 'ru' }
  })
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)

  const t = T[lang]
  const idRef = useRef(0)
  const abortRef = useRef(null)
  const endRef = useRef(null)
  const taRef = useRef(null)

  useEffect(() => {
    document.documentElement.lang = lang === 'uz' ? 'uz' : 'ru'
    try { localStorage.setItem('uzex-lang', lang) } catch { /* ignore */ }
  }, [lang])

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages, loading])

  useEffect(() => {
    const ta = taRef.current
    if (!ta) return
    ta.style.height = 'auto'
    ta.style.height = Math.min(ta.scrollHeight, 160) + 'px'
  }, [input])

  const add = (msg) => setMessages((m) => [...m, { id: ++idRef.current, ...msg }])

  async function send(text) {
    const q = text.trim()
    if (!q || loading) return

    const ctrl = new AbortController()
    abortRef.current = ctrl
    let timedOut = false
    const timer = setTimeout(() => { timedOut = true; ctrl.abort() }, TIMEOUT_MS)

    add({ role: 'user', text: q })
    setInput('')
    setLoading(true)

    try {
      const r = await askServer(q, ctrl.signal, lang)
      add({ role: 'assistant', ...r })
    } catch (e) {
      const kind = e.name === 'AbortError'
        ? (timedOut ? 'timeout' : 'stopped')
        : 'network'
      add({ role: 'assistant', error: kind, retry: q })
    } finally {
      clearTimeout(timer)
      abortRef.current = null
      setLoading(false)
      taRef.current?.focus()
    }
  }

  const stop = () => abortRef.current?.abort()

  const newChat = () => {
    abortRef.current?.abort()
    setMessages([])
    setInput('')
    taRef.current?.focus()
  }

  const onKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault()
      send(input)
    }
  }

  return (
    <div className="app">
      <div className="stripe" />
      <header className="topbar">
        <div className="brand">
          <Logo height={30} />
          <span className="brand-name">{t.product}</span>
        </div>
        <div className="tools">
          <div className="seg" role="group" aria-label="Language / Til">
            <button type="button" aria-pressed={lang === 'ru'} onClick={() => setLang('ru')}>RU</button>
            <button type="button" aria-pressed={lang === 'uz'} onClick={() => setLang('uz')}>UZ</button>
          </div>
          <button type="button" className="ghost newchat" onClick={newChat} disabled={!messages.length && !loading}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
              <path d="M12 5v14M5 12h14" />
            </svg>
            <span>{t.newChat}</span>
          </button>
        </div>
      </header>

      <main className="scroll">
        <div className="column">
          {messages.length === 0 && (
            <section className="hero">
              <Logo height={52} />
              <h1>{t.title}</h1>
              <p className="lead">{t.lead}</p>
              <div className="chips">
                {t.examples.map((ex) => (
                  <button key={ex} type="button" className="chip" onClick={() => send(ex)}>{ex}</button>
                ))}
              </div>
            </section>
          )}

          {messages.map((m) => <Message key={m.id} m={m} t={t} onRetry={send} />)}
          {loading && <Typing t={t} />}
          <div ref={endRef} />
        </div>
      </main>

      <footer className="dock">
        <form className="composer" onSubmit={(e) => { e.preventDefault(); send(input) }}>
          <textarea
            ref={taRef}
            rows={1}
            value={input}
            placeholder={t.placeholder}
            aria-label={t.placeholder}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKeyDown}
            autoFocus
          />
          {loading ? (
            <button type="button" className="send" onClick={stop} aria-label={t.stop} title={t.stop}>
              <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden="true"><rect width="14" height="14" rx="3" fill="currentColor" /></svg>
            </button>
          ) : (
            <button type="submit" className="send" disabled={!input.trim()} aria-label={t.send} title={t.send}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M12 19V5M5 12l7-7 7 7" />
              </svg>
            </button>
          )}
        </form>
        <p className="note">{t.note}</p>
      </footer>
    </div>
  )
}
