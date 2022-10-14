window.mitre = window.mitre || {};
window.mitre.fhirreferenceserver = window.mitre.fhirreferenceserver || {};
window.mitre.fhirreferenceserver.appLaunch = {

    /**
     * Initializes the page and all html components including actions
     */
     init: function () {

        const url = 'ehr-launch-context-options'
        $.getJSON(url, function (data, status) {
            let patientIds = Object.keys(data);
            let encounterIds = Object.values(data);

            $.each(patientIds, function(key, value) {
                $('#patientSelector')
                    .append($('<option>', { value : value })
                    .text(value));
            });

            $.each(encounterIds[0], function(key, value) {
                $('#encounterSelector')
                    .append($('<option>', { value : value })
                    .text(value));
            });

            $('#patientSelector').change( function() {
                var index = $(this).find('option:selected').index();
                $('#encounterSelector').empty();

                 $.each(encounterIds[index], function(key, value) {
                     $('#encounterSelector')
                        .append($('<option>', { value : value })
                        .text(value));
                 });
            });

            $('#launchAppButton').click(function () {
                const url = '/reference-server/app/fhir-server-path'
                $.get(url, function (data, status) {
                    let issParam = encodeURIComponent(data);
                    let launchParam = $('#patientSelector').val() + "+" + $('#encounterSelector').val();
                    let appURI = $('#appURI').val();
                    let launchLinkHref = appURI + '?' + 'launch=' + launchParam + '&' + 'iss=' + issParam;

                    window.location.href = launchLinkHref;

                }).fail(function () {
                    alert("Error getting ISS");
                });
            });
        }).fail(function () {
            alert('Unable to retrieve options for EHR Launch Context.');
        });
     }
}