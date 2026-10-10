import { useEffect, useMemo, useState } from 'react';
import * as XLSX from 'xlsx';
import { Building2, ClipboardList, FileText, MailCheck, RefreshCw, Rocket, Send, Sparkles, Upload, UsersRound } from 'lucide-react';
import {
  createCompany,
  createDrive,
  createInvitation,
  createInvitationsBulk,
  extractAnnouncement,
  getAdminDashboard,
  getAdminDrives,
  getCompanies,
  getDriveApplications,
  getInvitations,
  publishDrive,
  resendInvitation,
  updateApplicationStatus
} from '../services/api.js';

const tabs = [
  ['invite', 'Invite students', MailCheck],
  ['missed', 'Missed links', RefreshCw],
  ['drive', 'Create drive', Sparkles],
  ['drives', 'Drives', ClipboardList],
  ['applicants', 'Applicants', UsersRound]
];

const sampleList = `name,collegeEmail,branch
Ananya Rao,ananya.rao@college.edu,CSE
Vikram Iyer,vikram.iyer@college.edu,IT`;

const standardTemplate = `Company Name: SaasAnt Infotech
Website: https://www.saasant.com/
Job Title: Software Engineer Trainee
Employment Type: Internship
Role Description: Software development, technical rounds, and coding assessment.
Required Skills: Java, Python, SQL, Web Development

Eligible Branches: CSE, IT, AD, ECE, CSBS
Graduation Year: 2027
Minimum CGPA: 8.0
Maximum Active Backlogs: 0

CTC: 6 LPA (Stipend: 15k/month)
Job Location: Coimbatore

Registration Deadline: 2026-10-20
Application Link: https://forms.gle/QFF8YmgPc35PEQN57`;

function dateForInput(value) {
  if (!value) return '';
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return '';
  return parsed.toISOString().slice(0, 10);
}

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

export default function CoordinatorPage() {
  const [tab, setTab] = useState('invite');
  const [dashboard, setDashboard] = useState(null);
  const [invitations, setInvitations] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [drives, setDrives] = useState([]);
  const [applicants, setApplicants] = useState([]);
  const [selectedDriveId, setSelectedDriveId] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [importText, setImportText] = useState(sampleList);
  const [importResult, setImportResult] = useState(null);
  const [inviteForm, setInviteForm] = useState({ name: '', collegeEmail: '', branch: 'CSE' });
  const [companyForm, setCompanyForm] = useState({ name: '', description: '', website: '', industry: '' });
  const [announcement, setAnnouncement] = useState('ABC Technologies is hiring Software Engineer Trainees. Eligible branches: IT, CSE, ECE. Graduation year: 2027. Minimum CGPA: 7.0. No active backlogs. CTC: 5 LPA. Location: Chennai. Registration closes on 20 October 2026. Apply through the official company portal: https://example.com/careers.');
  const [extracted, setExtracted] = useState(null);
  const [driveForm, setDriveForm] = useState({
    companyId: '',
    jobTitle: '',
    jobDescription: '',
    requiredSkills: '',
    jobLocation: '',
    employmentType: '',
    salaryDetails: '',
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
      load()
        .catch((err) => active && setMessage(err.response?.data?.message || err.message))
        .finally(() => active && setLoading(false));
    }
    refresh();
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

  const pendingInvitations = useMemo(
    () => invitations.filter((item) => ['PENDING', 'SENT', 'FAILED'].includes(item.status)),
    [invitations]
  );

  function parseStudentRows(text) {
    if (!text) return [];
    const rows = text.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
    if (rows.length === 0) return [];
    
    const firstRowLower = rows[0].toLowerCase();
    const isHeader = firstRowLower.includes('name') || firstRowLower.includes('email') || firstRowLower.includes('branch');
    const dataRows = isHeader ? rows.slice(1) : rows;

    return dataRows.map((line) => {
      let parts = [];
      if (line.includes('\t')) {
        parts = line.split('\t');
      } else if (line.includes(';')) {
        parts = line.split(';');
      } else {
        parts = line.split(',');
      }
      return {
        name: (parts[0] || '').trim().replace(/^["']|["']$/g, ''),
        collegeEmail: (parts[1] || '').trim().replace(/^["']|["']$/g, ''),
        branch: (parts[2] || 'CSE').trim().replace(/^["']|["']$/g, '')
      };
    });
  }

  async function readStudentFile(event) {
    const file = event.target.files?.[0];
    if (!file) return;

    const name = file.name.toLowerCase();
    if (name.endsWith('.xlsx') || name.endsWith('.xls')) {
      try {
        setError('');
        const buffer = await file.arrayBuffer();
        const workbook = XLSX.read(buffer, { type: 'array' });
        const firstSheetName = workbook.SheetNames[0];
        const worksheet = workbook.Sheets[firstSheetName];
        const csvText = XLSX.utils.sheet_to_csv(worksheet);
        setImportText(csvText);
      } catch (err) {
        setError('Failed to parse Excel file: ' + (err.message || 'Invalid format'));
      }
    } else {
      setImportText(await file.text());
    }
  }

  async function sendBulkInvites() {
    try {
      setError('');
      const result = await createInvitationsBulk({ students: parseStudentRows(importText) });
      setImportResult(result);
      setMessage(`${result.sentCount} invitation links generated, ${result.skippedCount} rows skipped.`);
      await load();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send bulk invitations');
    }
  }

  async function submitSingleInvite(event) {
    event.preventDefault();
    try {
      setError('');
      const invitation = await createInvitation(inviteForm);
      setInviteForm({ name: '', collegeEmail: '', branch: 'CSE' });
      setMessage(invitationReadyMessage(invitation, 'Invitation queued for email delivery.'));
      await load();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send invitation');
    }
  }

  async function resend(id) {
    try {
      setError('');
      const invitation = await resendInvitation(id);
      setMessage(invitationReadyMessage(invitation, 'Invitation resent.'));
      await load();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to resend invitation (Invitation may not exist or is already accepted)');
    }
  }

  async function submitCompany(event) {
    event.preventDefault();
    try {
      setError('');
      const company = await createCompany(companyForm);
      setCompanyForm({ name: '', description: '', website: '', industry: '' });
      setDriveForm((current) => ({ ...current, companyId: String(company.id) }));
      setMessage('Company saved. Continue with drive details.');
      await load();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save company');
    }
  }

  async function parseAnnouncement() {
    try {
      setError('');
      const result = await extractAnnouncement(announcement);
      setExtracted(result);
      const fields = result.fields;
      setCompanyForm((current) => ({
        ...current,
        name: fields.companyName || current.name,
        website: fields.externalApplicationUrl || current.website,
        industry: fields.industry || current.industry
      }));
      setDriveForm((current) => ({
        ...current,
        jobTitle: fields.jobTitle || current.jobTitle,
        jobDescription: announcement.trim() || current.jobDescription,
        requiredSkills: fields.requiredSkills || current.requiredSkills,
        jobLocation: fields.jobLocation || current.jobLocation,
        employmentType: fields.employmentType || current.employmentType,
        salaryDetails: fields.salaryDetails || current.salaryDetails,
        eligibleBranches: fields.eligibleBranches || current.eligibleBranches,
        eligibleGraduationYears: fields.eligibleGraduationYears || current.eligibleGraduationYears,
        minimumCgpa: fields.minimumCgpa || current.minimumCgpa,
        maximumActiveBacklogs: fields.maximumActiveBacklogs ?? current.maximumActiveBacklogs,
        registrationDeadline: dateForInput(fields.registrationDeadline) || current.registrationDeadline,
        externalApplicationUrl: fields.externalApplicationUrl || current.externalApplicationUrl,
        applicationMode: fields.applicationMode || current.applicationMode
      }));
      setMessage('One-message job details parsed. Review company and drive fields before saving.');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to parse announcement');
    }
  }

  async function submitDrive(event) {
    event.preventDefault();
    try {
      setError('');
      await createDrive({
        ...driveForm,
        companyId: Number(driveForm.companyId),
        minimumCgpa: Number(driveForm.minimumCgpa),
        maximumActiveBacklogs: Number(driveForm.maximumActiveBacklogs),
        registrationDeadline: driveForm.registrationDeadline || null
      });
      setMessage('Drive saved as draft. Publish when ready.');
      setTab('drives');
      await load();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save drive');
    }
  }

  async function publish(id) {
    try {
      setError('');
      await publishDrive(id);
      setMessage('Drive published. Students will see it in their portal.');
      await load();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to publish drive');
    }
  }

  async function moveApplicant(id, status) {
    try {
      setError('');
      await updateApplicationStatus(id, { status, note: 'Updated from coordinator workspace' });
      setMessage('Student application status updated.');
      if (selectedDriveId) {
        setApplicants(await getDriveApplications(selectedDriveId));
      }
      await load();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update status');
    }
  }

  if (loading && !dashboard) return <div className="workspace"><p>Loading coordinator workspace...</p></div>;

  return (
    <div className="workspace">
      <header className="workspace-head">
        <div>
          <p className="eyebrow">Coordinator workspace</p>
          <h1>Placement operations</h1>
        </div>
        {message && <span className="toast">{message}</span>}
        {error && <span className="toast error-toast" style={{ background: '#fee2e2', color: '#991b1b', border: '1px solid #f87171' }}>{error}</span>}
      </header>

      <section className="summary-strip">
        <article><strong>{dashboard?.invitedStudents ?? 0}</strong><span>Invited</span></article>
        <article><strong>{dashboard?.registeredStudents ?? 0}</strong><span>Registered</span></article>
        <article><strong>{dashboard?.pendingInvitations ?? 0}</strong><span>Pending links</span></article>
        <article><strong>{dashboard?.publishedDrives ?? 0}</strong><span>Open drives</span></article>
        <article><strong>{dashboard?.totalApplications ?? 0}</strong><span>Applications</span></article>
      </section>

      <nav className="module-tabs" aria-label="Coordinator tasks">
        {tabs.map(([id, label, Icon]) => (
          <button key={id} type="button" className={tab === id ? 'active' : ''} onClick={() => setTab(id)}>
            <Icon size={17} />
            {label}
          </button>
        ))}
      </nav>

      {tab === 'invite' && (
        <section className="two-column">
          <div className="panel form-panel">
            <div className="panel-head"><div><h2>Send invitations from Excel list</h2><span>Upload Excel (.xlsx, .xls) / CSV, or copy-paste rows: name, college email, branch</span></div><Upload size={18} /></div>
            <input type="file" accept=".csv,.txt,.xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel" onChange={readStudentFile} />
            <textarea className="large-textarea" value={importText} onChange={(event) => setImportText(event.target.value)} />
            <button type="button" className="primary-button" onClick={sendBulkInvites}><Send size={17} /> Send links to valid students</button>
            {importResult && <ResultList rows={importResult.results} />}
          </div>

          <form className="panel form-panel" onSubmit={submitSingleInvite}>
            <div className="panel-head"><div><h2>Send one missed link</h2><span>For a student accidentally left out</span></div><MailCheck size={18} /></div>
            <label>Name<input value={inviteForm.name} onChange={(e) => setInviteForm({ ...inviteForm, name: e.target.value })} /></label>
            <label>College email<input value={inviteForm.collegeEmail} onChange={(e) => setInviteForm({ ...inviteForm, collegeEmail: e.target.value })} /></label>
            <label>Branch<input value={inviteForm.branch} onChange={(e) => setInviteForm({ ...inviteForm, branch: e.target.value })} /></label>
            <button className="primary-button"><Send size={17} /> Send invitation</button>
          </form>
        </section>
      )}

      {tab === 'missed' && (
        <section className="panel">
          <div className="panel-head"><div><h2>Missed and pending student links</h2><span>Resend a fresh secure link when a student did not receive one</span></div><RefreshCw size={18} /></div>
          <div className="table-list">
            {pendingInvitations.map((invitation) => (
              <div className="table-row" key={invitation.id}>
                <div><strong>{invitation.studentName}</strong><span>{invitation.collegeEmail} | {invitation.branch} | expires {new Date(invitation.expiresAt).toLocaleString()}{invitation.developmentToken ? ` | development mode: email not sent | code ${invitation.developmentToken}` : ''}{invitation.lastDeliveryError ? ` | error ${invitation.lastDeliveryError}` : ''}</span></div>
                <em className={`status-pill ${invitation.status.toLowerCase()}`}>{invitation.status}</em>
                {invitation.registrationUrl && <a className="compact-button" href={invitation.registrationUrl}>Open link</a>}
                <button className="compact-button" onClick={() => resend(invitation.id)}><RefreshCw size={15} /> Resend</button>
              </div>
            ))}
            {!pendingInvitations.length && <p className="muted">No pending invitation links.</p>}
          </div>
        </section>
      )}

      {tab === 'drive' && (
        <section className="two-column">
          <div className="panel form-panel">
            <div className="panel-head"><div><h2>Standard job intake</h2><span>Paste one message; PlaceTrack parses company and drive details</span></div><Sparkles size={18} /></div>
            <textarea className="large-textarea announcement-box" value={announcement} onChange={(e) => setAnnouncement(e.target.value)} />
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginBottom: '16px' }}>
              <button type="button" className="primary-button" onClick={parseAnnouncement}><Sparkles size={17} /> Parse message</button>
              <button type="button" className="compact-button" onClick={() => setAnnouncement(standardTemplate)}><FileText size={15} /> Load standard template</button>
            </div>
            {extracted && (
              <div className="extraction-card">
                <div className="parse-summary">
                  <span className="status-pill published">{extracted.extractionMode}</span>
                  <strong>{Object.values(extracted.fields).filter(Boolean).length} fields found</strong>
                </div>
                <div className="parsed-fields">
                  {Object.entries(extracted.fields).map(([key, value]) => (
                    <span key={key} className={value === null || value === '' ? 'is-missing' : ''}>
                      <strong>{key.replace(/([A-Z])/g, ' $1')}</strong>
                      {value === null || value === '' ? 'Needs review' : String(value)}
                    </span>
                  ))}
                </div>
                {extracted.missingFields.length > 0 && <p className="inline-error">Needs manual input: {extracted.missingFields.join(', ')}</p>}
              </div>
            )}
            <form className="mini-form" onSubmit={submitCompany}>
              <h3>Company profile from message</h3>
              <label>Company name<input value={companyForm.name} onChange={(e) => setCompanyForm({ ...companyForm, name: e.target.value })} /></label>
              <label>Website<input value={companyForm.website} onChange={(e) => setCompanyForm({ ...companyForm, website: e.target.value })} /></label>
              <label>Industry<input value={companyForm.industry} onChange={(e) => setCompanyForm({ ...companyForm, industry: e.target.value })} /></label>
              <label>Description<textarea value={companyForm.description} onChange={(e) => setCompanyForm({ ...companyForm, description: e.target.value })} /></label>
              <button className="compact-button"><Building2 size={15} /> Save company</button>
            </form>
          </div>

          <form className="panel form-panel" onSubmit={submitDrive}>
            <div className="panel-head"><div><h2>Review and save drive</h2><span>Nothing publishes automatically</span></div><Rocket size={18} /></div>
            <label>Company<select value={driveForm.companyId} onChange={(e) => setDriveForm({ ...driveForm, companyId: e.target.value })}>
              <option value="">Select company</option>
              {companies.map((company) => <option key={company.id} value={company.id}>{company.name}</option>)}
            </select></label>
            <label>Job title<input value={driveForm.jobTitle} onChange={(e) => setDriveForm({ ...driveForm, jobTitle: e.target.value })} /></label>
            <label>Description<textarea value={driveForm.jobDescription} onChange={(e) => setDriveForm({ ...driveForm, jobDescription: e.target.value })} /></label>
            <div className="form-grid">
              <label>Skills<input value={driveForm.requiredSkills} onChange={(e) => setDriveForm({ ...driveForm, requiredSkills: e.target.value })} /></label>
              <label>Employment type<input value={driveForm.employmentType} onChange={(e) => setDriveForm({ ...driveForm, employmentType: e.target.value })} /></label>
              <label>Location<input value={driveForm.jobLocation} onChange={(e) => setDriveForm({ ...driveForm, jobLocation: e.target.value })} /></label>
              <label>CTC<input value={driveForm.salaryDetails} onChange={(e) => setDriveForm({ ...driveForm, salaryDetails: e.target.value })} /></label>
              <label>Branches<input value={driveForm.eligibleBranches} onChange={(e) => setDriveForm({ ...driveForm, eligibleBranches: e.target.value })} /></label>
              <label>Graduation year<input value={driveForm.eligibleGraduationYears} onChange={(e) => setDriveForm({ ...driveForm, eligibleGraduationYears: e.target.value })} /></label>
              <label>Minimum CGPA<input type="number" step="0.1" value={driveForm.minimumCgpa} onChange={(e) => setDriveForm({ ...driveForm, minimumCgpa: e.target.value })} /></label>
              <label>Max backlogs<input type="number" value={driveForm.maximumActiveBacklogs} onChange={(e) => setDriveForm({ ...driveForm, maximumActiveBacklogs: e.target.value })} /></label>
            </div>
            <label>Deadline<input type="date" value={driveForm.registrationDeadline} onChange={(e) => setDriveForm({ ...driveForm, registrationDeadline: e.target.value })} /></label>
            <label>Application mode<select value={driveForm.applicationMode} onChange={(e) => setDriveForm({ ...driveForm, applicationMode: e.target.value })}>
              <option value="INTERNAL">Internal PlaceTrack application</option>
              <option value="EXTERNAL">External company website</option>
            </select></label>
            {driveForm.applicationMode === 'EXTERNAL' && <label>External URL<input value={driveForm.externalApplicationUrl} onChange={(e) => setDriveForm({ ...driveForm, externalApplicationUrl: e.target.value })} /></label>}
            <button className="primary-button"><FileText size={17} /> Save draft</button>
          </form>
        </section>
      )}

      {tab === 'drives' && (
        <section className="panel">
          <div className="panel-head"><div><h2>Placement drives</h2><span>Draft, publish, and monitor student-facing jobs</span></div><ClipboardList size={18} /></div>
          <DriveRows drives={drives} onPublish={publish} />
        </section>
      )}

      {tab === 'applicants' && (
        <section className="panel">
          <div className="panel-head">
            <div><h2>Applied students and status</h2><span>Move students through assessment, interview, selected, or rejected</span></div>
            <select value={selectedDriveId} onChange={(e) => setSelectedDriveId(e.target.value)}>
              {drives.map((drive) => <option key={drive.id} value={drive.id}>{drive.company.name} - {drive.jobTitle}</option>)}
            </select>
          </div>
          <div className="table-list">
            {applicants.map((application) => (
              <div className="table-row" key={application.id}>
                <div><strong>{application.selectedApplicationEmail}</strong><span>{application.companyName} | {application.jobTitle} | submitted {new Date(application.submittedAt).toLocaleString()}</span></div>
                <em className={`status-pill ${application.status.toLowerCase()}`}>{application.status}</em>
                <select value={application.status} onChange={(e) => moveApplicant(application.id, e.target.value)}>
                  {['APPLIED', 'ASSESSMENT', 'INTERVIEW', 'SELECTED', 'REJECTED', 'WITHDRAWN'].map((status) => <option key={status} value={status}>{status}</option>)}
                </select>
              </div>
            ))}
            {!applicants.length && <p className="muted">No students have applied for this drive yet.</p>}
          </div>
        </section>
      )}
    </div>
  );
}

function ResultList({ rows }) {
  return (
    <div className="table-list">
      {rows.map((row) => (
        <div className="table-row" key={`${row.rowNumber}-${row.collegeEmail}`}>
          <div><strong>Row {row.rowNumber}: {row.name || 'Unnamed student'}</strong><span>{row.collegeEmail} | {row.message}</span></div>
          <em className={`status-pill ${row.status.toLowerCase()}`}>{row.status}</em>
          {row.registrationUrl && <a className="compact-button" href={row.registrationUrl}>Open link</a>}
        </div>
      ))}
    </div>
  );
}

function DriveRows({ drives, onPublish }) {
  return (
    <div className="table-list">
      {drives.map((drive) => (
        <div className="table-row" key={drive.id}>
          <div><strong>{drive.company.name} - {drive.jobTitle}</strong><span>{drive.salaryDetails || 'CTC TBA'} | {drive.jobLocation || 'Location TBA'} | {drive.eligibleBranches} | deadline {drive.registrationDeadline || 'not set'}</span></div>
          <em className={`status-pill ${drive.status.toLowerCase()}`}>{drive.status}</em>
          {drive.status === 'DRAFT' && <button className="compact-button" onClick={() => onPublish(drive.id)}>Publish</button>}
        </div>
      ))}
    </div>
  );
}
