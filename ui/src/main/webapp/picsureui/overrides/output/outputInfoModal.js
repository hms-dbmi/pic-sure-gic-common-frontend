define(["backbone", "handlebars", 'text!overrides/output/outputInfoModal.hbs'],
		function(BB, HBS, template){
	let view = BB.View.extend({
        initialize: function(opts){
            this.template = HBS.compile(template);
            this.data = opts.data;
            this.columns = opts.columns;
        },
        render() {
            this.$el.html(this.template(this.opts));
            $('#output-modal-table').DataTable({
                data: this.data,
                columns: this.columns,
                ordering: false,
                searching: false,
                responsive: true,
                paging: false,
                info: false,
                columnDefs: [
                    {
                        targets: '_all',
                        className: 'center-vert dt-center',
                        defaultContent: '-',
                        type: 'string'
                    }
                ]
            });
        }
    });
    return view;
});