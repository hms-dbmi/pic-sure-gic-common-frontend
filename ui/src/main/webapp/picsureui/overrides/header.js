define(["jquery", "backbone", "data/dataInfo"], function($, BB, dataInfo){
	
	
	return {
		
		dataInfo:  new dataInfo.View(this.resources),
		
		render: function(){
			dataInfo.setElement($("#data-info-btn"));
			dataInfo.render();
		},
		/*
		 * The path to a logo image incase you don't want the default PrecisionLink one.
		 * 
		 * This should be a String value.
		 */
		logoPath : undefined
	};
});