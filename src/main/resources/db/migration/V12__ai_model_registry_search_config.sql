create table ai_model_registry (
  id bigint auto_increment primary key,
  name varchar(160) not null,
  provider varchar(60) not null,
  model_name varchar(180) not null,
  base_url varchar(260),
  api_key_ref varchar(120),
  model_type varchar(40) not null,
  purpose varchar(240),
  enabled boolean not null,
  is_default boolean not null,
  description varchar(500),
  last_status varchar(40),
  last_latency_ms bigint,
  last_error varchar(500),
  last_checked_at timestamp(6),
  created_at timestamp(6) not null,
  updated_at timestamp(6) not null,
  constraint uk_ai_model_registry_name unique (name)
);

create table ai_search_config (
  id bigint primary key,
  enabled boolean not null,
  provider varchar(60) not null,
  base_url varchar(260),
  api_key_env varchar(120),
  allowed_scenes varchar(240),
  safety_policy varchar(500),
  last_status varchar(40),
  last_latency_ms bigint,
  last_error varchar(500),
  last_tested_at timestamp(6),
  updated_at timestamp(6) not null
);

create table ai_search_result_log (
  id bigint auto_increment primary key,
  username varchar(64) not null,
  scene varchar(80) not null,
  query_text varchar(500) not null,
  provider varchar(60) not null,
  result_count integer not null,
  blocked boolean not null,
  block_reason varchar(240),
  created_at timestamp(6) not null
);

create index idx_ai_model_registry_type_default on ai_model_registry (model_type, enabled, is_default);
create index idx_ai_search_result_log_time on ai_search_result_log (created_at, scene);

insert into ai_model_registry
  (name, provider, model_name, base_url, api_key_ref, model_type, purpose, enabled, is_default, description, created_at, updated_at)
values
  ('Qwen3 8B Chat', 'OLLAMA', 'qwen3:8b', 'http://localhost:11434', 'OLLAMA_API_KEY', 'CHAT', '通用聊天、答辩问答、系统介绍', true, true, '默认聊天模型预设，可通过 OLLAMA_CHAT_MODEL 调整 ai-service 实际模型。', current_timestamp, current_timestamp),
  ('Qwen3 8B RAG', 'OLLAMA', 'qwen3:8b', 'http://localhost:11434', 'OLLAMA_API_KEY', 'RAG', '智能教务助手 RAG 综合回答', true, true, '默认教务问答模型预设。', current_timestamp, current_timestamp),
  ('Qwen2.5 Coder SQL', 'OLLAMA', 'qwen2.5-coder:7b', 'http://localhost:11434', 'OLLAMA_API_KEY', 'SQL', '自然语言只读查库 SQL 生成', true, true, '默认 SQL 生成模型预设。', current_timestamp, current_timestamp),
  ('Qwythos-9B-Claude-Mythos-5-1M', 'OLLAMA', 'hf.co/empero-ai/Qwythos-9B-Claude-Mythos-5-1M-GGUF:Q4_K_M', 'http://localhost:11434', 'OLLAMA_API_KEY', 'CHAT', '长上下文聊天、文档分析备选模型', false, false, 'Hugging Face 原始模型 empero-ai/Qwythos-9B-Claude-Mythos-5-1M；Ollama/GGUF 预设 hf.co/empero-ai/Qwythos-9B-Claude-Mythos-5-1M-GGUF:Q4_K_M。', current_timestamp, current_timestamp);

insert into ai_search_config
  (id, enabled, provider, base_url, api_key_env, allowed_scenes, safety_policy, updated_at)
values
  (1, false, 'LOCAL_DEMO', '', '', 'CHAT,ASSISTANT,TECHNICAL', '禁止个人数据、成绩、SQL、密码、密钥等敏感问题自动联网搜索；所有搜索操作写入审计和搜索日志。', current_timestamp);

insert into sys_menu (code, title, path, icon, parent_code, sort_order)
values ('admin-ai-models', 'AI模型与联网搜索', '/admin/ai-models', 'BrainCircuit', 'admin', 82);

insert into sys_role_menu (role_id, menu_id)
select r.id, m.id
from sys_role r, sys_menu m
where r.code = 'ADMIN'
  and m.code = 'admin-ai-models';
