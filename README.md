# ras-frontend

[![Build Status](https://travis-ci.org/hmrc/ras-frontend.svg)](https://travis-ci.org/hmrc/ras-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/ras-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/ras-frontend/_latestVersion)


This service is designed for Pension Scheme Administrators and Pension Scheme Practitioners to get residency status
of a pension scheme member.


Requirements
------------

This service is written in [Scala] and [Play], so needs at least a [JRE] to run.


Authentication
------------

This user logs into this service using the [Government Gateway]

Testing
------------

#### Unit Tests
To run the unit tests for the application run the following:

1. `cd $workspace`
2. `sbt test`

To run a single unit test/spec

1. `cd $workspace`
2. `sbt`
3. `test-only *SpecToUse*` - Example being the class name of your UnitSpec

#### Test Coverage
To run the test coverage suite `scoverage`

1. `sbt clean scoverage:test`

#### Acceptance Tests

**NOTE:** Cucumber/acceptance tests are available in a separate project at:
`https://github.com/hmrc/ras-acceptance-tests`

#### Performance Tests

**NOTE:** Performance tests are available in a separate project at:
`https://github.com/hmrc/ras-performance-tests`

Running Locally
------------

Install [Service Manager](https://github.com/hmrc/service-manager), then start dependencies:

    sm --start RAS_ALL -f

Start the app:

    sbt "run 9673"
    
Endpoint URL:

    /relief-at-source 

Acronyms
--------

In the context of this service we use the following acronyms:

* [API]: Application Programming Interface

* [HoD]: Head of Duty

* [JRE]: Java Runtime Environment

* [JSON]: JavaScript Object Notation

* [NINO]: National Insurance number

* [PSA]: Pension Scheme Administrator

* PSP: Pension Scheme Practitioner

* [NPS]: National Insurance and PAYE System

* [URL]: Uniform Resource Locator

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")

[Scala]: http://www.scala-lang.org/
[Play]: http://playframework.com/
[JRE]: http://www.oracle.com/technetwork/java/javase/overview/index.html

[Government Gateway]: http://www.gateway.gov.uk/

[API]: https://en.wikipedia.org/wiki/Application_programming_interface
[HoD]: http://webarchive.nationalarchives.gov.uk/+/http://www.hmrc.gov.uk/manuals/sam/samglossary/samgloss249.htm
[JSON]: http://json.org/
[NINO]:https://www.gov.uk/national-insurance/your-national-insurance-number
[PSA]: https://www.gov.uk/topic/business-tax/pension-scheme-administration
[NPS]: http://www.publications.parliament.uk/pa/cm201012/cmselect/cmtreasy/731/73107.htm
[URL]: https://en.wikipedia.org/wiki/Uniform_Resource_Locator
