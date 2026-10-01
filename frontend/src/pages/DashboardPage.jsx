import { useAuth } from '../auth/AuthContext.jsx'

function DashboardPage() {
  const { user, logout } = useAuth()

  return (
    <section className="dashboard-card">
      <h1>QamqorMed</h1>
      <p className="dashboard-welcome">Добро пожаловать, {user.firstName}</p>
      <button className="button button--secondary" type="button" onClick={() => logout()}>
        Выйти
      </button>
    </section>
  )
}

export default DashboardPage
