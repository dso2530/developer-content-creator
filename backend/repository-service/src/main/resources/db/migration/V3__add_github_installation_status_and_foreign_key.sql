-- Add installation lifecycle status.
-- Existing installations are considered active.
ALTER TABLE github_installations
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE github_installations
    ADD CONSTRAINT chk_github_installations_status
    CHECK (status IN ('ACTIVE', 'UNINSTALLED'));

-- Prevent registered repositories from referencing
-- a non-existent GitHub installation.
ALTER TABLE registered_repositories
    ADD CONSTRAINT fk_registered_repositories_installation
    FOREIGN KEY (installation_id)
    REFERENCES github_installations (installation_id);