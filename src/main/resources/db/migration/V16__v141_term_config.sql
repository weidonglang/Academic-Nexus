create table system_config (
  config_key varchar(120) primary key,
  config_value varchar(500) not null,
  description varchar(500),
  updated_at timestamp(6) not null
);

insert into system_config (config_key, config_value, description, updated_at)
values ('current_term', '2025-2026-2', '当前默认学期，可用于选课、首页统计和演示数据筛选。', current_timestamp);
