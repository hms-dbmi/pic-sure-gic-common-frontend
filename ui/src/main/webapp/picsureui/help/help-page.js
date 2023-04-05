define([
    'backbone',
    'handlebars',
    'text!help/help-page.hbs',
], function(BB, HBS, helpPageTemplate) {
    const HelpPage = BB.View.extend({
        initialize: function(){
            this.helpPage = HBS.compile(helpPageTemplate);
            this.privileges = [];
            const session = sessionStorage.getItem("session");
            if (session) {
                const currentSession = JSON.parse(session);
                this.privileges = currentSession.privileges;
            }
        },
        events: {},
        render: function(){
            this.$el.html(this.helpPage(this));
        }
    });
    return HelpPage;
});
