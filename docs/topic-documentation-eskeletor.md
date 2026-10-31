Based on the provided content, here is a structured template for Kafka Topic Documentation. This skeleton can be reused for any topic within the system while maintaining consistent formatting and documentation standards.

***

# Kafka Topic Documentation: {{Topic Name}}

## Revision History
| Date | User | Reason for Change | Description of Change |
| --- | --- | --- | --- |
| [YYYY-MM-DD] | [User Name/ID] | Initial Draft / Update [Version #] | [Brief description of the changes made] |
| | | | |

---

## 1. Overview
**Description:**
[Provide a clear, concise summary of what this topic is used for and which business process it supports.]

**Avro Namespace:** `[e.g., com.umdc.mercury.kafka.to]`
**Record Name:** `[e.g., Message]`

---

## 2. Schema Definition (Avro)
The following table defines the fields contained within the Avro record for this topic:

| Field Name | Type | Description | Example / Constraint |
| --- | --- | --- | --- |
| `field_name_1` | [string/int/record] | Detailed description of the field's purpose. | "Example value" or "[Constraint]" |
| `field_name_2` | ... | ... | ... |

---

## 3. Nested Structure: [Nested Object Name]
*Only include this section if the schema contains nested records.*

The `[field_name]` field is a complex record used to provide [description of nested logic].

| Field Name | Type | Description | Example |
| --- | --- | --- | --- |
| `sub_field_1` | string | Description of sub-field. | "Example" |
| `sub_field_2` | ... | ... | ... |

---

## 4. Sample Payload (JSON Representation)
The following shows an example of the data payload as it would be serialized/deserialized for this topic:

```json
{
  "field1": "value",
  "field2": "value",
  "nested_object": {
    "sub_field1": "value",
    "sub_field2": "value"
  },
  "field3": "value"
}
```

---

## 5. AVRO Schema
The following section contains the raw Avro schema definition for technical reference:

```json
{
  "type": "record",
  "namespace": "...",
  "name": "...",
  "fields": [
    {
      "name": "field1",
      "type": "string"
    },
    {
      "name": "nested_object",
      "type": {
        "type": "record",
        "name": "NestedType",
        "fields": [
          {
            "name": "sub_field1",
            "type": "string"
          }
        ]
      }
    }
  ]
}
```
