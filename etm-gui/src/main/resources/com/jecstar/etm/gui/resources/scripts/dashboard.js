function loadDashboard(name) {
	var currentDashboard;
	var currentGraph;
	
	$('#lnk-edit-dashboard').click(function (event) {
		event.preventDefault();
		showSettings(currentDashboard);
	});
	
	$('#btn-apply-dashboard-settings').click(function (event) {
		if (!currentDashboard) {
			currentDashboard = {
			};
		}
		currentDashboard.name = $('#input-dashboard-name').val();
		var oldRows = currentDashboard.rows;
		currentDashboard.rows = [];
		$('#dashboard-settings-columns > div.fieldConfigurationRow').each(function (rowIx, row){
			var nrOfCols = $(row).find("input[name='row-cols']").val();
			var height = $(row).find("input[name='row-height']").val();
			var oldRow;
			var rowId = $(row).attr('data-row-id');
			if (oldRows && rowId) {
				// find the row that is edited.
				oldRow = $.grep(oldRows, function(n, i) {
					return n.id == rowId;
				})[0];
			}
			var jsonRow = {
					id: generateUUID(),
					height: height,
					cols: []
			}
			if (oldRow) {
				jsonRow.id = oldRow.id;
				jsonRow.height =  height
			} 
			if (oldRow && oldRow.cols && oldRow.cols.length == nrOfCols) {
				// Number of columns in row not changed.
				jsonRow.cols = oldRow.cols;
			} else {
				var remainingParts = 12;
				for (i=0; i< nrOfCols; i++) {
					var parts = Math.ceil(remainingParts / (nrOfCols - i));
					column = {
						id: generateUUID(),
						parts: parts,
						bordered: true
					}
					if (oldRow && oldRow.cols && oldRow.cols[i]) {
						column = oldRow.cols[i];
						column.parts = parts;
					}
					jsonRow.cols.push(column);
					remainingParts -= parts;
				}		
			}
			currentDashboard.rows.push(jsonRow);
		});
		
		$('#modal-dashboard-settings').modal('hide');
		// TODO update dashboard via rest.
		buildPage(currentDashboard);
	});
	
	$('#btn-apply-graph-settings').click(function (event) {
		currentGraph.title = $("#input-graph-title").val();
		currentGraph.bordered = $('#sel-graph-border').val() == 'true' ? true : false;
		currentGraph.showLegend = $('#sel-graph-legend').val() == 'true' ? true : false;

		$("div[data-col-id='" + currentGraph.id + "']").replaceWith(createCell(currentGraph));
		
		// TODO update data via rest.
		$('#modal-graph-settings').modal('hide');
	});
	
	if (name) {
		// TODO load dashboard via rest.
		currentDashboard = {
				name: 'My Dashboard',
				rows: [
				  { 
					id: generateUUID(),
					height: 16,
					cols: [
					  { 
						id: generateUUID(),
						parts: 6,
						title: 'Log types over time',
						bordered: true,
						showLegend: true,
						type: 'line',
						area: true,
						index: {
							name: 'etm_event_all',
							types: ['log'],
						},
						x_axis: { 
							agg: {
								name: 'logs',
								type: 'date-histogram',
								field: 'endpoints.writing_endpoint_handler.handling_time',
								interval: 'hour',
								aggs: [
								       {
								    	   name: 'levels',
								    	   type: 'terms',
								    	   field: 'log_level'
								       }
								]
							}
						},
						y_axis: {
							agg : {
								name: 'count',
								type: 'count',
									
							},
							label: 'Count'
						}					
					  }	
					]
				  }
				]	
		}
		buildPage(currentDashboard);
	} else {
		showSettings();
	}
	
	function showSettings(dashboardData) {
		$('#dashboard-settings-columns').empty();
		$('#dashboard-settings-columns').append(
		    $('<div>').addClass('row').append(
		    	$('<div>').addClass('col-sm-5 font-weight-bold').text('Columns'), 
		    	$('<div>').addClass('col-sm-5 font-weight-bold').text('Height'), 
		    	$('<div>').addClass('col-sm-2 font-weight-bold')
		        	.append($('<a href="#">').text('Add row')
		        		.attr('id', 'link-add-dashboard-row')	
		        		.click(function (event) {
		        			event.preventDefault(); 
		                	$('#dashboard-settings-columns').append(createRow());
		                    updateRowActions();
		        		})
		            )        
		        )
		    );			
		if (dashboardData) {
			$('#input-dashboard-name').val(dashboardData.name);
			if (dashboardData.rows) {
				$.each(dashboardData.rows, function(ix, row) {
					$('#dashboard-settings-columns').append(createRow(row));
				});
				updateRowActions();
			}
		} else {
			$('#input-dashboard-name').val('My New Dashboard');
			$('#dashboard-settings-columns').append(createRow());
		}
		$('#modal-dashboard-settings').modal();
		
		function createRow(rowData) {
	        var row = $('<div>').addClass('row fieldConfigurationRow').attr('style', 'margin-top: 5px;');
	        $(row).append(
	            $('<div>').addClass('col-sm-5').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append(
	            		$('<input>').attr('type', 'number').attr('min', '1').attr('max', '12').attr('name', 'row-cols').addClass('form-control form-control-sm').val(1)
	            ),
	            $('<div>').addClass('col-sm-5').attr('style', 'padding-right: 0px; padding-left: 0.5em;').append(
	            		$('<input>').attr('type', 'number').attr('min', '1').attr('max', '50').attr('name', 'row-height').addClass('form-control form-control-sm').val(16)
	            )
	        );
	        var actionDiv = $('<div>').addClass('col-sm-2').append(
	            $('<div>').addClass('row actionRow').append(
	                $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-arrow-up').click(function (event) {event.preventDefault(); moveRowUp(row)})),
	                $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-arrow-down').click(function (event) {event.preventDefault(); moveRowDown(row)})),
	                $('<div>').addClass('col-sm-1').append($('<a href="#">').addClass('fa fa-times text-danger').click(function (event) {event.preventDefault(); removeRow(row)}))
	            )
	        );
	        $(row).append($(actionDiv));
	        if (rowData) {
	        	$(row).attr('data-row-id', rowData.id);
	        	$(row).children().each(function (index, child) {
	                if (0 === index) {
	                    $(child).find('input').val(rowData.cols.length);
	                } else if (1 === index) {
	                    $(child).find('input').val(rowData.height);
	                } 
	            }); 
	        }
	        return row;        
	    }
		
		function updateRowActions() {
	        $('#dashboard-settings-columns .actionRow').each(function (index, row) {
	            if ($('#dashboard-settings-columns').children().length > 2) {
	                if (index == 0) {
	                    $(row).find('.fa-arrow-up').hide();
	                } else {
	                    $(row).find('.fa-arrow-up').show();
	                }
	                if (index >= $('#dashboard-settings-columns').children().length -2) {
	                    $(row).find('.fa-arrow-down').hide();
	                } else {
	                    $(row).find('.fa-arrow-down').show();
	                }
	            } else {
	                $(row).find('.fa-arrow-up').hide();
	                $(row).find('.fa-arrow-down').hide();
	            }
	        });			
		}
		
		function removeRow(row) {
	        $(row).remove();
	        updateRowActions();
	    }
	    
	    function moveRowUp(row) {
	        $(row).after($(row).prev());
	        updateRowActions();
	    }

	    function moveRowDown(row) {
	        $(row).before($(row).next());
	        updateRowActions();
	    }
	}
	
	function buildPage() {
		$('#dashboard-name').text(currentDashboard.name);
		var graphContainer = $('#graph-container').empty();
		$.each(currentDashboard.rows, function(rowIx, row) {
			var rowContainer = $('<div>').addClass('row').attr('data-row-id', row.id).attr('style', 'height: ' + row.height + 'rem; padding-bottom: 15px;');
			if (rowIx != 0) {
				var oldStyle = $(rowContainer).attr('style');
				$(rowContainer).attr('style', oldStyle + ' padding-top: 15px;');
			}
			$.each(row.cols, function (colIx, col) {
				var cell = createCell(col);
				rowContainer.append(cell);
				// Now load the data.
				// TODO controleer of de col ook data heeft om op te halen...
			    $.ajax({
			        type: 'POST',
			        contentType: 'application/json',
			        url: '../rest/dashboard/chart',
			        data: JSON.stringify(col),
			        success: function(data) {
			            if (!data) {
			                return;
			            }
			            if ('line' == col.type) {
			            	renderLineChart(cell, col, data);
			            }
			        }
			    });							
			});
			graphContainer.append(rowContainer);
		});
	}
	
	function createCell(col) {
		var cellContainer = $('<div>').attr('data-col-id', col.id).addClass('col-lg-' + col.parts).attr('style', 'height: 100%;');
		var card = $('<div>').addClass('card card-block').attr('style', 'height: 100%;');
		cellContainer.append(card);
		if (!col.bordered) {
			card.addClass('noBorder');
		}
		card.append(
		  $('<h5>').addClass('card-title').text(col.title).append(
		    $('<a>').attr('href', '#').addClass('fa fa-pencil-square-o pull-right invisible').attr('name', 'edit-graph').click(function (event) {
		    	event.preventDefault();
		    	editGraph(col.id);
		    }) 
		  )
		);
		cellContainer.on("mouseover mouseout", function() {
			cellContainer.find("a[name='edit-graph']").toggleClass('invisible');
			card.toggleClass('selectedColumn');
			if (!col.bordered) {
				card.toggleClass('noBorder');
			}
		});	
		return cellContainer;
	}
	
	function editGraph(cellId) {
		for (rowIx=0; rowIx < currentDashboard.rows.length; rowIx++) {
			for (colIx=0; colIx < currentDashboard.rows[rowIx].cols.length; colIx++) {
				if (currentDashboard.rows[rowIx].cols[colIx].id == cellId) {
					currentGraph = currentDashboard.rows[rowIx].cols[colIx];
				}
			}
		}
		$('#input-graph-title').val(currentGraph.title);
		$('#sel-graph-border').val(currentGraph.bordered ? 'true' : 'false');
		$('#sel-graph-legend').val(currentGraph.showLegend ? 'true' : 'false');
		$('#modal-graph-settings').modal();
	}
	
	function renderLineChart(svgContainer, config, data) {
        nv.addGraph(function() {
      	  var chart = nv.models.lineChart()
            .x(function(d) { return d[0] })
            .y(function(d) { return d[1] })
      	  	.useInteractiveGuideline(true)
      	  	.duration(0)
      	  	.showLegend(config.showLegend);

          chart.xAxis
            .axisLabel(config.x_axis.label)
            .tickFormat(function(d) { return d3.time.format('%Y-%m-%d %H:%M')(new Date(d)) });
          chart.yAxis
          	.axisLabel(config.y_axis.label)

      	  d3.select(svgContainer.get(0))   
      	  	.append("svg")
      	    .datum(createData(config, data))
      	    .call(chart);

      	  nv.utils.windowResize(function() { chart.update() });
      	  return chart;
      });	
        
      function createData(config, data) {
    	  var result = [];
    	  var xBuckets = data[config.x_axis.agg.name].buckets;
    	  var xSubAggs = config.x_axis.agg.aggs;
    	  
    	  $.each(xBuckets, function(xBucketIx, xBucket) {
    		  var x = xBucket.key;
    		  var seriesName = config.y_axis.agg.name;
    		  if ("undefined" !== typeof xSubAggs) {
    			  var xSubAgg = xSubAggs[0];
    			  var xSubBuckets = xBucket[xSubAgg.name].buckets;
    			  $.each(xSubBuckets, function(xSubBucketIx, xSubBucket) {
    				  seriesName = xSubBucket.key;
    				  var y = "count" == config.y_axis.agg.name ? xSubBucket.doc_count : xSubBucket[config.y_axis.agg.name].value;
    				  addToResult(result, seriesName, config.area, x, y);
    			  });
    		  } else {
    			  var y = "count" == config.y_axis.agg.name ? xBucket.doc_count : xBucket[config.y_axis.agg.name].value;
    			  addToResult(result, seriesName, config.area, x, y);
    		  }
    	  });
    	  return result;
      }
      
      function addToResult(result, serieName, area, x, y) {
		  var serie = $.grep(result, function(n,i) {
			  return n.key === serieName;
		  });
		  if ("undefined" !== typeof serie && serie.length > 0) {
			  serie[0].values.push([x, y]);
		  } else {
			  result.push({
				 key: serieName,
				 values: [[x,y]],
				 area: area
			  });
		  }
      }
	}
	
	// TODO deze functie moet gebruikt worden tijdens het opslaan van een grafiek. Het ID moet dan geset worden naar een unieke waarde
	function generateUUID() {
    	var d = new Date().getTime();
	    if(window.performance && typeof window.performance.now === "function"){
	        d += performance.now(); //use high-precision timer if available
	    }
	    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
	        var r = (d + Math.random()*16)%16 | 0;
	        d = Math.floor(d/16);
	        return (c=='x' ? r : (r&0x3|0x8)).toString(16);
	    });
	    return uuid;
	}
}