window.mitre = window.mitre || {};
window.mitre.fhirreferenceserver = window.mitre.fhirreferenceserver || {};
window.mitre.fhirreferenceserver.patientPicker = {

    /**
     * initializes the page and all html components including actions
     */
    init: function () {
        let patientsTable = $('#patients').DataTable();

        //static code that the HAPI interceptor will look for to return token
        let urlParams = new URLSearchParams(window.location.search);

        let redirectUri = urlParams.get('redirect_uri');

        let clientId = urlParams.get('client_id');

        //http://localhost:8080/hapi-fhir-jpaserver/oauth/authorization?response_type=code&client_id=&redirect_uri=http%3A%2F%2Flocalhost%3A4567%2Finferno%2Foauth2%2Fstatic%2Fredirect&scope=launch%2Fpatient+patient%2F%2A.read+openid+fhirUser+offline_access&state=ddc2657d-7146-418b-8b4e-64e3f8e92eb0&aud=http%3A%2F%2Flocalhost%3A8080%2Fhapi-fhir-jpaserver%2Ffhir

        const url = 'authorizeClientId/' + clientId;
        $.get(url, function (data, status) {
            //populate patient picker with data

            let patients = data.total > 0 ? data.entry : [];
            for (let i = 0; i < patients.length; i++) {
                let patient = patients[i];

                let patientId = patient.resource.id;
                
                let buttonId = 'button-' + patientId;

                let id = patient.resource.id;
                let givenName = patient.resource.name[0].given ? patient.resource.name[0].given[0] : "";
                let familyName = patient.resource.name[0].family || "";
                let name = givenName + " " + familyName;
                name = name.trim();

                if (name.length === 0) {
                    name = "&lt; <em>Patient Name Absent</em> &gt;"
                }

                patientsTable.row.add([
                    id,
                    name,
                    '<button id="' + buttonId + '" class="btn btn-info" type="button">Select</button>'
                ]).draw(false);

                $('#' + buttonId).click(function () {
                    let redirect = redirectUri + "&patient_id=" + patientId;
                    window.location.href = redirect;
                });
            }

        }).fail(function () {
            alert('Invalid Client ID: ' + clientId);
        });
        
    }

}

