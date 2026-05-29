create table sys_role_menu (
  role_id bigint not null,
  menu_id bigint not null,
  primary key (role_id, menu_id),
  constraint fk_sys_role_menu_role foreign key (role_id) references sys_role (id),
  constraint fk_sys_role_menu_menu foreign key (menu_id) references sys_menu (id)
);
