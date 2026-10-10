import { Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import Shell from './components/Shell.jsx';
import AuthPage from './pages/AuthPage.jsx';
import CoordinatorPage from './pages/CoordinatorPage.jsx';
import StudentPortalPage from './pages/StudentPortalPage.jsx';
import { getMe } from './services/api.js';

export default function App() {
  const [user, setUser] = useState(null);
  const [booting, setBooting] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('placetrack_token');
    if (!token) {
      setBooting(false);
      return;
    }
    getMe()
      .then(setUser)
      .catch(() => localStorage.removeItem('placetrack_token'))
      .finally(() => setBooting(false));
  }, []);

  function handleAuth(auth) {
    localStorage.setItem('placetrack_token', auth.token);
    setUser(auth.user);
    navigate(auth.user.role === 'STUDENT' ? '/student' : '/admin');
  }

  function logout() {
    localStorage.removeItem('placetrack_token');
    setUser(null);
    navigate('/login');
  }

  if (booting) return <div className="boot-screen">Loading PlaceTrack</div>;

  return (
    <Routes>
      <Route path="/login" element={<AuthPage mode="login" onAuth={handleAuth} />} />
      <Route path="/register" element={<AuthPage mode="invite" onAuth={handleAuth} />} />
      <Route path="/accept-invitation" element={<AuthPage mode="invite" onAuth={handleAuth} />} />
      <Route element={<Shell user={user} onLogout={logout} />}>
        <Route path="/admin" element={user?.role === 'PLACEMENT_COORDINATOR' || user?.role === 'ADMIN' ? <CoordinatorPage /> : <Navigate to="/login" replace />} />
        <Route path="/student" element={user?.role === 'STUDENT' ? <StudentPortalPage /> : <Navigate to="/login" replace />} />
        <Route path="/" element={<Navigate to={user ? (user.role === 'STUDENT' ? '/student' : '/admin') : '/login'} replace />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
