CREATE TABLE github_installations
(
    installation_id   BIGINT       NOT NULL,
    github_account_id BIGINT       NOT NULL,
    account_login     VARCHAR(255) NOT NULL,
    account_type      VARCHAR(32)  NOT NULL,
    user_id           UUID         NOT NULL,

    CONSTRAINT pk_github_installations
        PRIMARY KEY (installation_id),

    CONSTRAINT chk_github_installations_installation_id
        CHECK (installation_id > 0),

    CONSTRAINT chk_github_installations_account_id
        CHECK (github_account_id > 0),

    CONSTRAINT chk_github_installations_account_login
        CHECK (btrim(account_login) <> ''),

    CONSTRAINT chk_github_installations_account_type
        CHECK (account_type IN ('USER', 'ORGANIZATION'))
);

CREATE INDEX idx_github_installations_user_id
    ON github_installations (user_id);
