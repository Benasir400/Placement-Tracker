import { BriefcaseBusiness, GraduationCap, LogOut, UserRound } from 'lucide-react';
import { NavLink, Outlet } from 'react-router-dom';

const navItems = [
  { label: 'Coordinator', to: '/admin', icon: BriefcaseBusiness, roles: ['ADMIN', 'PLACEMENT_COORDINATOR'] },
  { label: 'Student', to: '/student', icon: GraduationCap, roles: ['STUDENT'] }
];

export default function Shell({ user, onLogout }) {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span>PT</span>
          <div>
            <strong>PlaceTrack</strong>
            <small>Placement drive management</small>
          </div>
        </div>
        <nav aria-label="Main navigation">
          {navItems.filter((item) => user && item.roles.includes(user.role)).map((item) => {
            const Icon = item.icon;
            return (
              <NavLink key={item.label} to={item.to}>
                <Icon size={18} />
                {item.label}
              </NavLink>
            );
          })}
        </nav>
        {user && (
          <div className="side-footer">
            <div className="user-chip">
              <UserRound size={18} />
              <span>{user.fullName}</span>
              <small>{user.role.replace('_', ' ').toLowerCase()}</small>
            </div>
            <button type="button" className="ghost-button" onClick={onLogout}>
              <LogOut size={17} />
              Sign out
            </button>
          </div>
        )}
      </aside>

      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}
