
-- V1__baseline.sql  (community service)


CREATE TABLE community.posts (
                                 id           uuid NOT NULL,
                                 author       character varying(255),
                                 author_id    character varying(255),
                                 avatar_color character varying(255),
                                 created_at   timestamp(6) with time zone NOT NULL,
                                 location     character varying(255),
                                 tag          character varying(255),
                                 text         character varying(2000),
                                 CONSTRAINT posts_pkey PRIMARY KEY (id)
);

CREATE TABLE community.replies (
                                   id             uuid NOT NULL,
                                   author         character varying(255),
                                   created_at     timestamp(6) with time zone NOT NULL,
                                   text           character varying(2000),
                                   verified_helper boolean NOT NULL,
                                   post_id        uuid,
                                   CONSTRAINT replies_pkey PRIMARY KEY (id),
                                   CONSTRAINT fklpjxe9eiutj9ybkgketdk1o8s FOREIGN KEY (post_id) REFERENCES community.posts(id)
);