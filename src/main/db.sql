create table drug_dependence
(
	uuid varchar(36) null,
	player varchar(16) null,
	drug_name varchar(32) null,
	used_count int default 0 null,
	used_time datetime default now() null,
	level int default 0 null,
	symptoms_total int default 0 null,
	constraint drug_dependence_pk
		unique (uuid, drug_name)
);

create table log
(
	uuid varchar(36) null,
	player varchar(16) null,
	drug_name varchar(32) null,
	date datetime default now() null
);

