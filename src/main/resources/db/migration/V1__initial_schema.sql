create table sys_role (
  id bigint auto_increment primary key,
  code varchar(50) not null,
  name varchar(80) not null,
  constraint uk_sys_role_code unique (code)
);

create table sys_user (
  id bigint auto_increment primary key,
  username varchar(64) not null,
  password_hash varchar(100) not null,
  display_name varchar(64) not null,
  status varchar(20) not null,
  last_login_at timestamp(6),
  constraint uk_sys_user_username unique (username)
);

create table sys_user_role (
  user_id bigint not null,
  role_id bigint not null,
  primary key (user_id, role_id),
  constraint fk_sys_user_role_user foreign key (user_id) references sys_user (id),
  constraint fk_sys_user_role_role foreign key (role_id) references sys_role (id)
);

create table sys_menu (
  id bigint auto_increment primary key,
  code varchar(80) not null,
  title varchar(80) not null,
  path varchar(120) not null,
  icon varchar(60) not null,
  parent_code varchar(80),
  sort_order integer not null,
  constraint uk_sys_menu_code unique (code)
);

create table student (
  id bigint auto_increment primary key,
  user_id bigint not null,
  student_no varchar(32) not null,
  college varchar(80) not null,
  major varchar(80) not null,
  class_name varchar(80) not null,
  grade varchar(20) not null,
  status varchar(30) not null,
  phone varchar(30),
  email varchar(120),
  address varchar(200),
  constraint uk_student_user unique (user_id),
  constraint uk_student_student_no unique (student_no),
  constraint fk_student_user foreign key (user_id) references sys_user (id)
);

create table student_status_change_application (
  id bigint auto_increment primary key,
  student_id bigint not null,
  type varchar(30) not null,
  reason varchar(500) not null,
  status varchar(30) not null,
  submitted_at timestamp(6) not null,
  reviewed_at timestamp(6),
  review_comment varchar(500),
  constraint fk_status_change_student foreign key (student_id) references student (id)
);

create table course (
  id bigint auto_increment primary key,
  code varchar(40) not null,
  name varchar(120) not null,
  credit integer not null,
  category varchar(40) not null,
  constraint uk_course_code unique (code)
);

create table course_offering (
  id bigint auto_increment primary key,
  course_id bigint not null,
  teacher_name varchar(80) not null,
  term varchar(80) not null,
  capacity integer not null,
  schedule_text varchar(120) not null,
  classroom varchar(120) not null,
  selection_start_at timestamp(6) not null,
  selection_end_at timestamp(6) not null,
  constraint fk_course_offering_course foreign key (course_id) references course (id)
);

create index idx_course_offering_term on course_offering (term);

create table course_selection (
  id bigint auto_increment primary key,
  student_id bigint not null,
  offering_id bigint not null,
  selected_at timestamp(6) not null,
  constraint uk_course_selection_student_offering unique (student_id, offering_id),
  constraint fk_course_selection_student foreign key (student_id) references student (id),
  constraint fk_course_selection_offering foreign key (offering_id) references course_offering (id)
);

create table academic_grade (
  id bigint auto_increment primary key,
  student_id bigint not null,
  course_id bigint not null,
  term varchar(80) not null,
  score integer not null,
  grade_point decimal(4, 2) not null,
  exam_type varchar(40) not null,
  constraint uk_academic_grade unique (student_id, course_id, term, exam_type),
  constraint fk_academic_grade_student foreign key (student_id) references student (id),
  constraint fk_academic_grade_course foreign key (course_id) references course (id)
);

create table exam_schedule (
  id bigint auto_increment primary key,
  course_offering_id bigint not null,
  exam_time timestamp(6) not null,
  room varchar(80) not null,
  seat_no varchar(20) not null,
  exam_type varchar(40) not null,
  status varchar(40) not null,
  constraint uk_exam_schedule_offering_time unique (course_offering_id, exam_time),
  constraint fk_exam_schedule_offering foreign key (course_offering_id) references course_offering (id)
);

create table classroom (
  id bigint auto_increment primary key,
  campus varchar(40) not null,
  building varchar(60) not null,
  room varchar(40) not null,
  capacity integer not null,
  room_type varchar(40) not null,
  available_slot varchar(40) not null,
  constraint uk_classroom_room unique (room)
);
