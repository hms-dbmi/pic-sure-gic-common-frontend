define(["underscore"],  function(_){
    return {
        render: {
            phenoToString: function(query, format, stringRender){
                const filters = [];
                const variables = [];
                filters.push(format.categories(query.categoryFilters));
                filters.push(format.numeric(query.numericFilters));
                query.variantInfoFilters.forEach(({ numericVariantInfoFilters, categoryVariantInfoFilters }) => {
                    filters.push(format.categoryVariant(categoryVariantInfoFilters));
                    filters.push(format.numericVariant(numericVariantInfoFilters));
                });
                filters.push(format.anyRecordOf(query.anyRecordOf));
                filters.push(format.anyRecordOfMulti(query.anyRecordOfMulti));
                
                variables.push(format.selectedVariables(query.fields));
    
                const toString = (list, prefix) => {
                    const stringList = stringRender(_.flatten(list).filter(x => x)).map(item => '- ' + item);
                    return stringList.length > 0 ? [ prefix, ...stringList ] : [ `${prefix} none selected` ];
                };
    
                const listString = [
                    ...toString(filters, "Filters:"),
                    ...toString(variables, "Additional Variables:"),
                ].join("\n");
                return listString;
            }
        }
    };
});