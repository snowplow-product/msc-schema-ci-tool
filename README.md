# Schema CI
[![Build Status][travis-image]][travis]
[![Binary Download][bintray-image]][bintray]
[![License][license-image]][license]

Schema CI is a command-line tool which allows you to integrate Schema API into your CI/CD pipelines.  
Currently, it supports one common task:

* Verifying that all schema dependencies (declared in a specific "manifest") are already deployed into 
an environment (e.g. "DEV", "PROD")

## Prerequisites

- JRE 8 or above

## User Quickstart

You can download schema-ci from our Bintray repository, using the following command:

```
curl -L "http://dl.bintray.com/snowplow/snowplow-generic/schema-ci-0.1.0.zip" | jar xv
```

In order to be able to perform any task, you need to obtain some credentials and some details for the tool 
to be able to interact with Schema API. These credentials come in form of an organization id (uuid) and OAuth2 
client id, client secret, audience, username and password.  
Refer to [Data Workflow team on Slack](slack://channel?id=CDBKK3LCR&team=T0A28T6MN) on how to obtain them.

## CLI

### Check Deployments

This command allow to verify that all schema dependencies (declared in a specific "manifest") are already deployed into 
an environment (e.g. "DEV", "PROD")

Syntax: 
```bash
$ ./schema-ci check \
    --manifestPath /path/to/manifest/snowplow-schemas.json \
    --organizationId <organization-id> \
    --clientId <client-id> \
    --clientSecret <client-secret> \
    --audience <audience> \
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
export AUTH_SERVER_BASE_URL=https://snowplow-next.eu.auth0.com
export API_BASE_URL=https://next.console.snowplowanalytics.com
```

[travis-image]: https://travis-ci.com/snowplow-product/msc-schema-ci-tool.svg?token=F4Ce9m1YA8HqgpFQMcL5&branch=master
[travis]: https://travis-ci.com/snowplow-product/msc-schema-ci-tool

[bintray-image]: https://api.bintray.com/packages/snowplow/snowplow-generic/schema-ci/images/download.svg
[bintray]: https://bintray.com/snowplow/snowplow-generic/schema-ci/_latestVersion

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0