define(["jquery", "backbone", "data/dataInfo"], function($, BB, dataInfo){
	
	
	return {
		
		dataInfo:  new dataInfo.View(this.resources),
		
		renderExt: function(element){
			this.dataInfo.setElement(element);
			this.dataInfo.render();
		},
		/*
		 * The path to a logo image incase you don't want the default PrecisionLink one.
		 * 
		 * This should be a String value.
		 */
		logoPath : undefined
	};
});