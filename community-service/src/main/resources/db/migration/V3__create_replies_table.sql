CREATE TABLE replies (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                         author VARCHAR(100) NOT NULL,
                         verified_helper BOOLEAN NOT NULL DEFAULT FALSE,
                         text TEXT NOT NULL,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_replies_post_id ON replies(post_id);