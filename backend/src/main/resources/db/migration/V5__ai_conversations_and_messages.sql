-- Additive QamqorMed AI history. Only the authenticated owner can access rows through the API.
CREATE TABLE IF NOT EXISTS ai_conversations (
    id bigserial PRIMARY KEY,
    owner_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    owner_role varchar(20) NOT NULL,
    title varchar(120) NOT NULL DEFAULT 'QamqorMed AI',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ai_conversations_owner_role_check CHECK (owner_role IN ('PATIENT', 'DOCTOR', 'NURSE', 'ADMIN'))
);
CREATE INDEX IF NOT EXISTS idx_ai_conversations_owner_updated
    ON ai_conversations(owner_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS ai_messages (
    id bigserial PRIMARY KEY,
    conversation_id bigint NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    message_type varchar(20) NOT NULL,
    content text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ai_messages_type_check CHECK (message_type IN ('USER', 'ASSISTANT'))
);
CREATE INDEX IF NOT EXISTS idx_ai_messages_conversation_created
    ON ai_messages(conversation_id, created_at ASC);
