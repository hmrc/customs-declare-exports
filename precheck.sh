#!/usr/bin/env bash

sbt clean scalafmt Test/scalafmt it/Test/scalafmt coverage test it/test scalafmtCheckAll coverageReport
