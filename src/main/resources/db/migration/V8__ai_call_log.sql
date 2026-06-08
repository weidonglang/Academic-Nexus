create table ai_call_log (
  id bigint auto_increment primary key,
  user_id bigint,
  username varchar(64) not null,
  role_codes varchar(240),
  function_type varchar(40) not null,
  prompt_summary varchar(500),
  model_name varchar(120),
  duration_ms bigint not null,
  success boolean not null,
  error_message varchar(500),
  created_at timestamp(6) not null,
  constraint fk_ai_call_log_user foreign key (user_id) references sys_user (id)
);

create index idx_ai_call_log_created_at on ai_call_log (created_at);
create index idx_ai_call_log_function_type on ai_call_log (function_type, created_at);
