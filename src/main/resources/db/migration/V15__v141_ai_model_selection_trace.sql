alter table ai_call_log
  add column selected_model_name varchar(180);

alter table ai_call_log
  add column actual_model_name varchar(180);

alter table ai_call_log
  add column fallback_reason varchar(500);

update ai_call_log
set selected_model_name = coalesce(selected_model_name, model_name),
    actual_model_name = coalesce(actual_model_name, model_name)
where selected_model_name is null
   or actual_model_name is null;

create index idx_ai_call_log_selected_model on ai_call_log (selected_model_name, created_at);
