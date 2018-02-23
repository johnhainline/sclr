FROM openjdk:8-jre-alpine

# Add curl, ping, etc for debugging purposes.
RUN apk --no-cache add --update bash coreutils curl

ADD entrypoint.sh /tmp/entrypoint.sh
ENTRYPOINT ["/tmp/entrypoint.sh"]
