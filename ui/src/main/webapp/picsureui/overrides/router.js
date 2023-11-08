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
        tourMessages: [
            '<h4><strong class="color-tertiary">GIC</strong> Portal at a Glance:</h4>',
            '<li class="listless-li"><strong class="color-tertiary">Explore</strong> clinical, genomic, and biosample <strong>data</strong> from broadly <strong>consented</strong> cohorts, across multiple sites of care</li>',
            '<li class="listless-li"><strong class="color-tertiary">Query</strong> combined  phenotypic/genomic queries in <strong>real-time</strong></li>',
            '<li class="listless-li"><strong class="color-tertiary">Build cohorts</strong> and scout hypotheses to study <strong>prior</strong> to requesting data</li>',
            '<li class="listless-li"><strong class="color-tertiary">Request access</strong> to <strong>patient-level data</strong> by submitting the Samples & Data Request form</li>',
            '<li class="listless-li"><strong class="color-tertiary">Create & export</strong> datasets with select variables of interest to our <strong>secure</strong> research <strong>analysis</strong> platform - Service Workbench</li>',
            '<hr>',
            'During the tour, <strong>click anywhere</strong> to advance. Press <strong>escape</strong> to exit the tour at any point.'
        ],
        tourTitle: 'Welcome to the GIC',
        /**
         * Allows the default action to be overriden. If this is not set, the query builder will be shown for any
         * route that does not have a defined action
         */
        defaultAction: undefined
    };
});