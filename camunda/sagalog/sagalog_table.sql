-- Table: public.sagalog
-- DROP TABLE public.sagalog;

CREATE TABLE public.sagalog
(
    saga_id text COLLATE pg_catalog."default" NOT NULL,
    state text COLLATE pg_catalog."default" NOT NULL,
    time_stamp timestamp(0) without time zone NOT NULL,
    compensation boolean NOT NULL,
    CONSTRAINT sagalog_pkey PRIMARY KEY (saga_id, time_stamp)
)

TABLESPACE pg_default;

ALTER TABLE public.sagalog
    OWNER to postgres;