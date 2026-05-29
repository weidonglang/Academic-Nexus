create table student_registration_application (
  id bigint auto_increment primary key,
  student_id bigint not null,
  type varchar(50) not null,
  target_name varchar(120) not null,
  course_name varchar(120),
  reason varchar(500) not null,
  status varchar(30) not null,
  submitted_at timestamp(6) not null,
  reviewed_at timestamp(6),
  review_comment varchar(500),
  constraint fk_registration_application_student foreign key (student_id) references student (id)
);

create index idx_registration_application_student on student_registration_application (student_id, submitted_at);
create index idx_registration_application_status on student_registration_application (status);
