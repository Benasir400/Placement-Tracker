import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('placetrack_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('placetrack_token');
      localStorage.removeItem('placetrack_user');
    }
    return Promise.reject(error);
  }
);

export async function getHealth() {
  const response = await api.get('/public/health');
  return response.data;
}

export async function getPlatformSummary() {
  const response = await api.get('/public/summary');
  return response.data;
}

export async function login(payload) {
  const response = await api.post('/auth/login', payload);
  return response.data;
}

export async function register(payload) {
  const response = await api.post('/auth/register', payload);
  return response.data;
}

export async function acceptInvitation(payload) {
  const response = await api.post('/auth/accept-invitation', payload);
  return response.data;
}

export async function getInvitationPreview(code) {
  const response = await api.get('/auth/invitation', { params: { code } });
  return response.data;
}

export async function getMe() {
  const response = await api.get('/auth/me');
  return response.data;
}

export async function getAdminDashboard() {
  const response = await api.get('/admin/dashboard');
  return response.data;
}

export async function getInvitations() {
  const response = await api.get('/admin/invitations');
  return response.data;
}

export async function createInvitation(payload) {
  const response = await api.post('/admin/invitations', payload);
  return response.data;
}

export async function createInvitationsBulk(payload) {
  const response = await api.post('/admin/invitations/bulk', payload);
  return response.data;
}

export async function resendInvitation(id) {
  const response = await api.post(`/admin/invitations/${id}/resend`);
  return response.data;
}

export async function revokeInvitation(id) {
  const response = await api.post(`/admin/invitations/${id}/revoke`);
  return response.data;
}

export async function getCompanies() {
  const response = await api.get('/admin/companies');
  return response.data;
}

export async function createCompany(payload) {
  const response = await api.post('/admin/companies', payload);
  return response.data;
}

export async function getAdminDrives() {
  const response = await api.get('/admin/drives');
  return response.data;
}

export async function createDrive(payload) {
  const response = await api.post('/admin/drives', payload);
  return response.data;
}

export async function publishDrive(id) {
  const response = await api.post(`/admin/drives/${id}/publish`);
  return response.data;
}

export async function getDriveApplications(id) {
  const response = await api.get(`/admin/drives/${id}/applications`);
  return response.data;
}

export async function updateApplicationStatus(id, payload) {
  const response = await api.patch(`/admin/applications/${id}/status`, payload);
  return response.data;
}

export async function extractAnnouncement(text) {
  const response = await api.post('/admin/announcements/extract', { text });
  return response.data;
}

export async function getStudentProfile() {
  const response = await api.get('/student/profile');
  return response.data;
}

export async function updateStudentProfile(payload) {
  const response = await api.put('/student/profile', payload);
  return response.data;
}

export async function getStudentDrives() {
  const response = await api.get('/student/drives');
  return response.data;
}

export async function getEligibility(id) {
  const response = await api.get(`/student/drives/${id}/eligibility`);
  return response.data;
}

export async function applyToDrive(id, payload) {
  const response = await api.post(`/student/drives/${id}/applications`, payload);
  return response.data;
}

export async function getApplications() {
  const response = await api.get('/student/applications');
  return response.data;
}

export async function markExternalSubmitted(id) {
  const response = await api.post(`/student/applications/${id}/mark-submitted`);
  return response.data;
}

export async function toggleBookmark(id) {
  await api.post(`/student/bookmarks/${id}`);
}

export async function getBookmarks() {
  const response = await api.get('/student/bookmarks');
  return response.data;
}

export async function getNotifications() {
  const response = await api.get('/student/notifications');
  return response.data;
}

export default api;
