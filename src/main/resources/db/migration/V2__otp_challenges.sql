CREATE TABLE gs_otp (
 id UUID PRIMARY KEY, mobile VARCHAR(255), code_hash VARCHAR(255), expires_at TIMESTAMP WITH TIME ZONE,
 attempts INTEGER NOT NULL, consumed BOOLEAN NOT NULL
);
CREATE INDEX otp_expiry ON gs_otp(expires_at);
