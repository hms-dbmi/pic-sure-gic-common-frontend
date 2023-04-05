define(['help/help-page'], function(helpPage){
    return {
        routes : {
            /**
             * Additional routes for the backbone router can be defined here. The field name should be the path,
             * and the value should be a function.
             *
             * Ex:
             * "picsureui/queryBuilder2" : function() { renderQueryBuilder2(); }
             */
            "picsureui/help(/)" : function() {
                $(".header-btn.active").removeClass('active');
                $('#main-content').empty();
                const helpPageTemplate = new helpPage();
                helpPageTemplate.render();
                $('#main-content').append(helpPageTemplate.$el);
                $("#help-page-btn").addClass('active');
            }
        },
        /**
         * Allows the default action to be overriden. If this is not set, the query builder will be shown for any
         * route that does not have a defined action
         */
        defaultAction: undefined
    };
});