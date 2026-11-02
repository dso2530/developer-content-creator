CREATE TABLE registered_repositories
(
    id                  UUID         NOT NULL,
    github_repository_id BIGINT       NOT NULL,
    installation_id     BIGINT       NOT NULL,
    name                VARCHAR(255) NOT NULL,
    full_name           VARCHAR(512) NOT NULL,
    html_url            TEXT         NOT NULL,
    default_branch      VARCHAR(255),
    visibility          VARCHAR(20)  NOT NULL,

    CONSTRAINT pk_registered_repositories
        PRIMARY KEY (id),

    CONSTRAINT uk_registered_repositories_installation_repository
        UNIQUE (installation_id, github_repository_id),

    CONSTRAINT chk_registered_repositories_github_repository_id
        CHECK (github_repository_id > 0),

    CONSTRAINT chk_registered_repositories_installation_id
        CHECK (installation_id > 0),

    CONSTRAINT chk_registered_repositories_name
        CHECK (btrim(name) <> ''),

    CONSTRAINT chk_registered_repositories_full_name
        CHECK (btrim(full_name) <> ''),

    CONSTRAINT chk_registered_repositories_default_branch
        CHECK (default_branch IS NULL OR btrim(default_branch) <> ''),

    CONSTRAINT chk_registered_repositories_visibility
        CHECK (visibility IN ('PUBLIC', 'PRIVATE'))
);