-- Add ADMIN to the allowed roles and enforce the constraint at the DB level.
-- The role column was unconstrained in V1; this locks it to the known enum values.

ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('CANDIDATE', 'EMPLOYER', 'ADMIN'));
