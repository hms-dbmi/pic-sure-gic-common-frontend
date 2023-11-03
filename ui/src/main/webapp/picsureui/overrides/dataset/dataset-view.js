define([ "jquery" ], function($) {
    return {
        mappers: function(map){
            return {
                ...map,
                uuid: {
                    path: ['metadata', 'siteQueryIds'],
                    renderId: "detail-summary-id",
                    render: function(siteList){
                        return siteList.map(({ name, queryId }) => 
                            `<div id="site-${name}" class="row p-0">` +
                                `<div class="col-md-3 p-0">${name}</div>` +
                                `<div class="col-md-9 p-0">${queryId}</div>` +
                            `</div>`
                        ).join('');
                    }
                }
            };
        },
        renderExt: function(){
            $("#dataset-view-cancel-btn").removeClass("secondary");
        }
    };
});