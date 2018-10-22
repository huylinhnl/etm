function buildGroupPage() {
    $notifierSelect = $('<select>').addClass('form-control custom-select etm-notifier');

    $.when(
        $.ajax({
            type: 'GET',
            contentType: 'application/json',
            url: '../rest/search/keywords/etm_event_all',
            cache: false,
            success: function (data) {
                if (!data || !data.keywords) {
                    return;
                }
                $('#input-filter-query').bind('keydown', function (event) {
                    if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                        event.stopPropagation();
                    }
                }).autocompleteFieldQuery({queryKeywords: data.keywords});
            }
        }),
        $.ajax({
            type: 'GET',
            contentType: 'application/json',
            url: '../rest/settings/notifiers/basics',
            cache: false,
            success: function (data) {
                if (!data || !data.notifiers) {
                    // No groups, remove the fieldset.
                    $('#lnk-add-notifier').parent().remove();
                    return;
                }
                $.each(data.notifiers, function (index, notifier) {
                    $notifierSelect.append($('<option>').attr('value', notifier.name).text(notifier.name));
                });
                sortSelectOptions($notifierSelect);
            }
        })
    ).done(function () {
        $.ajax({
            type: 'GET',
            contentType: 'application/json',
            url: '../rest/settings/groups',
            cache: false,
            success: function (data) {
                if (!data) {
                    return;
                }
                if (data.has_ldap) {
                    $('#btn-confirm-import-group').show();
                }
                $groupSelect = $('#sel-group');
                $.each(data.groups, function (index, group) {
                    $groupSelect.append($('<option>').attr('value', group.name).text(group.name));
                    groupMap[group.name] = group;
                });
                sortSelectOptions($groupSelect)
                $groupSelect.val('');
            }
        });
    });

	var groupMap = {};
	$('#sel-group').change(function(event) {
		event.preventDefault();
		var groupData = groupMap[$(this).val()];
		if ('undefined' == typeof groupData) {
			resetValues();
			return;
		}
        $('#list-notifiers').empty();
		$('#input-group-name').val(groupData.name);
		$('#input-group-display-name').val(groupData.display_name);
		$('#input-filter-query').val(groupData.filter_query);
		$('#sel-filter-query-occurrence').val(groupData.filter_query_occurrence);
		$('#sel-always-show-correlated-events').val(groupData.always_show_correlated_events ? 'true' : 'false');
		$('#group-roles-container > label > input').prop('checked', false);
		if (groupData.roles) {
		    $('#card-acl').find('select').val('none');
			$.each(groupData.roles, function(index, role) {
			    $('#card-acl').find("option[value='" + role + "']").parent().val(role);
			});
		}
        if (groupData.dashboard_datasources) {
            $('#dashboard-datasource-block').find("input[type='checkbox']").prop('checked', false);
            $.each(groupData.dashboard_datasources, function (index, ds) {
                $('#check-dashboard-datasource-' + ds).prop('checked', true);
            });
        }
        if (groupData.signal_datasources) {
            $('#signal-datasource-block').find("input[type='checkbox']").prop('checked', false);
            $.each(groupData.signal_datasources, function (index, ds) {
                $('#check-signal-datasource-' + ds).prop('checked', true);
            });
        }
        if (groupData.notifiers) {
            $.each(groupData.notifiers, function (index, notifierName) {
                $('#list-notifiers').append(createNotifierRow(notifierName));
            });
        }
		enableOrDisableButtons();
	});

	$('#btn-confirm-save-group').click(function(event) {
		event.preventDefault();
		if (!document.getElementById('group_form').checkValidity()) {
			return;
		}
		var groupName = $('#input-group-name').val();
		if (isGroupExistent(groupName)) {
			$('#overwrite-group-name').text(groupName);
			$('#modal-group-overwrite').modal();
		} else {
			saveGroup();
		}
	});
	
	$('#btn-save-group').click(function(event) {
		saveGroup();
	});
	
	$('#btn-confirm-remove-group').click(function(event) {
		event.preventDefault();
		$('#remove-group-name').text($('#input-group-name').val());
        $('#modal-group-remove').modal();
	});	

	$('#btn-remove-group').click(function(event) {
		removeGroup($('#input-group-name').val());
	});
	
	$('#btn-confirm-import-group').click(function(event) {
		event.preventDefault();
		$("#sel-import-group").empty();
		$.ajax({
		    type: 'GET',
		    contentType: 'application/json',
		    url: '../rest/settings/groups/ldap',
		    cache: false,
		    success: function(data) {
		        if (!data) {
		            return;
		        }
		        $groupSelect = $('#sel-import-group');
		        $.each(data.groups, function(index, group) {
		        	$groupSelect.append($('<option>').attr('value', group.name).text(group.name));
		        });
		        sortSelectOptions($groupSelect)
		        $groupSelect.val('');
		        $('#modal-group-import').modal();
		    }
		});		
	});

	$('#btn-import-group').click(function(event) {
		event.preventDefault();
		var groupName = $("#sel-import-group").val();
		if (!groupName) {
			return false;
		}
		$.ajax({
		    type: 'PUT',
		    contentType: 'application/json',
		    url: '../rest/settings/groups/ldap/import/' + encodeURIComponent(groupName),
		    cache: false,
		    success: function(group) {
		        if (!group) {
		            return;
		        }
				// First the group if it is already present
				$('#sel-group > option').each(function () {
				    if(group.name == $(this).attr('value')) {
				        $(this).remove();
				    }
				});
				// Now add the updated group
		        $('#sel-group').append($('<option>').attr('value', group.name).text(group.name));
		        sortSelectOptions($('#sel-group'));
		        groupMap[group.name] = group;
		        $('#sel-group').val(group.name).trigger('change');
		    }
		}).always(function () {
		    hideModals($('#modal-group-import'));
        });
	});

    $('#lnk-add-notifier').click(function (event) {
        event.preventDefault();
        $('#list-notifiers').append(createNotifierRow());
    });
	
	$('#input-group-name').on('input', enableOrDisableButtons);
	
	function sortSelectOptions($select) {
		var options = $select.children('option');
		options.detach().sort(function(a,b) {
		    var at = $(a).text();
		    var bt = $(b).text();         
		    return (at > bt) ? 1 : ((at < bt) ? -1 : 0);
		});
		options.appendTo($select);
	}
	
	function enableOrDisableButtons() {
		var groupName = $('#input-group-name').val();
		if (groupName) {
			$('#btn-confirm-save-group').removeAttr('disabled');
			if (isGroupExistent(groupName)) {
				$('#btn-confirm-remove-group').removeAttr('disabled');
			} else {
				$('#btn-confirm-remove-group').attr('disabled', 'disabled');
			}
		} else {
			$('#btn-confirm-save-group, #btn-confirm-remove-group').attr('disabled', 'disabled');
		}
	}
	
	function isGroupExistent(groupName) {
		return "undefined" != typeof groupMap[groupName];
	}
	
	function saveGroup() {
		var groupData = createGroupData();
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/group/' + encodeURIComponent(groupData.name),
            cache: false,
            data: JSON.stringify(groupData),
            success: function(data) {
                if (!data) {
                    return;
                }
        		if (!isGroupExistent(groupData.name)) {
        			$groupSelect = $('#sel-group');
        			$groupSelect.append($('<option>').attr('value', groupData.name).text(groupData.name));
        			sortSelectOptions($groupSelect);
        		}
        		groupMap[groupData.name] = groupData;
        		$('#groups_infoBox').text('Group \'' + groupData.name+ '\' saved.').show('fast').delay(5000).hide('fast');
            }
        }).always(function () {
            hideModals($('#modal-group-overwrite'));
        });
	}
	
	function removeGroup(groupName) {
		$.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/settings/group/' + encodeURIComponent(groupName),
            cache: false,
            success: function(data) {
                if (!data) {
                    return;
                }
        		delete groupMap[groupName];
        		$("#sel-group > option").filter(function(i){
        		       return $(this).attr("value") == groupName;
        		}).remove();
        		$('#groups_infoBox').text('Group \'' + groupName + '\' removed.').show('fast').delay(5000).hide('fast');
            }
        }).always(function () {
            hideModals($('#modal-group-remove'));
        });
	}

    function createNotifierRow(notifierName) {
        var notifierRow = $('<li>').attr('style', 'margin-top: 5px; list-style-type: none;').append(
            $('<div>').addClass('input-group').append(
                $notifierSelect.clone(true),
                $('<div>').addClass('input-group-append').append(
                    $('<button>').addClass('btn btn-outline-secondary fa fa-times text-danger').attr('type', 'button').click(function (event) {
                        event.preventDefault();
                        removeNotifierRow($(this));
                    })
                )
            )
        );
        if (notifierName) {
            $(notifierRow).find('.etm-notifier').val(notifierName)
        }
        return notifierRow;
    }

    function removeNotifierRow(anchor) {
        anchor.parent().parent().parent().remove();
    }
	
	function createGroupData() {
		var groupData = {
			name: $('#input-group-name').val(),
			display_name: $('#input-group-display-name').val() ? $('#input-group-display-name').val() : null,
			filter_query: $('#input-filter-query').val() ? $('#input-filter-query').val() : null,
			filter_query_occurrence: $('#sel-filter-query-occurrence').val(),		
			always_show_correlated_events: $('#sel-always-show-correlated-events').val() == 'true' ? true : false,
            roles: [],
            dashboard_datasources: $('#dashboard-datasource-block')
                .find("input[type='checkbox']:checked")
                .map(function () {
                    return $(this).val();
                }).get(),
            signal_datasources: $('#signal-datasource-block')
                .find("input[type='checkbox']:checked")
                .map(function () {
                    return $(this).val();
                }).get(),
            notifiers: []
		}
		$('#card-acl').find('select').each(function () {
		    if ($(this).val() !== 'none') {
			    groupData.roles.push($(this).val());
			}
		});
        $('.etm-notifier').each(function () {
            var notifierName = $(this).val();
            if (-1 == groupData.notifiers.indexOf(notifierName)) {
                groupData.notifiers.push(notifierName);
            }
        });
		return groupData;
	}

	function resetValues() {
	    document.getElementById('group_form').reset();
		enableOrDisableButtons();
	}
}