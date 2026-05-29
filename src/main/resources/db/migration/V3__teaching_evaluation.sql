create table teaching_evaluation (
  id bigint auto_increment primary key,
  student_id bigint not null,
  offering_id bigint not null,
  teaching_score integer not null,
  content_score integer not null,
  interaction_score integer not null,
  overall_score integer not null,
  comment varchar(500),
  submitted_at timestamp(6) not null,
  constraint uk_teaching_evaluation_student_offering unique (student_id, offering_id),
  constraint fk_teaching_evaluation_student foreign key (student_id) references student (id),
  constraint fk_teaching_evaluation_offering foreign key (offering_id) references course_offering (id)
);

create index idx_teaching_evaluation_offering on teaching_evaluation (offering_id);
