CREATE TABLE results (
  id                BIGINT NOT NULL AUTO_INCREMENT,
  data              VARCHAR(255) NOT NULL,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY       (id)
);
