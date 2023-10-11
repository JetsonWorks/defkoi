import React, { useEffect, useState } from 'react';
import $ from "jquery";
import { Flip, toast } from "react-toastify";
import classNames from "classnames";
import PropTypes from "prop-types";

import { useDispatch, useSelector } from 'react-redux';
import { selectPrefs, setBurgerAnimation, setBurgerAutoHide, setBurgerMenuOpen, setBurgerSide, setGridAnimatedRows, } from '../features/metadata/MdSlice';

const menus = {
  slide: {buttonText: "Slide", items: 1},
  stack: {buttonText: "Stack", items: 1},
  elastic: {buttonText: "Elastic", items: 1},
  bubble: {buttonText: "Bubble", items: 1},
  fallDown: {buttonText: "Fall Down", items: 1},
  push: {buttonText: "Push", items: 1},
  pushRotate: {buttonText: "Push Rotate", items: 1},
  reveal: {buttonText: "Reveal", items: 1},
  scaleDown: {buttonText: "Scale Down", items: 1},
  scaleRotate: {buttonText: "Scale Rotate", items: 1}
};

export const prefsUrl = "model/prefs";

export function savePrefs(prefs) {
  return $.ajax({
    type: "PATCH",
    url: prefsUrl,
    xhrFields: { withCredentials: true },
    data: JSON.stringify(prefs),
    success: function() {
      toast.success("Saved preferences", { position: "bottom-right", autoClose: 1000, transition: Flip, });
    },
    contentType: "application/json"
  });
}

export default function Preferences(props) {
  const dispatch = useDispatch();
  const prefs = useSelector(selectPrefs);

  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    document.title = props.title ? props.title : "Preferences";
  }, [props.title]);

  // update the menu animation in the global state, and then open the menu
  function changeMenu(menu) {
    dispatch(setBurgerAnimation(menu));
    setDirty(true);
    dispatch(setBurgerMenuOpen(true));
  }

  function changeSide(side) {
    dispatch(setBurgerSide(side));
    setDirty(true);
  }

  function changeAutoHide(enabled) {
    dispatch(setBurgerAutoHide(enabled));
    setDirty(true);
  }

  function changeGridAnimatedRows(enabled) {
    dispatch(setGridAnimatedRows(enabled));
    setDirty(true);
  }

  function makePrefs() {
    return {
      "burgerAnimation": prefs.burgerAnimation,
      "burgerSide": prefs.burgerSide,
      "burgerAutoHide": prefs.burgerAutoHide,
      "gridAnimatedRows": prefs.gridAnimatedRows,
    }
  }

  function save(prefs) {
    savePrefs(prefs).then(() => setDirty(false));
  }

  const buttons = Object.keys(menus).map((menu) => {
    return (
      <button key={menu}
        className={classNames({"current-demo": menu === prefs.burgerAnimation})}
        onClick={() => changeMenu(menu)}>
        {menus[menu].buttonText}
      </button>
    );
  });

  return (
    <main>
      <h1>Menu Preferences</h1>
      <h2 className="description">Side of page</h2>
      <button className={classNames({"side-button": true, "left": true, "active": prefs.burgerSide === "left"})}
        onClick={() => changeSide("left")}>Left</button>
      <button className={classNames({"side-button": true, "right": true, "active": prefs.burgerSide === "right"})}
        onClick={() => changeSide("right")}>Right</button>

      <h2 className="description">Choose an animation</h2>
      <nav className="demo-buttons">
        {buttons}
      </nav>

      <nav>
        <h2 className="description">Menu auto-hide</h2>
        <button className={classNames({"side-button": true, "left": true,
          "active": prefs.burgerAutoHide === false})}
          onClick={() => changeAutoHide(false)}>Off</button>
        <button className={classNames({"side-button": true, "right": true,
          "active": prefs.burgerAutoHide === true})}
          onClick={() => changeAutoHide(true)}>On</button>
      </nav>

      <nav>
        <h2 className="description">Grid animated rows</h2>
        <button className={classNames({"side-button": true, "left": true,
          "active": prefs.gridAnimatedRows === false})}
          onClick={() => changeGridAnimatedRows(false)}>Off</button>
        <button className={classNames({"side-button": true, "right": true,
          "active": prefs.gridAnimatedRows === true})}
          onClick={() => changeGridAnimatedRows(true)}>On</button>
      </nav>

      <nav>
        <h2 className="description">Preferences</h2>
        <button className={classNames({"side-button": true, "left": true, "active": dirty === false})}>Saved</button>
        <button className={classNames({"side-button": true, "right": true, "active": dirty === true})}
          onClick={() => save(makePrefs())}>Save</button>
      </nav>
    </main>
  );

}
Preferences.propTypes = {
  title: PropTypes.string,
}

