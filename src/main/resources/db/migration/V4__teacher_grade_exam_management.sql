alter table academic_grade add column grade_status varchar(30) not null default 'PUBLISHED';
alter table academic_grade add column locked boolean not null default false;
alter table exam_schedule add column invigilator varchar(80);
