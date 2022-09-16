#!/usr/bin/env bash
docker run  --rm -v /var/run/docker.sock:/var/run/docker.sock \
    -v $(pwd)/compose.yml:/etc/compose.yml \
    docker/compose:1.23.2 \
    -p sqool-frontend \
    -f /etc/compose.yml up \
    --force-recreate