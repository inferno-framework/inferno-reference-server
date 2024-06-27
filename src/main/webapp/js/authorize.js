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
    const appLaunchUrlLink = '<a class="text-white" href="' + appLaunchUrl + '">' + appLaunchUrl + '</a>';

    let patientId = urlParams.get("patient_id") || "";
    let encounterId = urlParams.get("encounter_id") || "";

    if (aud !== expectedAud)
    {
      let htmlSafeAud = $('<span class="font-weight-bold" />').text(aud)[0].outerHTML;
      const launchAudError = "<div>The Audience value " + htmlSafeAud + " is invalid. If you are attempting to simulate an EHR launch, please enter the appropriate launch URI into the form at " + appLaunchUrlLink + ".</div>";
      window.mitre.fhirreferenceserver.authorize.showErrorMessage(launchAudError);
      return;
    }

    if (urlParams.has('launch'))
    {
      let launch = urlParams.get('launch');

      let expectedPatientLaunch = [];
      let expectedEncounterLaunch = [];

      if (launch.includes(" ")) {
        const launchArray = launch.split(" ");
        patientId = launchArray[0];
        encounterId = launchArray[1];

        const url = '/reference-server/app/ehr-launch-context-options';
        $.ajax({
          async: false,
          dataType: "json",
          url: url,
          success: function(data) {
            expectedPatientLaunch = Object.keys(data);
            expectedEncounterLaunch = Object.values(data)[expectedPatientLaunch.indexOf(patientId) || 0];
          }
        });
      }

      // if launch is provided
      if (!expectedPatientLaunch.includes(patientId) || !expectedEncounterLaunch.includes(encounterId))
      {
        let htmlSafeLaunch = $('<div class="font-weight-bold" />').text(launch)[0].outerHTML;
        const launchError = "<div>The Launch value " + htmlSafeLaunch + " is invalid. If you are attempting to simulate an EHR launch, please enter the appropriate launch URI into the form at " + appLaunchUrlLink + ".</div>";
        window.mitre.fhirreferenceserver.authorize.showErrorMessage(launchError);
        return;
      }
    }

    let clientId = urlParams.get('client_id') || '';

    // check for a patient id, if no one exists redirect to patient picker
    if (!urlParams.has('patient_id') && patientId == "")
    {

      let this_uri = window.location;
      let this_url_encoded = encodeURIComponent(this_uri);
      let redirect = "../oauth/patient-picker?client_id=" + clientId + "&redirect_uri=" + this_url_encoded;
      window.location.href = redirect;
      return;
    }

    $('#banner').show();
    $('#pageContent').show();

    let state = urlParams.get('state') || '';

    const codeChallenge = urlParams.get('code_challenge');
    const codeChallengeMethod = urlParams.get('code_challenge_method') || 'plain';

    // static code that the HAPI interceptor will look for to return token
    let sampleCode = "SAMPLE_CODE";

    let scopes = urlParams.get('scope') || '';

    scopes = scopes.trim();

    let scopesList = scopes.split(' ');

    // load scopes
    let checkBoxesHtml = '';

	const createCheckbox = (scope, index, subscope=false) => {
	  let scopeId = "scope-" + index;
      return (
          `<div class="form-check">
             <input class="form-check-input" id="${scopeId}" name="scopeCheckbox" type="checkbox" value="${scope}" ${!subscope && 'checked'}>
             <label class="form-check-label" for="${scopeId}">${scope}</label>
           </div>`
      );
	}
	
	const getSubscopes = scope => {
		const knownParams = {
			'Condition': [
				'category=http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern',
				'category=http://terminology.hl7.org/CodeSystem/condition-category|encounter-diagnosis',
				'category=http://terminology.hl7.org/CodeSystem/condition-category|problem-list-item'
			],
			'Observation': [
				'category=http://hl7.org/fhir/us/core/CodeSystem/us-core-category|sdoh',
				'category=http://terminology.hl7.org/CodeSystem-observation-category|social-history',
				'category=http://terminology.hl7.org/CodeSystem/observation-category|laboratory',
				'category=http://terminology.hl7.org/CodeSystem/observation-category|survey',
				'category=http://terminology.hl7.org/CodeSystem/observation-category|vital-signs'
			],
			'DocumentReference': [
				'category=http://hl7.org/fhir/us/core/CodeSystem/us-core-documentreference-category|clinical-note'
			]
		}
		
		// syntax: 
		const slash = scope.indexOf('/');
		const dot = scope.indexOf('.', slash);
		
		if (slash < 0 || dot < 0) {
			// not a SMART clinical scope
			return [];
		}
		
		const resourceType = scope.slice(slash + 1, dot);
		
		const paramsForResourceType = knownParams[resourceType];
		if (!paramsForResourceType) {
			// no params for the requested resource type
			return [];
		}
		
		return paramsForResourceType.map(p => `${scope}?${p}`);
	}

    for (let i = 0; i < scopesList.length; i++)
    {
      let scope = scopesList[i];

      if (scope === '')
      {
        continue;
      }

      checkBoxesHtml += createCheckbox(scope, i);
      
      const subscopes = getSubscopes(scope);
      for (let j = 0; j < subscopes.length; j++) {
		const subscope = subscopes[j];
		checkBoxesHtml += createCheckbox(subscope, i + "-" + j, true);
	  }
    }

    $('#scopes').append(checkBoxesHtml);

    $('#submit').click(function(){

      // get checked scopes
      let selectedScopes = "";
      $('#scopes [name="scopeCheckbox"]:checked').each(function(index, checkbox) {
        selectedScopes += `${checkbox.value} `;
      });

      const codeParams = Object.fromEntries(Object.entries({
        code: sampleCode,
        scopes: selectedScopes,
        patientId,
        encounterId,
        codeChallenge,
        codeChallengeMethod
      }).filter(([_, v]) => v));

      const code = btoa(JSON.stringify(codeParams));

      let redirect = urlParams.get('redirect_uri') + '?code=' + code + '&' + 'state=' + state;

      window.location.href = redirect;
    });
  },

  showErrorMessage(errorMessage)
  {
    $('#banner').show();
    $('#errorMessage').html(errorMessage).show();
  }
};
