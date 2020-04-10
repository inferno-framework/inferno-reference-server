window.mitre = window.mitre || {};
window.mitre.fhirreferenceserver = window.mitre.fhirreferenceserver || {};
window.mitre.fhirreferenceserver.utils = {

    /**
     * initializes the page and all html components including actions
     */
    getBaseURL : function(currentPageName)
    {
        let base_url = window.location.href;
        base_url = base_url.substring(0, base_url.indexOf(currentPageName)); //cut off current page name
        return base_url;
    }

}

