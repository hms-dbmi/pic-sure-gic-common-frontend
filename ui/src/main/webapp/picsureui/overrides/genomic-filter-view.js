define(["backbone"], function(BB){
	return {
        /*
         * Override how the genomic filters are applied and the modal is closed.
        */
        applyGenomicFilters : function(genomicFilterView, filter){
                Backbone.pubSub.trigger('update:genomicFilter', filter);
                genomicFilterView.cancelGenomicFilters();
        },

        /*
         * This methind is used to set up the tab order of elements in the genomic filter modal.
         * If you change the html elements order or add new elements, you need to update this method.
         */ 
        createTabIndex: undefined,

        /*
         * This method is used to disable the apply button in the genomic filter modal.
         * If you want to control when the user is allowed to apply the filter, you need to update this method.
         * The default behavior is to require a Gene_with_variant and
         * to disable the apply button if there is no filters selected.
         */
        updateDisabledButtons: undefined,


        // Describe the frequency to the user. Used for the tool tip
        frequencyDescription: 'The variant allele frequency in gnomAD genomes of combined population as discrete text categories. Possible values: Rare (variant frequency less than 1%), Common (variant frequency greater than or equal to 1%).',
	};
});
