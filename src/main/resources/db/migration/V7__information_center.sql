create table academic_warning (
  id bigint auto_increment primary key,
  student_id bigint not null,
  term varchar(80) not null,
  level varchar(30) not null,
  reason varchar(300) not null,
  status varchar(30) not null,
  created_at timestamp(6) not null,
  constraint fk_academic_warning_student foreign key (student_id) references student (id)
);

create table graduation_audit (
  id bigint auto_increment primary key,
  student_id bigint not null,
  audit_item varchar(120) not null,
  required_value varchar(120) not null,
  current_value varchar(120) not null,
  passed boolean not null,
  remark varchar(300),
  updated_at timestamp(6) not null,
  constraint fk_graduation_audit_student foreign key (student_id) references student (id)
);

create table teaching_plan_item (
  id bigint auto_increment primary key,
  grade varchar(20) not null,
  major varchar(80) not null,
  term varchar(80) not null,
  course_code varchar(40) not null,
  course_name varchar(120) not null,
  credit integer not null,
  course_type varchar(40) not null,
  assessment_type varchar(40) not null
);

create table teaching_feedback (
  id bigint auto_increment primary key,
  student_id bigint not null,
  category varchar(40) not null,
  title varchar(120) not null,
  content varchar(1000) not null,
  status varchar(30) not null,
  reply varchar(1000),
  submitted_at timestamp(6) not null,
  replied_at timestamp(6),
  constraint fk_teaching_feedback_student foreign key (student_id) references student (id)
);

create table thesis_grade (
  id bigint auto_increment primary key,
  student_id bigint not null,
  title varchar(200) not null,
  advisor varchar(80) not null,
  proposal_score integer,
  midterm_score integer,
  defense_score integer,
  final_score integer,
  grade_level varchar(20),
  status varchar(40) not null,
  updated_at timestamp(6) not null,
  constraint fk_thesis_grade_student foreign key (student_id) references student (id)
);

create index idx_teaching_plan_major_grade on teaching_plan_item (major, grade, term);
create index idx_teaching_feedback_student on teaching_feedback (student_id, submitted_at);
