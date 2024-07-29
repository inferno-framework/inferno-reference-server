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

    let scopesData;
    $.ajax({
            async: false,
            dataType: "json",
            data: JSON.stringify(scopes),
            processData: false,
            contentType: 'application/json',
            method: "POST",
            url: '/reference-server/oauth/supportedScopes',
            success: function(data) {
              scopesData = data;
            }
          });

    // load scopes
    let checkBoxesHtml = '';

    const originallyHasV2 = scopesData.some(s => !s.v1); 
    // the v2 field will always be populated, so we distinguish an "originally v2" scope if its v1 field is null

    if (!originallyHasV2) {
      // if none of the scopes are v2 then show the notice: v1 scopes may be converted to v2
      $('#v1scopesupgradenotice').show();
    }

    // if there is an originally v2 scope or subscope selected, show all scopes as v2
    // otherwise show scopes as v1 || v2
    const createCheckbox = (v1, v2, index, subscope=false) => {
      let scopeId = "scope-" + index;
      let value = originallyHasV2 ? v2 : v1 || v2;
      return (
        `<div class="form-check">
           <input class="form-check-input ${subscope ? 'subscope' : 'main-scope'}" id="${scopeId}" name="scopeCheckbox" 
                  type="checkbox" value="${value}" data-v1="${v1 || ""}" data-v2="${v2}" ${subscope ? 'disabled' : 'checked'}>
           <label class="form-check-label" for="${scopeId}">${value}</label>
         </div>`
      );
    }

    for (let i = 0; i < scopesData.length; i++) {
      let scope = scopesData[i];
      checkBoxesHtml += createCheckbox(scope.v1, scope.v2, i);

      for (let j = 0; j < scope.subscopes.length; j++) {
        const subscope = scope.subscopes[j];
        checkBoxesHtml += createCheckbox(null, subscope, i + "-" + j, true);
      }
    }

    $('#scopes').append(checkBoxesHtml);

    $('.main-scope').change(function(e) {
      const selector = `[id^="${e.target.id}-"]`; // selector to find ids starting with "{this.id}-"
      if (e.target.checked) {
        // the main scope was checked, so uncheck and disable all its subscopes
        $(selector).prop( "checked", false );
        $(selector).prop( "disabled", true );
      } else {
        // the main scope was unchecked, so enable all the subscopes
        $(selector).prop( "disabled", false );
      }
    });

    $('#scopes [name="scopeCheckbox"]').change(function(e) {
      // when we toggle a checkbox, if it's enabling a v2 scope then all scopes need to be converted to v2
      // if it's disabling a v2 scope, we may want to revert to the original requested
      const allCheckboxes = $('#scopes [name="scopeCheckbox"]').toArray();
      const isAnyV2Selected = allCheckboxes.some(c => c.checked && !c.dataset.v1);
      let valueSelector;
      // same logic as in createCheckbox above
      if (isAnyV2Selected) {
        valueSelector = c => c.dataset.v2;
      } else {
        valueSelector = c => c.dataset.v1 || c.dataset.v2;
      }

      allCheckboxes.forEach(c => {
        c.value = valueSelector(c);
        $(`label[for="${c.id}"]`).html(c.value);
      });
    });

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
