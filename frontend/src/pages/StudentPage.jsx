import { useEffect, useMemo, useState } from 'react';
import { Bookmark, BriefcaseBusiness, CheckCircle2, Clock, ExternalLink, FileCheck2, UserRound } from 'lucide-react';
import {
  applyToDrive,
  getApplications,
  getBookmarks,
  getEligibility,
  getNotifications,
  getStudentDrives,
  getStudentProfile,
  markExternalSubmitted,
  toggleBookmark,
  updateStudentProfile
} from '../services/api.js';

export default function StudentPage() {
  const [profile, setProfile] = useState(null);
  const [drives, setDrives] = useState([]);
  const [applications, setApplications] = useState([]);
  const [bookmarks, setBookmarks] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [eligibility, setEligibility] = useState({});
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  async function load(options = { includeProfile: true }) {
    const profileRequest = options.includeProfile ? getStudentProfile() : Promise.resolve(null);
    const [profileData, driveData, applicationData, bookmarkData, notificationData] = await Promise.all([
      profileRequest,
      getStudentDrives(),
      getApplications(),
      getBookmarks(),
      getNotifications()
    ]);
    if (profileData) setProfile(profileData);
    setDrives(driveData);
    setApplications(applicationData);
    setBookmarks(bookmarkData);
    setNotifications(notificationData);
  }

  useEffect(() => {
    let active = true;

    function refresh() {
      load({ includeProfile: false }).catch((err) => {
        if (active) setMessage(err.response?.data?.message || err.message);
      });
    }

    load()
      .catch((err) => {
        if (active) setMessage(err.response?.data?.message || err.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    const timer = window.setInterval(refresh, 15000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  const bookmarkedIds = useMemo(() => new Set(bookmarks.map((drive) => drive.id)), [bookmarks]);
  const applicationDriveIds = useMemo(() => new Set(applications.map((application) => application.driveId)), [applications]);
  const completion = useMemo(() => {
    if (!profile) return 0;
    const fields = ['fullName', 'collegeEmail', 'branch', 'registerNumber', 'graduationYear', 'cgpa', 'preferredApplicationEmail'];
    const filled = fields.filter((field) => profile[field] !== null && profile[field] !== undefined && profile[field] !== '').length;
    return Math.round((filled / fields.length) * 100);
  }, [profile]);

  async function saveProfile(event) {
    event.preventDefault();
    const saved = await updateStudentProfile(profile);
    setProfile(saved);
    setMessage('Profile saved.');
  }

  async function checkDrive(id) {
    const result = await getEligibility(id);
    setEligibility((current) => ({ ...current, [id]: result }));
  }

  async function apply(drive) {
    if (drive.applicationMode === 'EXTERNAL' && drive.externalApplicationUrl) {
      window.open(drive.externalApplicationUrl, '_blank', 'noopener,noreferrer');
    }
    await applyToDrive(drive.id, { selectedApplicationEmail: profile.preferredApplicationEmail || profile.collegeEmail });
    setMessage('Application recorded.');
    await load();
  }

  async function markSubmitted(id) {
    await markExternalSubmitted(id);
    setMessage('External submission marked as completed.');
    await load();
  }

  async function bookmark(id) {
    await toggleBookmark(id);
    await load();
  }

  if (loading) return <div className="workspace"><p>Loading student workspace...</p></div>;

  return (
    <div className="workspace">
      <header className="workspace-head">
        <div>
          <p className="eyebrow">Student workspace</p>
          <h1>Placement dashboard</h1>
        </div>
        {message && <span className="toast">{message}</span>}
      </header>

      <section className="metric-grid">
        <article className="metric-card"><span>Open drives</span><strong>{drives.length}</strong></article>
        <article className="metric-card"><span>Applications</span><strong>{applications.length}</strong></article>
        <article className="metric-card"><span>Bookmarks</span><strong>{bookmarks.length}</strong></article>
        <article className="metric-card"><span>Profile</span><strong>{completion}%</strong></article>
      </section>

      <section className="two-column">
        <form className="panel form-panel" onSubmit={saveProfile}>
          <div className="panel-head">
            <div><h2>Profile readiness</h2><span>{profile.profileComplete ? 'Ready for eligibility checks' : 'Complete key fields to apply confidently'}</span></div>
            <UserRound size={18} />
          </div>
          <div className="progress-track"><span style={{ width: `${completion}%` }} /></div>
          <div className="form-grid">
            <label>Full name<input value={profile.fullName || ''} onChange={(e) => setProfile({ ...profile, fullName: e.target.value })} /></label>
            <label>College email<input value={profile.collegeEmail || ''} disabled /></label>
            <label>Branch<input value={profile.branch || ''} disabled /></label>
            <label>Register number<input value={profile.registerNumber || ''} onChange={(e) => setProfile({ ...profile, registerNumber: e.target.value })} /></label>
            <label>Graduation year<input type="number" value={profile.graduationYear || ''} onChange={(e) => setProfile({ ...profile, graduationYear: Number(e.target.value) })} /></label>
            <label>CGPA<input type="number" step="0.01" value={profile.cgpa || ''} onChange={(e) => setProfile({ ...profile, cgpa: Number(e.target.value) })} /></label>
            <label>Active backlogs<input type="number" value={profile.activeBacklogs ?? 0} onChange={(e) => setProfile({ ...profile, activeBacklogs: Number(e.target.value) })} /></label>
            <label>Phone<input value={profile.phoneNumber || ''} onChange={(e) => setProfile({ ...profile, phoneNumber: e.target.value })} /></label>
            <label className="wide-field">Preferred application email<input value={profile.preferredApplicationEmail || ''} onChange={(e) => setProfile({ ...profile, preferredApplicationEmail: e.target.value })} /></label>
          </div>
          <button className="primary-button"><FileCheck2 size={17} /> Save profile</button>
        </form>

        <div className="panel">
          <div className="panel-head"><div><h2>Notifications</h2><span>Latest placement updates</span></div><Clock size={18} /></div>
          <div className="table-list">
            {notifications.map((item) => (
              <div className="table-row" key={item.id}>
                <div><strong>{item.title}</strong><span>{item.message}</span></div>
                <em>{item.type}</em>
              </div>
            ))}
            {!notifications.length && <p className="muted">No notifications yet.</p>}
          </div>
        </div>
      </section>

      <section className="panel">
        <div className="panel-head"><div><h2>Placement drives</h2><span>Check eligibility before applying</span></div><BriefcaseBusiness size={18} /></div>
        <div className="drive-grid">
          {drives.map((drive) => {
            const result = eligibility[drive.id];
            return (
              <article className="drive-card" key={drive.id}>
                <div className="drive-card-head">
                  <span className="logo-mark">{drive.company.name.slice(0, 2).toUpperCase()}</span>
                  <em className={`status-pill ${drive.status.toLowerCase()}`}>{drive.status}</em>
                </div>
                <div className="drive-card-body">
                  <strong>{drive.company.name}</strong>
                  <h3>{drive.jobTitle}</h3>
                  <p>{drive.jobLocation || 'Location TBA'} | {drive.salaryDetails || 'CTC TBA'} | {drive.applicationMode}</p>
                  <div className="tag-row">
                    <span>{drive.eligibleBranches}</span>
                    <span>{drive.eligibleGraduationYears}</span>
                    <span>CGPA {drive.minimumCgpa || 'Any'}</span>
                  </div>
                  {result && (
                    <div className={`eligibility-note ${result.eligible ? 'ok' : 'warn'}`}>
                      {result.eligible ? 'Eligible to apply' : result.reasons.join(', ')}
                    </div>
                  )}
                </div>
                <div className="action-row">
                  <button className="compact-button" onClick={() => bookmark(drive.id)}><Bookmark size={15} /> {bookmarkedIds.has(drive.id) ? 'Saved' : 'Save'}</button>
                  <button className="compact-button" onClick={() => checkDrive(drive.id)}>Check</button>
                  <button className="compact-button primary-compact" disabled={applicationDriveIds.has(drive.id)} onClick={() => apply(drive)}>
                    {drive.applicationMode === 'EXTERNAL' ? <ExternalLink size={15} /> : <CheckCircle2 size={15} />}
                    {applicationDriveIds.has(drive.id) ? 'Applied' : 'Apply'}
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      </section>

      <section className="panel">
        <div className="panel-head"><h2>My applications</h2><span>{applications.length} records</span></div>
        <div className="table-list">
          {applications.map((application) => (
            <div className="table-row" key={application.id}>
              <div>
                <strong>{application.companyName}</strong>
                <span>{application.jobTitle} | {application.selectedApplicationEmail}</span>
              </div>
              <em className={`status-pill ${application.status.toLowerCase()}`}>{application.status}</em>
              {!application.externalSubmittedAt && (
                <button className="compact-button" onClick={() => markSubmitted(application.id)}>Mark submitted</button>
              )}
            </div>
          ))}
          {!applications.length && <p className="muted">No applications submitted yet.</p>}
        </div>
      </section>
    </div>
  );
}
