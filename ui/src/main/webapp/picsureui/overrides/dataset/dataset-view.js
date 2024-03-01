define([ "jquery" ], function($) {
    return {
        mappers: function(map){
            return {
                ...map,
                uuid: {
                    path: ['query', 'resourceResultId'],
                    renderId: "detail-summary-id",
                    render: function(caUUID){
                        return caUUID;
                    }
                }
            };
        },
        renderExt: function(){
            $("#dataset-view-cancel-btn").removeClass("secondary");
        }
    };
});