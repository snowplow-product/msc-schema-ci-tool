# Data Structures CI
[![Build Status][gh-actions-image]][gh-actions]
[![Download][gh-release-image]][gh-release]
[![License][license-image]][license]

Data Structures CI is a command-line tool which allows you to integrate Data Structures API (formerly Schema API) 
into your CI/CD pipelines.
Currently, it supports one common task:

* Verifying that all schema dependencies for a project are already deployed into an environment (e.g. "DEV", "PROD")

## Prerequisites

- JRE 8 or above

## User Quickstart

You can download data-structures-ci using the following command:

```
curl -L "https://github.com/snowplow-product/msc-schema-ci-tool/releases/download/1.0.0/data_structures_ci_1.0.0.zip" | jar xv 
```

In order to be able to perform any task, you will need to be a customer and supply both your Organization ID and 
an API key, which you can generate from https://console.snowplowanalytics.com/credentials.
Those two should be provided as env vars.

## Usage

### Check Deployments

This command allows to verify that all schema dependencies for a project (declared in a specific "manifest") 
are already deployed into an environment (e.g. "DEV", "PROD")

Syntax: 
```bash
$ export ORGANIZATION_ID=<organization-id>
$ export API_KEY=<api-key>
$ ./data-structures-ci check \
    --manifestPath /path/to/manifest/snowplow-schemas.json \
    --environment DEV
```

The manifest must adhere to the following Self Describing JSON Schema, published to Iglu Central:
http://iglucentral.com/schemas/com.snowplowanalytics.insights/data_structures_dependencies/jsonschema/1-0-0

An example of how a manifest looks like:
```yaml
{
  "schema": "iglu:com.snowplowanalytics.insights/data_structures_dependencies/jsonschema/1-0-0",
  "data": {
    "schemas": [
      {
        "vendor": "com.snplow.msc.ci",
        "name": "example_event",
        "format": "jsonschema",
        "version": "1-0-0"
      }
    ]
  }
}
```

[gh-actions-image]: https://github.com/snowplow-product/msc-schema-ci-tool/workflows/ci/badge.svg?branch=master
[gh-actions]: https://github.com/snowplow-product/msc-schema-ci-tool/actions?query=workflow%3Aci

[gh-release-image]: https://img.shields.io/github/downloads/snowplow-product/msc-schema-ci-tool/total
[gh-release]: https://github.com/snowplow-product/msc-schema-ci-tool/releases/download/1.0.0/data_structures_ci_1.0.0.zip

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0
