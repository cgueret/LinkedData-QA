#!/bin/bash
saxon "$1" convert-html-report-to-json.xslt | tr -d '\n' | sed -r 's/\t//g'

