USE login;

CREATE TABLE IF NOT EXISTS account (
    id INT AUTO_INCREMENT PRIMARY KEY,

    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    password_updated DATETIME NULL,

    email VARCHAR(255) NULL,
    oauth_provider VARCHAR(255) NULL,

    registration_ip VARCHAR(45) NULL,
    registration_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    muted_until DATETIME NULL,
    banned_until DATETIME NULL,

    staffmodlevel INT NOT NULL DEFAULT 0,

    notes TEXT NULL,
    notes_updated DATETIME NULL,

    members BOOLEAN NOT NULL DEFAULT FALSE,

    tfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    tfa_last_code INT NOT NULL DEFAULT 0,
    tfa_secret_base32 VARCHAR(255) NULL,
    tfa_incorrect_attempts INT NOT NULL DEFAULT 0
);
