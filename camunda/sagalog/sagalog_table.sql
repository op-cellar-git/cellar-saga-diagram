-- Drop table

-- DROP TABLE public.sagalog;

CREATE TABLE public.sagalog (
	saga_id text NOT NULL,
	state text NOT NULL,
	time_stamp timestamp NOT NULL,
	compensation bool NOT NULL,
	activity_id text NOT NULL,
	params text NULL,
	CONSTRAINT sagalog_pk PRIMARY KEY (saga_id, activity_id, state)
);

