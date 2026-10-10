import { useEffect, useState } from 'react';
import { Activity, AlertCircle, CheckCircle2, Database, Server, Shield } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { getHealth, getPlatformSummary } from '../services/api.js';

const roadmap = [
  { phase: 'Phase 1', scope: 'Project foundation, schema, first API', status: 'Ready' },
  { phase: 'Phase 2', scope: 'JWT auth, invitations, registration', status: 'Next' },
  { phase: 'Phase 3', scope: 'Profiles, companies, drives, eligibility', status: 'Planned' },
  { phase: 'Phase 4', scope: 'Applications, bookmarks, notifications', status: 'Planned' },
  { phase: 'Phase 5', scope: 'CSV import and announcement parser', status: 'Planned' },
  { phase: 'Phase 6', scope: 'Analytics, exports, deployment polish', status: 'Planned' }
];

export default function HomePage() {
  const [health, setHealth] = useState(null);
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState('');
  const [summaryError, setSummaryError] = useState('');
  const [loading, setLoading] = useState(true);
  const [summaryLoading, setSummaryLoading] = useState(true);

  const metrics = [
    { name: 'Invited', value: summary?.invitedStudents ?? 0 },
    { name: 'Registered', value: summary?.registeredStudents ?? 0 },
    { name: 'Pending', value: summary?.pendingInvitations ?? 0 },
    { name: 'Failed', value: summary?.failedInvitations ?? 0 },
    { name: 'Companies', value: summary?.totalCompanies ?? 0 },
    { name: 'Drives', value: summary?.publishedDrives ?? 0 },
    { name: 'Applications', value: summary?.totalApplications ?? 0 }
  ];

  useEffect(() => {
    let active = true;

    function refreshHealth() {
      getHealth()
        .then((data) => {
          if (!active) return;
          setHealth(data);
          setError('');
        })
        .catch((err) => {
          if (active) setError(err.message || 'Backend health check failed');
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    }

    refreshHealth();
    const timer = window.setInterval(refreshHealth, 30000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  useEffect(() => {
    let active = true;

    function refreshSummary() {
      getPlatformSummary()
        .then((data) => {
          if (!active) return;
          setSummary(data);
          setSummaryError('');
        })
        .catch((err) => {
          if (active) setSummaryError(err.message || 'Summary API failed');
        })
        .finally(() => {
          if (active) setSummaryLoading(false);
        });
    }

    refreshSummary();
    const timer = window.setInterval(refreshSummary, 15000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  return (
    <div className="page">
      <header className="hero">
        <div>
          <p className="eyebrow">Smart Placement Drive Management System</p>
          <h1>PlaceTrack</h1>
          <p>
            A production-minded placement portal foundation with Spring Boot, React, MySQL-ready migrations,
            secure architecture, and a staged roadmap for coordinator and student workflows.
          </p>
        </div>
        <div className={`health-card ${error ? 'bad' : 'good'}`}>
          {loading && <Activity size={24} />}
          {!loading && error && <AlertCircle size={24} />}
          {!loading && !error && <CheckCircle2 size={24} />}
          <span>{loading ? 'Checking API' : error ? 'API unavailable' : health.status}</span>
          <small>{error || health?.phase}</small>
        </div>
      </header>

      <section className="status-grid">
        <article>
          <Server />
          <strong>Backend</strong>
          <span>{health?.modules?.backend || 'Spring Boot API foundation'}</span>
        </article>
        <article>
          <Database />
          <strong>Database</strong>
          <span>{health?.modules?.database || 'Flyway migration schema'}</span>
        </article>
        <article>
          <Shield />
          <strong>Security</strong>
          <span>{health?.modules?.security || 'Role-protected API design'}</span>
        </article>
      </section>

      <section className="content-grid">
        <div className="panel">
          <div className="panel-head">
            <h2>Phase Roadmap</h2>
            <span>Incremental build</span>
          </div>
          <div className="roadmap">
            {roadmap.map((item) => (
              <div key={item.phase} className="roadmap-row">
                <strong>{item.phase}</strong>
                <span>{item.scope}</span>
                <em>{item.status}</em>
              </div>
            ))}
          </div>
        </div>

        <div className="panel">
          <div className="panel-head">
            <h2>Analytics Shell</h2>
            <span>{summaryLoading ? 'Loading live counts' : summaryError ? 'Summary unavailable' : 'Database-backed counts'}</span>
          </div>
          {summaryError && <p className="inline-error">{summaryError}</p>}
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={metrics}>
              <CartesianGrid strokeDasharray="3 3" stroke="#d7dee8" />
              <XAxis dataKey="name" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="value" fill="#2563eb" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </section>
    </div>
  );
}
