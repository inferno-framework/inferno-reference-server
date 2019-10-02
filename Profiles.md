# Profiles

When you post a resource to the FHIR API, it will by default be validated against the base profile for that resource.

So let's assume I post the following Patient record:

    {
      "resourceType": "Patient",
      "name": [
        {
          "family": "Adams",
          "given": ["John", "Quincy"]
        },
        {
          "family": "Adams",
          "given": ["Alternative", "Name"]
        }
      ]
    }

When this is POSTed to http://localhost:8080/r4/Patient it will validate against the default Patient profile at `http://hl7.org/fhir/StructureDefinition/Patient` and create a patient resource.

It's the same as sending this patient (the profile is implied):

    {
      "resourceType": "Patient",
      "meta": {
        "profile": "http://hl7.org/fhir/StructureDefinition/Patient"
      },
      "name": [
        {
          "family": "Adams",
          "given": ["John", "Quincy"]
        },
        {
          "family": "Adams",
          "given": ["Alternative", "Name"]
        }
      ]
    }

## Creating Custom Profiles

One of the main features of FHIR is that you can create your own profiles in the form of StrucutreDefinition objects.

Let's say that we want to extend the Patient profile by creating a MitrePatient profile. The default Patient profile allows for a patient to have 0 to many names. In the above example, the patient has 2 names. In our MitrePatient profile, we want to ensure that each patient only has exactly one name.

First we need to craft a MitrePatient StructureDefinition that limits the name field.

This might look something like this. Note that this is truncated for brevity in this documentation.

    {
      "resourceType": "StructureDefinition",
      "url": "StructureDefinition/MitrePatient",
      "id": "MitrePatient",
      "name": "MitrePatient",
      "type": "Patient",
      "kind": "resource",
      "derivation": "constraint",
      "status": "draft",
      "date": "2019-04-20T00:00:00-04:00",
      "publisher": "The MITRE Corporation",
      "abstract": false,
      "baseDefinition": "http://hl7.org/fhir/StructureDefinition/Patient",
      "snapshot": {
        ...
      },
      "differential": {
        "element": [
          {
            "id": "Patient.name",
            "path": "Patient.name",
            "short": "A name associated with the patient",
            "definition": "A name associated with the individual.",
            "min": 1,
            "max": "1",
            ...
          }
        ]
      }
    }

Now we can `PUT` this StructureDefinition to our server at `http://localhost:8080/r4/StructureDefinition/MitrePatient`

A few things to notice:

1. We give this a URL of `StructureDefinition/MitrePatient`.
2. We are deriving from a baseDefinition of the default Patient profile.
3. In the `differential` section, we changed the fields that differ from the base profile.
4. In this specific case, we have changed `min` to 1 and `max` to 1 to allow only one and exactly one name.

In the real world, your StructureDefintion will not be created by hand, but should probably come from a tool like CIMPL.

## Using Custom Profiles

Now that we have a custom Patient profile loaded to our FHIR server, we likely want to use it to validate our Patient resources.

This FHIR server has been configured to validate against local StructureDefinitions that have been loaded. What does that mean? When you load a resource, you can specify the url to any local StructureDefinition in the `profile` field and the server will validate against that profile instead of the default profile.

Here's an example Patient resource:

    {
      "resourceType": "Patient",
      "meta": {
        "profile": "StructureDefinition/MitrePatient"
      },
      "name": [
        {
          "family": "Adams",
          "given": ["John", "Quincy"]
        },
        {
          "family": "Adams",
          "given": ["Alternative", "Name"]
        }
      ]
    }

Notice the `profile` matches the `url` of the custom StructureDefinition that we created above.

When we try to POST this patient to our FHIR server, we get the following error:

    {
      "resourceType": "OperationOutcome",
      "issue": [
          {
              "severity": "error",
              "code": "processing",
              "diagnostics": "Profile StructureDefinition/MitrePatient, Element 'Patient.name': max allowed = 1, but found 2",
              "location": [
                  "Patient"
              ]
          }
      ]
    }

That's because it's validating against our custom `StructureDefinition/MitrePatient` profile which only allows one name.

