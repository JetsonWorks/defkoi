import React, { useCallback, useEffect, useState } from 'react';
import $ from "jquery";
import { Flip, toast } from "react-toastify";
import PropTypes from "prop-types";
import debounce from "lodash/debounce";

import { restUrl } from "../App";
import "../tableview.css";
import "../defkoi.css";

import { extractMessage, } from "../AjaxResponse";

import { useDispatch, useSelector } from 'react-redux';
import { selectFilterText, selectPrefs, setFilterText, } from '../features/metadata/MdSlice';
import { refreshCache, selectCache, storeSearchedEntities, } from '../features/cache/CacheSlice';

export const SearchAPI = React.createContext({});
const autoDisplayThreshold = 1000;

export default function SearchPage(props) {
  const dispatch = useDispatch();
  const searchUrl = props.searchUrl ? props.searchUrl : restUrl + props.itemName + "/search";
  const cache = useSelector(selectCache);
  const prefs = useSelector(selectPrefs);
  let [filterText, stateFilterText] = useState(useSelector(selectFilterText));

  let [toastId, setToastId] = useState(null);
  let [results, setResults] = useState([]);

  useEffect(() => {
    if(props.title)
      document.title = props.title;
  }, [props.title]);

  const debounceSearch = useCallback(
    debounce((event, ft) => {
      doSearch(event, ft);
    }, 500), []
  );

  function search(filter) {
    doSearch(null, typeof filter === "string" ? filter : filterText);
  }

  function doSearch(event, filterText) {
    if(!isFiltered()) {
      dispatch(refreshCache({type: props.itemName}));
      return;
    }
    let params = {
      filterText: filterText,
    }
    if(typeof props.addSearchParams === "function")
      props.addSearchParams(params);
    let promise = $.ajax({
      method: "GET",
      url: searchUrl,
      xhrFields: {withCredentials: true},
      data: params
    }).done(function(data, textStatus, jqXHR) {
      results = data;
      setResults(data);
      if(props.itemName) {
        dispatch(storeSearchedEntities({type: props.itemName, entities: results}));
      }
    });
    setToastId(toast.promise(promise, {
      pending: "Searching" + (props.itemName ? (" " + props.itemName) : ""),
      error: ({data}) => extractMessage(data)
    }, {
      transition: Flip,
      position: "top-center",
      autoClose: false,
      closeOnClick: false,
      draggable: false
    }));
    return promise;
  }

  // return array of cached entities based on results IDs; dispatch a call to refresh cache if needed
  function processSearchResults() {
    if(props.rawSearchResults === true)
      return results;
    let ecache = cache[props.itemName];
    let ids;
    try {
      ids = results.map(x => x.id);
    } catch(err) {
    }
    let cached = ecache.entities.filter(x => ids.includes(x.id));
    return cached;
  }

  function isReady() {
    if(typeof props.isReady === "function")
      return props.isReady();
    return results;
  }

  function onSearchControlChange(event) {
    event.persist();
    let ft = $("#filterText").val();
    if(!isFiltered())
      setResults([]);

    if("filterText" === event.target.id) {
      if(filterText !== ft) {
        debounceFilterText(ft);
      }
    }

    // initiate a search if requested or total elements <= threshold
    let searchNow = true;
    if(props.itemName) {
      let total = cache.control[props.itemName].count;
      if(!isFiltered() && (!total || total <= autoDisplayThreshold || total === count))
        searchNow = false;
      // prevent duplicate searches from responding to both onChange and onKeyPress using xor on charCode
      // if would not search but enter was pressed, then search
      // if would search and enter was not pressed, then don't search
      if(searchNow === false) {
        if(event.charCode && event.charCode === 13)
          searchNow = true;
      } else {
        if(event.charCode && event.charCode !== 13)
          searchNow = false;
      }
    }

    if(searchNow === true) {
      debounceSearch(event, ft);
    }
  }

  const debounceFilterText = useCallback(
    debounce((ft) => {
      stateFilterText(ft);
      dispatch(setFilterText(ft));
    }, 500), []
  );

  function enabledFilter() {
    return props.enableFilter === false ? false : true;
  }

  function isFiltered() {
    if($("#filterText").val() > "")
      return true;
    if(typeof props.filterCheck === "function")
      return props.filterCheck();
    return false;
  }

  function renderFilter() {
    if(enabledFilter() === false)
      return (
        <span onChange={search}>
          {renderAdditionalControls()}
        </span>
      )
    let large = (total && total > autoDisplayThreshold && !isFiltered()) ?
      <label>There are {total} records. Please consider filtering them.</label>
      : null;

    return (
      <div className="css-tableview">
        {large}
        <ul>
          <li onChange={onSearchControlChange}>
            <label>Filter by text:</label>
            <input id="filterText" type="text" autoFocus defaultValue={filterText} onKeyPress={onSearchControlChange}/>
            <input id="filter" type="button" value="Search" onClick={search}/>
            <label/>
            {renderAdditionalControls()}
          </li>
        </ul>
      </div>
    );
  }

  function renderAdditionalControls() {
    if(typeof props.renderAdditionalControls === "function")
      return props.renderAdditionalControls();
    return (
      <span></span>
    );
  }

  function isFilterRetiredEntities() {
    if(typeof props.isFilterRetiredEntities === "function") {
      return props.isFilterRetiredEntities();
    }
    return false === prefs[props.showRetiredPrefKey];
  }

  // if we haven't searched yet and preferences are loaded, search using appropriate function
  // but don't auto-search if it's likely to cause a significant delay, unless we already have filterText
  const total = props.itemName ? cache.control[props.itemName].count : 0;
  const count = props.itemName ? cache[props.itemName].entities.length : 0;
  if(!toastId) {
    if(isFiltered()) {
      debounceSearch(null, filterText);
    }
  }

  let rowData = processSearchResults();
  if(props.itemName) {
    if(rowData.length === 0 && !isFiltered() && total && (total <= autoDisplayThreshold)) {
      rowData = cache[props.itemName].entities;
    }
  }
  // filter out retired if pref is to not show them
  if(isFilterRetiredEntities())
    rowData = rowData.filter(x => x.active === true || x.active === undefined);
  return (
    <div>
      {props.title && (
        <h2 className="pageTitle">{props.title}</h2>
      )}
      <SearchAPI.Provider value={{
        search: search,
        results: rowData,
      }}>
        {renderFilter()}
        <div>
          {isReady() && props.children}
        </div>
      </SearchAPI.Provider>
    </div>
  );
}

SearchPage.propTypes = {
  title: PropTypes.string,
  searchUrl: PropTypes.string,                  // custom search URL, overrides default of restUrl + itemName + "/search"
  enableFilter: PropTypes.bool,                 // overrides class default state value (default: true)
  renderAdditionalControls: PropTypes.func,     // callback to render additional filter controls
  filterCheck: PropTypes.func,                  // callback to perform additional checks whether filters are applied
  isReady: PropTypes.func,                      // callback to determine if we have enough data loaded to render
  addSearchParams: PropTypes.func,              // callback to add parameters to the search request
  itemName: PropTypes.string,                   // the singular entity resource key
  rawSearchResults: PropTypes.bool,             // whether to use raw results from search (default: false)
  isFilterRetiredEntities: PropTypes.func,      // whether retired entities should be filtered (default: !props.showRetiredPrefKey)
}

