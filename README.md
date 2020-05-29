# Data Structures CI
[![Build Status][travis-image]][travis]
[![Binary Download][bintray-image]][bintray]
[![License][license-image]][license]

Data Structures CI is a command-line tool which allows you to integrate Data Structures API (formerly Schema API) 
into your CI/CD pipelines.
Currently, it supports one common task:

* Verifying that all schema dependencies for a project are already deployed into an environment (e.g. "DEV", "PROD")

## Prerequisites

- JRE 8 or above

## User Quickstart

You can download schema-ci from our Bintray repository, using the following command:

```
curl -L "http://dl.bintray.com/snowplow/snowplow-generic/data-structures-ci-0.3.0.zip" | jar xv
```

In order to be able to perform any task, you need to supply credentials of a user which you will use for CI purposes.
These credentials come in form of a username and a password which can be obtained by creating an __admin__
user for your organization from the dedicated Snowplow Insights Console page.

## Usage

### Check Deployments

This command allows to verify that all schema dependencies for a project (declared in a specific "manifest") 
are already deployed into an environment (e.g. "DEV", "PROD")

Syntax: 
```bash
$ ./data-structures-ci check \
    --manifestPath /path/to/manifest/snowplow-schemas.json \
    --username <username> \
    --password <password> \
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
        "vendor": "com.snplow.msc.gcp",
        "name": "example_event",
        "format": "jsonschema",
        "version": "1-0-7"
      }
    ]
  }
}
```

## Development

If you are developing new commands or if you just want to test this tool against Next, just make sure to set these 
environment variables before starting the tool:

```bash
export API_BASE_URL=https://next.console.snowplowanalytics.com
export AUTH_SERVER_BASE_URL=https://snowplow-next.eu.auth0.com
export AUTH_CLIENT_ID='<Next client id>'
export AUTH_CLIENT_SECRET='<Next client secret>'
```

[travis-image]: https://travis-ci.com/snowplow-product/msc-schema-ci-tool.svg?token=F4Ce9m1YA8HqgpFQMcL5&branch=master
[travis]: https://travis-ci.com/snowplow-product/msc-schema-ci-tool

[bintray-image]: https://api.bintray.com/packages/snowplow/snowplow-generic/schema-ci/images/download.svg
[bintray]: https://bintray.com/snowplow/snowplow-generic/schema-ci/_latestVersion

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0