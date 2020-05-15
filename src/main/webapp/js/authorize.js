window.mitre = window.mitre || {};
window.mitre.fhirreferenceserver = window.mitre.fhirreferenceserver || {};
window.mitre.fhirreferenceserver.authorize = {

    /**
     * initializes the page and all html components including actions
     */
    init: function () {

        //static code that the HAPI interceptor will look for to return token
        let urlParams = new URLSearchParams(window.location.search);

        let state = urlParams.get('state') || '';

        let clientId = urlParams.get('client_id') || '';

        //static code that the HAPI interceptor will look for to return token
        let sampleCode = "SAMPLE_CODE";

        let scopes = urlParams.get('scopes') || '';

        let scopesList = scopes.split(' ');
        //http://localhost:8080/hapi-fhir-jpaserver/oauth/authorization?response_type=code&client_id=&redirect_uri=http%3A%2F%2Flocalhost%3A4567%2Finferno%2Foauth2%2Fstatic%2Fredirect&scope=launch%2Fpatient+patient%2F%2A.read+openid+fhirUser+offline_access&state=ddc2657d-7146-418b-8b4e-64e3f8e92eb0&aud=http%3A%2F%2Flocalhost%3A8080%2Fhapi-fhir-jpaserver%2Ffhir

        //load scopes
        let checkBoxesHtml = '';

        for (let i = 0; i < scopesList.length; i++)
        {
                let scope = scopesList[i];

                let scopeCheckboxHtml = '<div class="form-check">'
                scopeCheckboxHtml += '<input class="form-check-input" name="scopeCheckbox" type="checkbox" value="' + scope + '" checked>'
                scopeCheckboxHtml += '<label class="form-check-label" for="defaultCheck1">' + scope + '</label>'
                scopeCheckboxHtml += '</div>';

                checkBoxesHtml += scopeCheckboxHtml;
        }

        $('#scopes').append(checkBoxesHtml);



        //populate patient picker with data

 
        $('#submit').click(function(){

            //get checked scopes
            let selectedScopes = "";
            $('#scopes [name="scopeCheckbox"]:checked').each(function(index, checkbox) {
                selectedScopes += checkbox.value + " ";
            });

            let patientId = urlParams.get('patient_id');
            console.log("patientId is :" + patientId);
            let base64URLEncodedPatientId = btoa(patientId);

            // base64 encoding that is escaped so it can be used in a url
            let base64URLEncodedScopes = btoa(selectedScopes);

            let code = sampleCode + "." + base64URLEncodedScopes + "." + base64URLEncodedPatientId;
            console.log("code is " + code);

            let redirect = urlParams.get('redirect_uri') + '?code=' + code + '&' + 'state=' + state;

            window.location.href = redirect;
        });
            
            

    }

}

