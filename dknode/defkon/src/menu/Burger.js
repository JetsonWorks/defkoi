import React from 'react';
import BurgerMenu from "react-burger-menu";
import { NavLink } from "react-router-dom";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from 'react-redux';
import { useAuth } from "react-oidc-context";

import "./burger.css";

import { selectBurgerMenuOpen, selectPrefs, setBurgerMenuOpen, } from '../features/metadata/MdSlice';

export class MenuWrap extends React.Component {
  constructor (props) {
    super(props);
    this.state = {
      hidden: false
    };
  }

  componentDidUpdate(prevProps) {
    const sideChanged = this.props.children.props.right !== prevProps.children.props.right;
    if (sideChanged) {
      this.setState({hidden: true});

      setTimeout(() => {
        this.show();
      }, this.props.wait);
    }
  }

  show() {
    this.setState({hidden: false});
  }

  hide() {
    this.setState({hidden: true});
  }

  render() {
    let style = this.state.hidden ? {display: "none"} : {};
    return (
      <div style={style} className={this.props.side}>
        {this.props.children}
      </div>
    );
  }
}

MenuWrap.propTypes = {
  wait: PropTypes.number,
  side: PropTypes.string,
  children: PropTypes.element, // the Menu element
  autoHide: PropTypes.bool
}

export default function Burger(props) {
  const wrap = React.createRef();
  const dispatch = useDispatch();
  const auth = useAuth();

  const prefs = useSelector(selectPrefs);
  const overlay = props.overlay == null || props.overlay === true;
  const menuOpen = useSelector(selectBurgerMenuOpen);

  const closeMenu = () => dispatch(setBurgerMenuOpen(false));
  const onSelectItem = () => {prefs.burgerAutoHide === true && closeMenu()};
  const handleStateChange = state => dispatch(setBurgerMenuOpen(state.isOpen));

  function getItems() {
    let items = [];

    items = items.concat([
      <NavLink key="status" className="menu-item-1" to="/status" onClick={onSelectItem}>Status</NavLink>,
      <NavLink key="dash" className="menu-item-1" to="/" onClick={onSelectItem}>Dashboard</NavLink>,
      <NavLink key="config" className="menu-item-2" to="/config" onClick={onSelectItem}>Config</NavLink>,
      <NavLink key="devices" className="menu-item-2" to="/devices" onClick={onSelectItem}>Devices</NavLink>,
      <NavLink key="capabilities" className="menu-item-2" to="/capabilities" onClick={onSelectItem}>Capabilities</NavLink>,
      <NavLink key="pipeConfs" className="menu-item-2" to="/pipeConfs" onClick={onSelectItem}>PipeConfs</NavLink>,
    ]);

    items = items.concat([
      <NavLink key="preferences" className="menu-item-1" to="/preferences" onClick={onSelectItem}>Preferences</NavLink>,
      <button key="logout" className="menu-item-1" onClick={auth.removeUser}>Log Out</button>
    ]);

    return items;
  }

  const Menu = BurgerMenu[prefs.burgerAnimation];
  let greeting = <label/>;

  return (
    <div>
      <div id="outer-container" style={{height: "100%"}}>
        <MenuWrap autoHide={prefs.burgerAutoHide} ref={wrap} wait={20} side={prefs.burgerSide}>
          <div>
            <Menu noOverlay={!overlay} id={prefs.burgerAnimation}
              pageWrapId={"mainContent"} outerContainerId={"outer-container"} right={prefs.burgerSide === "right"}
              isOpen={menuOpen} onStateChange={(state) => handleStateChange(state)}>
              {getItems()}
            </Menu>
            <h2 className="burgerGreeting">{greeting}</h2>
          </div>
        </MenuWrap>
      </div>
    </div>
  );
}

Burger.propTypes = {
  burgerAnimation: PropTypes.string,  // the animation style, default: "push"
  burgerSide: PropTypes.string,       // default: "left"
  burgerOverlay: PropTypes.bool       // default: true
}

