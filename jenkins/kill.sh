#!/usr/bin/env bash

echo $(cat .pidFile)
kill $(cat .pidFile)