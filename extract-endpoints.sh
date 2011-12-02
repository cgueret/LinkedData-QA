#!/bin/bash
saxon "$1" extract-endpoints.xslt | sort -u

