import React, { useEffect } from 'react';
import PropTypes from "prop-types";

import "../tableview.css";
import { useSelector } from 'react-redux';
import { selectCache, } from '../features/cache/CacheSlice';

export default function Status(props) {
  const cache = useSelector(selectCache);

  useEffect(() => {
    document.title = "Status";
  });

  let children = [];
  children.push(
    <li key="head" className="table-header-group">
      <label>type</label>
      <label>last</label>
      <label>status</label>
    </li>
  );
  Object.keys(cache.control).forEach(key => children.push(
    <li key={key}>
      <label>{key}</label>
      <label>{cache[key].modTime}</label>
      <label>{cache[key].status}</label>
    </li>
  ));
  return (
    <div>
      <h2 className="pageTitle">Cache Status</h2>
      Status: {cache.status}
      <div className="css-tableview grid">
        <ul>
          {children}
        </ul>
      </div>
    </div>
  );

}
Status.propTypes = {
  title: PropTypes.string,
}

