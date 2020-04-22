# Schema CI
[![Build Status][travis-image]][travis]
[![Binary Download][bintray-image]][bintray]
[![License][license-image]][license]

Schema CI is a command-line tool which allows you to integrate Schema API into your CI/CD pipelines.  
Currently, it supports one common task:

* Verifying that all schema dependencies for a project are already deployed into an environment (e.g. "DEV", "PROD")

## Prerequisites

- JRE 8 or above

## User Quickstart

You can download schema-ci from our Bintray repository, using the following command:

```
curl -L "http://dl.bintray.com/snowplow/snowplow-generic/schema-ci-0.1.0.zip" | jar xv
```

In order to be able to perform any task, you need to supply an organization id (UUID), 
a username and a password.
You should be able to obtain them from Insights UI:
- The organization ID can be extracted from the Insights page URL
- The username and password can be obtained by creating an admin user for your organization that you will use for CI
  purposes

## Usage

### Check Deployments

This command allow to verify that all schema dependencies for a project (declared in a specific "manifest") 
are already deployed into an environment (e.g. "DEV", "PROD")

Syntax: 
```bash
$ ./schema-ci check \
    --manifestPath /path/to/manifest/snowplow-schemas.json \
    --organizationId <organization-id> \
    --username <username> \
    --password <password> \
    --environment DEV
```

The manifest must adhere to the following Self Describing JSON Schema: 
```yaml
{
  "$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
  "description": "Schema for recording schema dependencies",
  "self": {
    "vendor": "com.snowplowanalytics.schemaci",
    "name": "schema_dependencies",
    "format": "jsonschema",
    "version": "1-0-0"
  },
  "type": "object",
  "properties": {
    "schemas": {
      "type": "array",
      "description": "An array of schema dependencies",
      "items": {
        "type": "object",
        "properties": {
          "vendor": {
            "type": "string",
            "pattern": "^[a-zA-Z0-9-_.]+$"
          },
          "name": {
            "type": "string",
            "pattern": "^[a-zA-Z0-9-_]+$"
          },
          "format": {
            "type": "string",
            "pattern": "^[a-zA-Z0-9-_]+$"
          },
          "version": {
            "type": "string",
            "pattern": "^[0-9]+-[0-9]+-[0-9]+$"
          }
        },
        "required": ["vendor", "name", "format", "version"],
        "additionalProperties": false
      }
    }
  },
  "additionalProperties": false,
  "required": ["schemas"]
}
```

An example of how a manifest looks like:
```yaml
{
  "schema": "iglu:com.snowplowanalytics.schemaci/schema_dependencies/jsonschema/1-0-0",
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

If you are developing new commands or if you just want to test this tool against Next, just make sure to set these environment
variables before starting the tool:

```bash
export API_BASE_URL=https://next.console.snowplowanalytics.com
export AUTH_SERVER_BASE_URL=https://snowplow-next.eu.auth0.com
export AUTH_CLIENT_ID=YCE5aZubvHRZ7hqF0B1XxwRR3cAApu9G
export AUTH_CLIENT_SECRET=nPbA7bAp8p_0gmClRJCHmUqO5Lv1ky6xSnElAV4oIpesaXzUf9mTcoo5uFZoUHUG
export AUTH_AUDIENCE=https://snowplowanalytics.com/api/
```

[travis-image]: https://travis-ci.com/snowplow-product/msc-schema-ci-tool.svg?token=F4Ce9m1YA8HqgpFQMcL5&branch=master
[travis]: https://travis-ci.com/snowplow-product/msc-schema-ci-tool

[bintray-image]: https://api.bintray.com/packages/snowplow/snowplow-generic/schema-ci/images/download.svg
[bintray]: https://bintray.com/snowplow/snowplow-generic/schema-ci/_latestVersion

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0