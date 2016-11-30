CREATE TABLE IF NOT EXISTS device (
	id 			INT		PRIMARY KEY NOT NULL,
	name 		TEXT				NOT NULL,
	room		TEXT				NOT NULL,
	floor		TEXT				NOT NULL,
	model		TEXT				NOT NULL,
	protocol 	TEXT				NOT NULL,
	house		TEXT,
	unit		TEXT,
	hwretries	INT					NOT NULL DEFAULT 0,
	swretries	INT					NOT NULL DEFAULT 0,
	icon		VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS device_schedule_entry (
	id 			INTEGER PRIMARY KEY AUTOINCREMENT,
	device		INT		NOT NULL,
	description 	VARCHAR(255),
	lastSent		UNSIGNED BIGINT,
	enforceDelay 	UNSIGNED BIGINT,
	resendInterval 	UNSIGNED BIGINT,
	command		INT		NOT NULL,
	startHour	INT		NOT NULL,
	startMinute	INT		NOT NULL,
	tdValue		INT		NOT NULL,

	FOREIGN KEY(device) REFERENCES device(id)
);

CREATE TABLE IF NOT EXISTS user_device (
	user	INTEGER NOT NULL,
	device	INT		NOT NULL,
	bookmarked INT	NOT NULL,
	position   INT  NOT NULL,

	FOREIGN KEY(device) REFERENCES device(id)
);
