CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(180) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE student_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    branch VARCHAR(40) NOT NULL,
    register_number VARCHAR(60),
    graduation_year INT,
    cgpa DECIMAL(4,2),
    active_backlogs INT NOT NULL DEFAULT 0,
    phone_number VARCHAR(30),
    preferred_application_email VARCHAR(180),
    resume_url VARCHAR(500),
    profile_complete BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_student_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_name VARCHAR(120) NOT NULL,
    college_email VARCHAR(180) NOT NULL,
    branch VARCHAR(40) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP NULL,
    accepted_at TIMESTAMP NULL,
    last_delivery_error VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_invitations_college_email UNIQUE (college_email),
    CONSTRAINT uk_invitations_token_hash UNIQUE (token_hash)
);

CREATE TABLE companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    website VARCHAR(500),
    logo_url VARCHAR(500),
    industry VARCHAR(120),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_companies_name UNIQUE (name)
);

CREATE TABLE placement_drives (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    job_title VARCHAR(180) NOT NULL,
    job_description VARCHAR(5000),
    required_skills VARCHAR(1000),
    job_location VARCHAR(160),
    employment_type VARCHAR(80),
    salary_details VARCHAR(160),
    eligible_branches VARCHAR(500) NOT NULL,
    eligible_graduation_years VARCHAR(160) NOT NULL,
    minimum_cgpa DECIMAL(4,2),
    maximum_active_backlogs INT,
    registration_deadline DATE,
    assessment_date DATE,
    interview_date DATE,
    application_mode VARCHAR(30) NOT NULL,
    external_application_url VARCHAR(500),
    internal_application_instructions VARCHAR(2000),
    status VARCHAR(30) NOT NULL,
    published_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_drives_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

CREATE TABLE applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    drive_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    selected_application_email VARCHAR(180),
    external_submitted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_applications_student_drive UNIQUE (student_id, drive_id),
    CONSTRAINT fk_applications_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_applications_drive FOREIGN KEY (drive_id) REFERENCES placement_drives(id)
);

CREATE TABLE application_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    actor_id BIGINT NOT NULL,
    note VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_application_events_application FOREIGN KEY (application_id) REFERENCES applications(id),
    CONSTRAINT fk_application_events_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE TABLE bookmarks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    drive_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_bookmarks_student_drive UNIQUE (student_id, drive_id),
    CONSTRAINT fk_bookmarks_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_bookmarks_drive FOREIGN KEY (drive_id) REFERENCES placement_drives(id)
);

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    type VARCHAR(80),
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(80),
    details VARCHAR(2000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_invitations_status ON invitations(status);
CREATE INDEX idx_drives_status_deadline ON placement_drives(status, registration_deadline);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, read_at);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
