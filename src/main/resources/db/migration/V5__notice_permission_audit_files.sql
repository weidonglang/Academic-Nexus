create table sys_permission (
  id bigint auto_increment primary key,
  code varchar(80) not null,
  name varchar(120) not null,
  description varchar(240),
  constraint uk_sys_permission_code unique (code)
);

create table sys_role_permission (
  role_id bigint not null,
  permission_id bigint not null,
  primary key (role_id, permission_id),
  constraint fk_sys_role_permission_role foreign key (role_id) references sys_role (id),
  constraint fk_sys_role_permission_permission foreign key (permission_id) references sys_permission (id)
);

create table notice (
  id bigint auto_increment primary key,
  title varchar(120) not null,
  content varchar(1000) not null,
  category varchar(40) not null,
  pinned boolean not null,
  published_at timestamp(6) not null,
  publisher varchar(64) not null
);

create table user_notification (
  id bigint auto_increment primary key,
  user_id bigint not null,
  title varchar(120) not null,
  content varchar(1000) not null,
  category varchar(40) not null,
  read_flag boolean not null,
  created_at timestamp(6) not null,
  read_at timestamp(6),
  related_type varchar(60),
  related_id bigint,
  constraint fk_user_notification_user foreign key (user_id) references sys_user (id)
);

create index idx_user_notification_user_read on user_notification (user_id, read_flag);

create table operation_audit_log (
  id bigint auto_increment primary key,
  operator varchar(64) not null,
  action varchar(80) not null,
  target_type varchar(80) not null,
  target_id varchar(80),
  detail varchar(1000),
  trace_id varchar(80),
  created_at timestamp(6) not null
);

create table status_change_attachment (
  id bigint auto_increment primary key,
  application_id bigint not null,
  original_filename varchar(240) not null,
  stored_path varchar(500) not null,
  content_type varchar(120),
  size_bytes bigint not null,
  uploaded_at timestamp(6) not null,
  constraint fk_status_change_attachment_application foreign key (application_id) references student_status_change_application (id)
);
