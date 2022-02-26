CREATE TABLE publish_requests
(
	id            INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
	owner         TEXT    NOT NULL,
	repository    TEXT    NOT NULL,
	plugin        TEXT    NOT NULL,
	target_commit TEXT    NOT NULL,
	message_id    INTEGER NULL,
	updates       INTEGER DEFAULT 0
);

CREATE TABLE plugin_repositories
(
	id              INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
	owner           TEXT    NOT NULL,
	repository      TEXT    NOT NULL,
	approved_commits TEXT    NOT NULL
)
