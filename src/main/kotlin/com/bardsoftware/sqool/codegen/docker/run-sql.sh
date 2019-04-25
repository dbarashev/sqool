#!/usr/bin/env bash

find /workspace -type f -name "*-static.sql" -exec cat {} + | psql