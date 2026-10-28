CREATE TABLE gs_account (
 id UUID PRIMARY KEY, mobile VARCHAR(255) NOT NULL UNIQUE, name VARCHAR(255), role VARCHAR(255),
 active BOOLEAN NOT NULL, password_hash VARCHAR(255), created_at TIMESTAMP WITH TIME ZONE,
 consent_version VARCHAR(255), consent_at TIMESTAMP WITH TIME ZONE
);
CREATE TABLE gs_session (token_hash VARCHAR(255) PRIMARY KEY, account_id UUID REFERENCES gs_account(id), expires_at TIMESTAMP WITH TIME ZONE);
CREATE INDEX session_expiry ON gs_session(expires_at);
CREATE TABLE gs_record (
 id UUID PRIMARY KEY, kind VARCHAR(255) NOT NULL, owner_id UUID NOT NULL, status VARCHAR(255), unique_key VARCHAR(255) UNIQUE,
 payload VARCHAR(50000), created_at TIMESTAMP WITH TIME ZONE, updated_at TIMESTAMP WITH TIME ZONE, version BIGINT NOT NULL
);
CREATE INDEX record_kind ON gs_record(kind);
CREATE INDEX record_owner ON gs_record(owner_id);
CREATE TABLE gs_audit (
 id UUID PRIMARY KEY, actor_id UUID, record_id UUID, action VARCHAR(255), kind VARCHAR(255),
 previous_status VARCHAR(255), next_status VARCHAR(255), occurred_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX audit_record ON gs_audit(record_id, occurred_at);
CREATE TABLE gs_upload (
 id UUID PRIMARY KEY, owner_id UUID, record_id UUID REFERENCES gs_record(id), filename VARCHAR(255), mime VARCHAR(255), size BIGINT NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX upload_record ON gs_upload(record_id);
