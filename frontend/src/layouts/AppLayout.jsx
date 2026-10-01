function AppLayout({ children }) {
  return (
    <div className="app-shell">
      <header className="app-header">
        <a className="brand-link" href="/" aria-label="QamqorMed, на главную">
          <span className="brand-mark" aria-hidden="true">Q</span>
          <span className="brand-name">QamqorMed</span>
        </a>
      </header>
      <main className="app-main">{children}</main>
    </div>
  )
}

export default AppLayout
