window.mitre = window.mitre || {};
window.mitre.fhirreferenceserver = window.mitre.fhirreferenceserver || {};
window.mitre.fhirreferenceserver.authorize = {

    /**
     * initializes the page and all html components including actions
     */
    init: function () {

        //static code that the HAPI interceptor will look for to return token
        let urlParams = new URLSearchParams(window.location.search);

        let aud = urlParams.get('aud');

        const expectedAud = window.location.origin + "/reference-server/r4"

        if (aud !== expectedAud)
        {
            alert("Audience " + aud + " is invalid"); 
            $("#pageContent").hide();
            return;
        }

        if (urlParams.has('launch'))
        {
            let launch = urlParams.get('launch');

            const expectedLaunch = "123";

            //if launch is provided
            if (launch !== expectedLaunch)
            {
                alert("Launch " + launch + " is invalid"); 
                $("#pageContent").hide();
                return;
            }
        }

        let clientId = urlParams.get('client_id') || '';

        //check for a patient id, if no one exists redirect to patient picker
        if (!urlParams.has('patient_id'))
        {
            let this_uri = window.location;
            let this_url_encoded = encodeURIComponent(this_uri);
            let redirect = "../oauth/patient-picker?client_id=" + clientId + "&redirect_uri=" + this_url_encoded;  
            window.location.href = redirect;
        }

        let state = urlParams.get('state') || '';


        //static code that the HAPI interceptor will look for to return token
        let sampleCode = "SAMPLE_CODE";

        let scopes = urlParams.get('scope') || '';
        
        scopes = scopes.trim();

        let scopesList = scopes.split(' ');

        //load scopes
        let checkBoxesHtml = '';

        for (let i = 0; i < scopesList.length; i++)
        {
            let scope = scopesList[i];

        	if (scope === '')
        	{
        		continue;
        	}        	

        	let scopeId = "scope-" + i;
            let scopeCheckboxHtml = '<div class="form-check">'
            scopeCheckboxHtml += '<input class="form-check-input" id="' + scopeId + '" name="scopeCheckbox" type="checkbox" value="' + scope + '" checked>'
            scopeCheckboxHtml += '<label class="form-check-label" for="' + scopeId + '">' + scope + '</label>'
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

