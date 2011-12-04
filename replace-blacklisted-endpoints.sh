#!/bin/bash

source config

./replace-endpoint.sh 'http://160.45.137.69:8890/sparql' 'http://example.org/sparql'
./replace-endpoint.sh 'http://160.45.137.73:8890/sparql' 'http://example.org/sparql'

