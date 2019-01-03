# SQooL Frontend Server database schema
FROM busybox
ADD build/docker/workspace/ /workspace
VOLUME /workspace
CMD /bin/true
