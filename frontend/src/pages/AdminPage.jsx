import { useEffect, useMemo, useState } from 'react';
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Building2, FileText, ListChecks, Rocket, Send, Sparkles, UsersRound } from 'lucide-react';
import {
  createCompany,
  createDrive,
  createInvitation,
  extractAnnouncement,
  getAdminDashboard,
  getAdminDrives,
  getCompanies,
  getDriveApplications,
  getInvitations,
  publishDrive,
  updateApplicationStatus
} from '../services/api.js';

const colors = ['#2563eb', '#16a34a', '#f59e0b', '#dc2626', '#7c3aed', '#0891b2'];

function invitationReadyMessage(invitation, fallback) {
  if (invitation.status === 'FAILED') {
    return `Invitation email failed: ${invitation.lastDeliveryError || 'check SMTP settings'}`;
  }
  if (!invitation.registrationUrl) return fallback;
  if (invitation.developmentToken) {
    return `Development mode: email was not sent. Share link: ${invitation.registrationUrl} | code: ${invitation.developmentToken}`;
  }
  const code = invitation.developmentToken ? ` | code: ${invitation.developmentToken}` : '';
  return `Invitation ready: ${invitation.registrationUrl}${code}`;
}

export default function AdminPage() {
  const [dashboard, setDashboard] = useState(null);
  const [invitations, setInvitations] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [drives, setDrives] = useState([]);
  const [applicants, setApplicants] = useState([]);
  const [selectedDriveId, setSelectedDriveId] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [announcement, setAnnouncement] = useState('ABC Technologies is hiring Software Engineer Trainees. Eligible branches: IT, CSE, ECE. Graduation year: 2027. Minimum CGPA: 7.0. No active backlogs. CTC: 5 LPA. Location: Chennai. Registration closes on 20 October 2026. Apply through the official company portal: https://example.com/careers.');
  const [extracted, setExtracted] = useState(null);
  const [inviteForm, setInviteForm] = useState({ name: '', collegeEmail: '', branch: 'IT' });
  const [companyForm, setCompanyForm] = useState({ name: '', description: '', website: '', industry: '' });
  const [driveForm, setDriveForm] = useState({
    companyId: '',
    jobTitle: '',
    eligibleBranches: 'IT,CSE',
    eligibleGraduationYears: '2027',
    minimumCgpa: '7.0',
    maximumActiveBacklogs: '0',
    registrationDeadline: '',
    applicationMode: 'INTERNAL',
    externalApplicationUrl: ''
  });

  async function load() {
    const [dashboardData, invitationData, companyData, driveData] = await Promise.all([
      getAdminDashboard(),
      getInvitations(),
      getCompanies(),
      getAdminDrives()
    ]);
    setDashboard(dashboardData);
    setInvitations(invitationData);
    setCompanies(companyData);
    setDrives(driveData);
    setDriveForm((current) => (!current.companyId && companyData[0] ? { ...current, companyId: String(companyData[0].id) } : current));
    setSelectedDriveId((current) => (!current && driveData[0] ? String(driveData[0].id) : current));
  }

  useEffect(() => {
    let active = true;

    function refresh() {
      load().catch((err) => {
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

  useEffect(() => {
    if (!selectedDriveId) return;
    getDriveApplications(selectedDriveId)
      .then(setApplicants)
      .catch((err) => setMessage(err.response?.data?.message || err.message));
  }, [selectedDriveId]);

  const metricData = useMemo(() => dashboard ? [
    { label: 'Invited', value: dashboard.invitedStudents },
    { label: 'Registered', value: dashboard.registeredStudents },
    { label: 'Companies', value: dashboard.totalCompanies },
    { label: 'Drives', value: dashboard.publishedDrives },
    { label: 'Applications', value: dashboard.totalApplications },
    { label: 'Selected', value: dashboard.selectedStudents }
  ] : [], [dashboard]);

  const statusData = useMemo(() => dashboard ? Object.entries(dashboard.applicationDistribution).map(([name, value]) => ({ name, value })) : [], [dashboard]);

  async function submitInvitation(event) {
    event.preventDefault();
    try {
      const invitation = await createInvitation(inviteForm);
      setInviteForm({ name: '', collegeEmail: '', branch: 'IT' });
      setMessage(invitationReadyMessage(invitation, 'Invitation created and queued for delivery.'));
      await load();
    } catch (err) {
      setMessage(err.response?.data?.message || err.message);
    }
  }

  async function submitCompany(event) {
    event.preventDefault();
    try {
      await createCompany(companyForm);
      setCompanyForm({ name: '', description: '', website: '', industry: '' });
      setMessage('Company saved.');
      await load();
    } catch (err) {
      setMessage(err.response?.data?.message || err.message);
    }
  }

  async function submitDrive(event) {
    event.preventDefault();
    try {
      await createDrive({
        ...driveForm,
        companyId: Number(driveForm.companyId),
        minimumCgpa: Number(driveForm.minimumCgpa),
        maximumActiveBacklogs: Number(driveForm.maximumActiveBacklogs),
        registrationDeadline: driveForm.registrationDeadline || null,
        applicationMode: driveForm.applicationMode
      });
      setMessage('Drive saved as draft.');
      await load();
    } catch (err) {
      setMessage(err.response?.data?.message || err.message);
    }
  }

  async function parseAnnouncement() {
    const result = await extractAnnouncement(announcement);
    setExtracted(result);
    setDriveForm((current) => ({
      ...current,
      jobTitle: result.fields.jobTitle || current.jobTitle,
      eligibleBranches: result.fields.eligibleBranches || current.eligibleBranches,
      eligibleGraduationYears: result.fields.eligibleGraduationYears || current.eligibleGraduationYears,
      minimumCgpa: result.fields.minimumCgpa || current.minimumCgpa,
      maximumActiveBacklogs: result.fields.maximumActiveBacklogs ?? current.maximumActiveBacklogs,
      externalApplicationUrl: result.fields.externalApplicationUrl || current.externalApplicationUrl,
      applicationMode: result.fields.applicationMode || current.applicationMode
    }));
    setMessage('Announcement parsed with rule-based extraction.');
  }

  async function publish(id) {
    await publishDrive(id);
    setMessage('Drive published and students notified.');
    await load();
  }

  async function moveApplicant(id, status) {
    await updateApplicationStatus(id, { status, note: 'Updated from coordinator dashboard' });
    setMessage('Application status updated.');
    setApplicants(await getDriveApplications(selectedDriveId));
    await load();
  }

  if (loading) return <div className="workspace"><p>Loading coordinator workspace...</p></div>;

  return (
    <div className="workspace">
      <header className="workspace-head">
        <div>
          <p className="eyebrow">Coordinator workspace</p>
          <h1>Placement operations</h1>
        </div>
        {message && <span className="toast">{message}</span>}
      </header>

      <section className="metric-grid">
        {metricData.map((metric) => (
          <article key={metric.label} className="metric-card">
            <span>{metric.label}</span>
            <strong>{metric.value}</strong>
          </article>
        ))}
      </section>

      <section className="process-strip">
        {[
          ['1', 'Import or invite students', `${dashboard.pendingInvitations} pending`],
          ['2', 'Create company and drive', `${dashboard.publishedDrives} published`],
          ['3', 'Check applicants', `${dashboard.totalApplications} applications`],
          ['4', 'Update results', `${dashboard.selectedStudents} selected`]
        ].map(([step, title, detail]) => (
          <article key={step}>
            <strong>{step}</strong>
            <span>{title}</span>
            <small>{detail}</small>
          </article>
        ))}
      </section>

      <section className="two-column">
        <div className="panel">
          <div className="panel-head"><div><h2>Live analytics</h2><span>Database-backed placement metrics</span></div></div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={metricData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#d7dee8" />
              <XAxis dataKey="label" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="value" fill="#2563eb" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
        <div className="panel">
          <div className="panel-head"><div><h2>Application status</h2><span>Lifecycle distribution</span></div></div>
          <ResponsiveContainer width="100%" height={240}>
            <PieChart>
              <Pie data={statusData} dataKey="value" nameKey="name" outerRadius={90}>
                {statusData.map((entry, index) => <Cell key={entry.name} fill={colors[index % colors.length]} />)}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </section>

      <section className="three-column">
        <form className="panel form-panel" onSubmit={submitInvitation}>
          <div className="panel-head"><div><h2>Invite student</h2><span>Create a secure invitation record</span></div><Send size={18} /></div>
          <label>Name<input placeholder="Student name" value={inviteForm.name} onChange={(e) => setInviteForm({ ...inviteForm, name: e.target.value })} /></label>
          <label>College email<input placeholder="student@college.edu" value={inviteForm.collegeEmail} onChange={(e) => setInviteForm({ ...inviteForm, collegeEmail: e.target.value })} /></label>
          <label>Branch<input placeholder="IT" value={inviteForm.branch} onChange={(e) => setInviteForm({ ...inviteForm, branch: e.target.value })} /></label>
          <button className="primary-button"><UsersRound size={17} /> Create invitation</button>
        </form>

        <form className="panel form-panel" onSubmit={submitCompany}>
          <div className="panel-head"><div><h2>Company</h2><span>Reusable company profile</span></div><Building2 size={18} /></div>
          <label>Company name<input placeholder="ABC Technologies" value={companyForm.name} onChange={(e) => setCompanyForm({ ...companyForm, name: e.target.value })} /></label>
          <label>Website<input placeholder="https://example.com" value={companyForm.website} onChange={(e) => setCompanyForm({ ...companyForm, website: e.target.value })} /></label>
          <label>Industry<input placeholder="Software" value={companyForm.industry} onChange={(e) => setCompanyForm({ ...companyForm, industry: e.target.value })} /></label>
          <label>Description<textarea placeholder="Brief company description" value={companyForm.description} onChange={(e) => setCompanyForm({ ...companyForm, description: e.target.value })} /></label>
          <button className="primary-button"><Building2 size={17} /> Save company</button>
        </form>

        <form className="panel form-panel" onSubmit={submitDrive}>
          <div className="panel-head"><div><h2>Drive draft</h2><span>Review before publishing</span></div><Rocket size={18} /></div>
          <label>Company<select value={driveForm.companyId} onChange={(e) => setDriveForm({ ...driveForm, companyId: e.target.value })}>
            {companies.map((company) => <option key={company.id} value={company.id}>{company.name}</option>)}
          </select></label>
          <label>Job title<input placeholder="Software Engineer Trainee" value={driveForm.jobTitle} onChange={(e) => setDriveForm({ ...driveForm, jobTitle: e.target.value })} /></label>
          <div className="form-grid">
            <label>Branches<input placeholder="IT,CSE" value={driveForm.eligibleBranches} onChange={(e) => setDriveForm({ ...driveForm, eligibleBranches: e.target.value })} /></label>
            <label>Graduation years<input placeholder="2027" value={driveForm.eligibleGraduationYears} onChange={(e) => setDriveForm({ ...driveForm, eligibleGraduationYears: e.target.value })} /></label>
            <label>Minimum CGPA<input type="number" step="0.1" value={driveForm.minimumCgpa} onChange={(e) => setDriveForm({ ...driveForm, minimumCgpa: e.target.value })} /></label>
            <label>Max backlogs<input type="number" value={driveForm.maximumActiveBacklogs} onChange={(e) => setDriveForm({ ...driveForm, maximumActiveBacklogs: e.target.value })} /></label>
          </div>
          <label>Deadline<input type="date" value={driveForm.registrationDeadline} onChange={(e) => setDriveForm({ ...driveForm, registrationDeadline: e.target.value })} /></label>
          <label>Mode<select value={driveForm.applicationMode} onChange={(e) => setDriveForm({ ...driveForm, applicationMode: e.target.value })}>
            <option value="INTERNAL">Internal PlaceTrack form</option>
            <option value="EXTERNAL">External company portal</option>
          </select></label>
          {driveForm.applicationMode === 'EXTERNAL' && (
            <label>External URL<input placeholder="https://example.com/careers" value={driveForm.externalApplicationUrl} onChange={(e) => setDriveForm({ ...driveForm, externalApplicationUrl: e.target.value })} /></label>
          )}
          <button className="primary-button"><FileText size={17} /> Save draft</button>
        </form>
      </section>

      <section className="two-column">
        <div className="panel form-panel">
          <div className="panel-head"><div><h2>Smart Announcement Parser</h2><span>Rule-based extraction, never auto-publishes</span></div><Sparkles size={18} /></div>
          <textarea className="large-textarea" value={announcement} onChange={(e) => setAnnouncement(e.target.value)} />
          <button type="button" className="primary-button" onClick={parseAnnouncement}><Sparkles size={17} /> Extract fields</button>
          {extracted && (
            <div className="extraction-card">
              <span className="status-pill published">{extracted.extractionMode}</span>
              <pre className="json-preview">{JSON.stringify(extracted.fields, null, 2)}</pre>
              {!!extracted.missingFields.length && <p className="inline-error">Missing: {extracted.missingFields.join(', ')}</p>}
            </div>
          )}
        </div>
        <div className="panel">
          <div className="panel-head"><div><h2>Placement drives</h2><span>{drives.length} total</span></div><ListChecks size={18} /></div>
          <div className="table-list">
            {drives.map((drive) => (
              <div className="table-row" key={drive.id}>
                <div><strong>{drive.company.name}</strong><span>{drive.jobTitle} | {drive.eligibleBranches} | {drive.registrationDeadline || 'No deadline'}</span></div>
                <em className={`status-pill ${drive.status.toLowerCase()}`}>{drive.status}</em>
                {drive.status === 'DRAFT' && <button className="compact-button" onClick={() => publish(drive.id)}>Publish</button>}
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="panel">
        <div className="panel-head">
          <div><h2>Applicant management</h2><span>Review and move candidates through the pipeline</span></div>
          <select value={selectedDriveId} onChange={(e) => setSelectedDriveId(e.target.value)}>
            {drives.map((drive) => <option key={drive.id} value={drive.id}>{drive.company.name} - {drive.jobTitle}</option>)}
          </select>
        </div>
        <div className="table-list">
          {applicants.map((application) => (
            <div className="table-row" key={application.id}>
              <div><strong>{application.companyName}</strong><span>{application.jobTitle} | {application.selectedApplicationEmail}</span></div>
              <em className={`status-pill ${application.status.toLowerCase()}`}>{application.status}</em>
              <select value={application.status} onChange={(e) => moveApplicant(application.id, e.target.value)}>
                {['APPLIED', 'ASSESSMENT', 'INTERVIEW', 'SELECTED', 'REJECTED', 'WITHDRAWN'].map((status) => (
                  <option key={status} value={status}>{status}</option>
                ))}
              </select>
            </div>
          ))}
          {!applicants.length && <p className="muted">No applications for this drive yet.</p>}
        </div>
      </section>

      <section className="panel">
        <div className="panel-head"><h2>Invitations</h2><span>{invitations.length} records</span></div>
        <div className="table-list">
          {invitations.map((invitation) => (
            <div className="table-row" key={invitation.id}>
              <div><strong>{invitation.studentName}</strong><span>{invitation.collegeEmail} | {invitation.branch}{invitation.lastDeliveryError ? ` | error ${invitation.lastDeliveryError}` : ''}{invitation.developmentToken ? ` | development mode: email not sent | code ${invitation.developmentToken}` : ''}</span></div>
              <em className={`status-pill ${invitation.status.toLowerCase()}`}>{invitation.status}</em>
              {invitation.registrationUrl && <a className="compact-button" href={invitation.registrationUrl}>Open link</a>}
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
