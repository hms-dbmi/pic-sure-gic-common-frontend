define(["jquery", "backbone", "data/dataInfo"], function($, BB, dataInfo){
	
	dataInfo = new dataInfo.View(this.resources);
	dataInfo.setElement($("#data-info-btn"));
	dataInfo.render();
	
	return {
		/*
		 * The path to a logo image incase you don't want the default PrecisionLink one.
		 * 
		 * This should be a String value.
		 */
		logoPath : undefined
	};
});