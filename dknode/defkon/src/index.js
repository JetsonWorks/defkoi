import React from "react";
import ReactDOM from 'react-dom/client';
import { CookiesProvider } from "react-cookie";
import { BrowserRouter } from "react-router-dom";
import { store } from './store';
import { Provider } from 'react-redux';
import reportWebVitals from './reportWebVitals';
import { AuthProvider } from "react-oidc-context"

import App from "./App";
import "./defkoi.css";
import "./normalize.css";
import "react-toastify/dist/ReactToastify.css";

const onSigninCallback = (_user) => {
    window.history.replaceState(
        {},
        document.title,
        window.location.pathname
    )
}

const oidcConfig = {
    authority: process.env.REACT_APP_oidcAuthority,
    client_id: process.env.REACT_APP_oidcClientId,
    redirect_uri: process.env.REACT_APP_baseUrl,
    onSigninCallback: onSigninCallback,
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <AuthProvider
      {...oidcConfig}>
      <CookiesProvider>
        <BrowserRouter>
          <Provider store={store}>
            <App />
          </Provider>
        </BrowserRouter>
      </CookiesProvider>
    </AuthProvider>
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
