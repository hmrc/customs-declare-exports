#!/usr/bin/env bash

sbt clean scalafmt test:scalafmt it:test::scalafmt coverage test it:test scalafmtCheckAll coverageReport
