window.mitre = window.mitre || {};
window.mitre.fhirreferenceserver = window.mitre.fhirreferenceserver || {};
window.mitre.fhirreferenceserver.authorize = {

    /**
	 * initializes the page and all html components including actions
	 */
    init: function () {

        // static code that the HAPI interceptor will look for to return token
        let urlParams = new URLSearchParams(window.location.search);

        let aud = urlParams.get('aud');

        const expectedAud = window.location.origin + "/reference-server/r4";
        
        const appLaunchUrl = window.location.origin + "/reference-server/app/app-launch";
        const appLaunchUrlLink = '<a class="text-white" href="' + appLaunchUrl + '">' + appLaunchUrl + '</a>'
        
        if (aud !== expectedAud)
        {
            let htmlSafeAud = $('<span class="font-weight-bold" />').text(aud)[0].outerHTML;
            const launchAudError = "<div>The Audience value " + htmlSafeAud + " is invalid. If you are attempting to simulate an EHR launch, please enter the appropriate launch URI into the form at " + appLaunchUrlLink + ".</div>"; 
            window.mitre.fhirreferenceserver.authorize.showErrorMessage(launchAudError);
            $("#pageContent").hide();
            return;
        }

        if (urlParams.has('launch'))
        {
            let launch = urlParams.get('launch');

            const expectedLaunch = "123";

            // if launch is provided
            if (launch !== expectedLaunch)
            {
                let htmlSafeLaunch = $('<div class="font-weight-bold" />').text(launch)[0].outerHTML;
                const launchError = "<div>The Launch value " + htmlSafeLaunch + " is invalid. If you are attempting to simulate an EHR launch, please enter the appropriate launch URI into the form at " + appLaunchUrlLink + ".</div>"
                window.mitre.fhirreferenceserver.authorize.showErrorMessage(launchError);
                $("#pageContent").hide();
                return;
            }
        }

        let clientId = urlParams.get('client_id') || '';

        // check for a patient id, if no one exists redirect to patient picker
        if (!urlParams.has('patient_id'))
        {
            let this_uri = window.location;
            let this_url_encoded = encodeURIComponent(this_uri);
            let redirect = "../oauth/patient-picker?client_id=" + clientId + "&redirect_uri=" + this_url_encoded;  
            window.location.href = redirect;
        }

        let state = urlParams.get('state') || '';

        // static code that the HAPI interceptor will look for to return token
        let sampleCode = "SAMPLE_CODE";

        let scopes = urlParams.get('scope') || '';
        
        scopes = scopes.trim();

        let scopesList = scopes.split(' ');

        // load scopes
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

        $('#submit').click(function(){

            // get checked scopes
            let selectedScopes = "";
            $('#scopes [name="scopeCheckbox"]:checked').each(function(index, checkbox) {
                selectedScopes += checkbox.value + " ";
            });

            let patientId = urlParams.get('patient_id');

            let base64URLEncodedPatientId = btoa(patientId);

            // base64 encoding that is escaped so it can be used in a url
            let base64URLEncodedScopes = btoa(selectedScopes);

            let code = sampleCode + "." + base64URLEncodedScopes + "." + base64URLEncodedPatientId;

            let redirect = urlParams.get('redirect_uri') + '?code=' + code + '&' + 'state=' + state;

            window.location.href = redirect;
        });            
    },
    
    showErrorMessage(errorMessage)
    {
        $('#errorMessage').html(errorMessage).show();
    }
}

