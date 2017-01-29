var max_search_templates;

$('#template-name').on('input', function() {
    if (!$(this).val()) {
        $('#btn-save-template').attr('disabled', 'disabled');
    } else {
        $('#btn-save-template').removeAttr('disabled');
    }
});

$('#btn-save-template').click(function() {
    var template = createTemplate();
    var current = $('#list-template-links > li > a').filter(function() {
        return $(this).text() === template.name;
    }).toArray();
    if (current.length != 0) {
        $('#overwrite-template-name').text(template.name);
        $('#modal-template-overwrite').modal();
    } else {
        storeTemplate(template, false);
    }
});

$('#btn-overwrite-template').click(function() {
    $('#modal-template-overwrite').modal('hide');
    storeTemplate(createTemplate(), true);
});

$('#btn-remove-template').click(function(event) {
    event.preventDefault();
    var templateName = $('#remove-template-name').text();
    $.ajax({
        type: 'DELETE',
        contentType: 'application/json',
        url: '../rest/search/templates/' + encodeURIComponent(templateName),
        success: function() {
            $('#list-template-links > li > a').filter(function() {
                    return $(this).text() === templateName;
                }).parent().remove();
            validateMaxTemplates();
        }
    });        
    $('#modal-template-remove').modal('hide');
});


// Load the stores search templates.
$.ajax({
    type: 'GET',
    contentType: 'application/json',
    url: '../rest/search/templates',
    success: function(data) {
        if (!data || !data.search_templates) {
            return;
        }
        max_search_templates = data.max_search_templates;
        $.each(data.search_templates, function(index, template){
            $('#list-template-links').append(
                $('<li>').append(
                    $('<a href="#">').click(function(event) {
                       event.preventDefault();
                       setValuesFromTemplate(template)
                    }).text(template.name),
                    $('<a href="#" class="fa fa-times pull-right text-danger">').click(function(event) {
                       event.preventDefault();
                       askRemoveTemplate(template)
                    })
                )
            );
        });
        validateMaxTemplates();
    }
});

$.ajax({
    type: 'GET',
    contentType: 'application/json',
    url: '../rest/search/history',
    success: function(data) {
        if (!data || !data.search_history) {
            return;
        }
        data.search_history.reverse();
        $.each(data.search_history, function(index, query){
            $('#list-search-history-links').append(
                $('<li>').append(
                    $('<a href="#">')
                    .click(function(event) {
                       event.preventDefault();
                       setValuesFromHistory(query)
                    })
                    .text(query.query)
                    .attr('title', query.query)
                )
            );
        });
    }
});

function validateMaxTemplates() {
	if ($('#list-template-links').children().length >= max_search_templates ) {
		$('#no-more-tenplates-allowed').show();
		$('#template-save-group').hide();
	} else {
		$('#no-more-tenplates-allowed').hide();
		$('#template-save-group').show();		
	}
}

function setValuesFromTemplate(template) {
    $('[id^=check-type-]').prop('checked', false);
    $.each(template.types, function(index, type){
        $('#check-type-' + type).prop('checked', true);    
    });
    $('#query-string').val(template.query);
    $('#template-name').val(template.name);
    tableLayout.current_ix = 0;
    tableLayout.fields = template.fields;
    tableLayout.results_per_page = template.results_per_page;
    tableLayout.sort_field = template.sort_field;
    tableLayout.sort_order = template.sort_order;
    $('#query-string').removeAttr('disabled').trigger('input');
}

function setValuesFromHistory(query) {
    $('[id^=check-type-]').prop('checked', false);
    $.each(query.types, function(index, type){
        $('#check-type-' + type).prop('checked', true);    
    });
    $('#query-string').val(query.query);
    tableLayout.current_ix = 0;
    tableLayout.fields = query.fields;
    tableLayout.results_per_page = query.results_per_page;
    tableLayout.sort_field = query.sort_field;
    tableLayout.sort_order = query.sort_order;
    $('#query-string').removeAttr('disabled').trigger('input');
}

function updateHistory(query, max_size) {
	$('#list-search-history-links > li > a[title="' + query.query.replace(/(")/g, "\\$1" ) + '"]').parent().remove();
    $('#list-search-history-links').prepend(
            $('<li>').append(
                $('<a href="#">')
                .click(function(event) {
                   event.preventDefault();
                   setValuesFromHistory(query)
                })
                .text(query.query)
                .attr('title', query.query)
            )
     ).children().slice(max_size).remove();
    
}

function createTemplate() {
    var template = { 
        name: $('#template-name').val(),
        query: $('#query-string').val(),
        types: $('[id^=check-type-]:checked').map(function(){ return $(this).val(); }).get(),
        fields: tableLayout.fields,
        results_per_page: tableLayout.results_per_page,
        sort_field: tableLayout.sort_field,
        sort_order: tableLayout.sort_order            
    };
    return template;
}

function storeTemplate(template, isOverwrite) {
    $.ajax({
        type: 'PUT',
        contentType: 'application/json',
        url: '../rest/search/templates/' + encodeURIComponent($('#template-name').val()),
        data: JSON.stringify(template),
        success: function() {
            $('#template-name').val('');
            $('#btn-save-template').attr('disabled', 'disabled');
            if (isOverwrite) {
                $('#list-template-links > li > a').filter(function() {
                    return $(this).text() === template.name;
                }).unbind('click').click(function(event) {
                    event.preventDefault();
                    setValuesFromTemplate(template)
                });
            } else {
                $('#list-template-links').append(
                    $('<li>').append(
                        $('<a href="#">').click(function(event) {
                           event.preventDefault();
                           setValuesFromTemplate(template)
                        }).text(template.name),
                        $('<a href="#" class="fa fa-times pull-right text-danger">').click(function(event) {
                           event.preventDefault();
                           askRemoveTemplate(template)
                        })
                    )
                );
                validateMaxTemplates();                
            }
        }
    });
}

function askRemoveTemplate(template) {
    $('#remove-template-name').text(template.name);
    $('#modal-template-remove').modal();
}